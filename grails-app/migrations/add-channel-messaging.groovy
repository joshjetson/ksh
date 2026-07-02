databaseChangeLog = {

    // ── Channel messaging + inbox consolidation ────────────────────────────────
    // The old 1:1 student↔staff support inbox (conversation + conversation-shaped
    // message rows) becomes the new channel model: each conversation converts to a
    // 'dm' Channel between the student and their staff counterpart (the sender of
    // the thread's most recent staff message; fallback: the first admin). The
    // message table is transformed IN PLACE — snapshot the DB before running.

    changeSet(author: "ksh", id: "create-channel-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "channel") }
        }
        createTable(tableName: "channel") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "name", type: "VARCHAR(80)") { constraints(nullable: false) }
            column(name: "description", type: "VARCHAR(300)")
            column(name: "visibility", type: "VARCHAR(20)", defaultValue: "school") { constraints(nullable: false) }
            column(name: "sort_order", type: "INTEGER")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
    }

    changeSet(author: "ksh", id: "create-channel-membership-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "channel_membership") }
        }
        createTable(tableName: "channel_membership") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "channel_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "user_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "status", type: "VARCHAR(20)", defaultValue: "active") { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(
            constraintName: "fk_channel_membership_channel",
            baseTableName: "channel_membership", baseColumnNames: "channel_id",
            referencedTableName: "channel", referencedColumnNames: "id")
        addForeignKeyConstraint(
            constraintName: "fk_channel_membership_user",
            baseTableName: "channel_membership", baseColumnNames: "user_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        addUniqueConstraint(tableName: "channel_membership", columnNames: "channel_id, user_id",
                            constraintName: "uq_channel_membership")
    }

    changeSet(author: "ksh", id: "message-add-channel-columns") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "message", columnName: "channel_id") }
        }
        addColumn(tableName: "message") {
            column(name: "channel_id", type: "BIGINT")
            column(name: "author_id", type: "BIGINT")
            column(name: "last_updated", type: "TIMESTAMP")
        }
    }

    changeSet(author: "ksh", id: "convert-conversations-to-dm-channels") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "conversation")
        }
        comment("Each support conversation becomes a dm Channel (student + staff counterpart); messages re-point to it")
        grailsChange {
            change {
                def fallbackStaff = sql.firstRow("""
                    SELECT u.id AS id FROM app_user u
                    JOIN user_role ur ON ur.user_id = u.id
                    JOIN role r ON r.id = ur.role_id
                    WHERE r.authority = 'ROLE_ADMIN' ORDER BY u.id LIMIT 1""")?.id

                sql.rows("SELECT id, student_id, date_created, last_updated FROM conversation").each { c ->
                    def keys = sql.executeInsert("""
                        INSERT INTO channel (version, name, visibility, sort_order, date_created, last_updated)
                        VALUES (0, 'Direct message', 'dm', 0, ?, ?)""",
                        [c.date_created, c.last_updated])
                    Long chId = keys[0][0] as Long

                    // Staff counterpart = latest sender in the thread who isn't the student.
                    def staffId = sql.firstRow("""
                        SELECT sender_id AS sid FROM message
                        WHERE conversation_id = ? AND sender_id <> ?
                        ORDER BY date_created DESC LIMIT 1""", [c.id, c.student_id])?.sid ?: fallbackStaff

                    sql.executeUpdate("""
                        INSERT INTO channel_membership (version, channel_id, user_id, status, date_created, last_updated)
                        VALUES (0, ?, ?, 'active', ?, ?)""", [chId, c.student_id, c.date_created, c.last_updated])
                    if (staffId && (staffId as Long) != (c.student_id as Long)) {
                        sql.executeUpdate("""
                            INSERT INTO channel_membership (version, channel_id, user_id, status, date_created, last_updated)
                            VALUES (0, ?, ?, 'active', ?, ?)""", [chId, staffId, c.date_created, c.last_updated])
                    }

                    sql.executeUpdate("""
                        UPDATE message SET channel_id = ?, author_id = sender_id, last_updated = date_created
                        WHERE conversation_id = ?""", [chId, c.id])
                }
            }
        }
    }

    changeSet(author: "ksh", id: "message-finalize-channel-shape") {
        preConditions(onFail: "MARK_RAN") {
            columnExists(tableName: "message", columnName: "conversation_id")
        }
        // Any message that somehow has no channel after conversion would violate the
        // NOT NULL below — that's deliberate: fail loud rather than orphan chat data.
        grailsChange {
            change {
                sql.executeUpdate("UPDATE message SET last_updated = date_created WHERE last_updated IS NULL")
            }
        }
        addNotNullConstraint(tableName: "message", columnName: "channel_id", columnDataType: "BIGINT")
        addNotNullConstraint(tableName: "message", columnName: "last_updated", columnDataType: "TIMESTAMP")
        addForeignKeyConstraint(
            constraintName: "fk_message_channel",
            baseTableName: "message", baseColumnNames: "channel_id",
            referencedTableName: "channel", referencedColumnNames: "id")
        addForeignKeyConstraint(
            constraintName: "fk_message_author",
            baseTableName: "message", baseColumnNames: "author_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        createIndex(tableName: "message", indexName: "idx_message_channel") {
            column(name: "channel_id")
        }
        dropColumn(tableName: "message", columnName: "read_at")
        dropColumn(tableName: "message", columnName: "sender_id")
        dropColumn(tableName: "message", columnName: "conversation_id")
    }

    changeSet(author: "ksh", id: "drop-conversation-table") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "conversation")
        }
        dropTable(tableName: "conversation")
    }

    // FTS for chat search (same pattern as add-fts.groovy).
    changeSet(author: "ksh", id: "add-fts-message", dbms: "postgresql") {
        comment("search_fts generated tsvector + GIN for message")
        grailsChange {
            change {
                sql.execute("ALTER TABLE message ADD COLUMN IF NOT EXISTS search_fts tsvector GENERATED ALWAYS AS (setweight(to_tsvector('simple', COALESCE(body, '')), 'A')) STORED")
                sql.execute("CREATE INDEX IF NOT EXISTS idx_message_search_fts ON message USING gin (search_fts)")
            }
            rollback {
                sql.execute("DROP INDEX IF EXISTS idx_message_search_fts")
                sql.execute("ALTER TABLE message DROP COLUMN IF EXISTS search_fts")
            }
        }
    }
}
