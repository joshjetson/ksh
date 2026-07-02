databaseChangeLog = {

    // ── Community wall ──────────────────────────────────────────────────────────
    // Replaces the flat WallPost newsfeed: posts gain visibility (public/staff/self),
    // anonymity, moderation (hidden), pins and cheers. Existing wall_post rows migrate
    // into community_post as public posts, then wall_post is dropped.

    changeSet(author: "ksh", id: "create-community-post-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "community_post") }
        }
        createTable(tableName: "community_post") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "kind", type: "VARCHAR(20)", defaultValue: "post") { constraints(nullable: false) }
            column(name: "title", type: "VARCHAR(200)") { constraints(nullable: false) }
            column(name: "body", type: "TEXT") { constraints(nullable: false) }
            column(name: "visibility", type: "VARCHAR(20)", defaultValue: "public") { constraints(nullable: false) }
            column(name: "anonymous", type: "BOOLEAN", defaultValueBoolean: false) { constraints(nullable: false) }
            column(name: "hidden", type: "BOOLEAN", defaultValueBoolean: false) { constraints(nullable: false) }
            column(name: "author_id", type: "BIGINT")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(
            constraintName: "fk_community_post_author",
            baseTableName: "community_post", baseColumnNames: "author_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
    }

    changeSet(author: "ksh", id: "create-post-pin-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "post_pin") }
        }
        createTable(tableName: "post_pin") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "user_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "post_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(constraintName: "fk_post_pin_user",
            baseTableName: "post_pin", baseColumnNames: "user_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        addForeignKeyConstraint(constraintName: "fk_post_pin_post",
            baseTableName: "post_pin", baseColumnNames: "post_id",
            referencedTableName: "community_post", referencedColumnNames: "id")
        addUniqueConstraint(tableName: "post_pin", columnNames: "user_id, post_id",
                            constraintName: "uq_post_pin")
    }

    changeSet(author: "ksh", id: "create-post-cheer-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "post_cheer") }
        }
        createTable(tableName: "post_cheer") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "user_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "post_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(constraintName: "fk_post_cheer_user",
            baseTableName: "post_cheer", baseColumnNames: "user_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        addForeignKeyConstraint(constraintName: "fk_post_cheer_post",
            baseTableName: "post_cheer", baseColumnNames: "post_id",
            referencedTableName: "community_post", referencedColumnNames: "id")
        addUniqueConstraint(tableName: "post_cheer", columnNames: "user_id, post_id",
                            constraintName: "uq_post_cheer")
    }

    changeSet(author: "ksh", id: "migrate-wall-posts-to-community") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "wall_post")
        }
        comment("Old newsfeed WallPost rows become public community posts, then wall_post drops")
        grailsChange {
            change {
                sql.rows("SELECT id, user_id, message, date_created, last_updated FROM wall_post").each { w ->
                    String msg = (w.message ?: '').toString()
                    String title = msg.length() > 60 ? msg.substring(0, 57) + '…' : (msg ?: 'Post')
                    sql.executeUpdate("""
                        INSERT INTO community_post (version, kind, title, body, visibility, anonymous, hidden, author_id, date_created, last_updated)
                        VALUES (0, 'post', ?, ?, 'public', false, false, ?, ?, ?)""",
                        [title, msg, w.user_id, w.date_created, w.last_updated ?: w.date_created])
                }
            }
        }
        dropTable(tableName: "wall_post")
    }

    // FTS for post search (same pattern as add-fts.groovy).
    changeSet(author: "ksh", id: "add-fts-community-post", dbms: "postgresql") {
        comment("search_fts generated tsvector + GIN for community_post")
        grailsChange {
            change {
                sql.execute("ALTER TABLE community_post ADD COLUMN IF NOT EXISTS search_fts tsvector GENERATED ALWAYS AS (setweight(to_tsvector('simple', COALESCE(title, '')), 'A') || setweight(to_tsvector('simple', COALESCE(body, '')), 'B')) STORED")
                sql.execute("CREATE INDEX IF NOT EXISTS idx_community_post_search_fts ON community_post USING gin (search_fts)")
            }
            rollback {
                sql.execute("DROP INDEX IF EXISTS idx_community_post_search_fts")
                sql.execute("ALTER TABLE community_post DROP COLUMN IF EXISTS search_fts")
            }
        }
    }
}
