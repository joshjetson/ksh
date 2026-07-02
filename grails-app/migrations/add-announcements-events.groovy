databaseChangeLog = {

    changeSet(author: "ksh", id: "create-announcement-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "announcement") }
        }
        createTable(tableName: "announcement") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "title", type: "VARCHAR(200)") { constraints(nullable: false) }
            column(name: "body", type: "TEXT") { constraints(nullable: false) }
            column(name: "pinned", type: "BOOLEAN", defaultValueBoolean: false) { constraints(nullable: false) }
            column(name: "author_id", type: "BIGINT")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(
            constraintName: "fk_announcement_author",
            baseTableName: "announcement", baseColumnNames: "author_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
    }

    changeSet(author: "ksh", id: "create-event-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "event") }
        }
        createTable(tableName: "event") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "title", type: "VARCHAR(200)") { constraints(nullable: false) }
            column(name: "description", type: "TEXT")
            column(name: "location", type: "VARCHAR(200)")
            column(name: "starts_at", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "ends_at", type: "TIMESTAMP")
            column(name: "all_day", type: "BOOLEAN", defaultValueBoolean: false) { constraints(nullable: false) }
            column(name: "color", type: "VARCHAR(20)", defaultValue: "rose") { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        createIndex(tableName: "event", indexName: "idx_event_starts_at") {
            column(name: "starts_at")
        }
    }

    // FTS for the new searchable tables (same pattern as add-fts.groovy).
    changeSet(author: "ksh", id: "add-fts-announcement-event", dbms: "postgresql") {
        comment("search_fts generated tsvector + GIN for announcement and event")
        grailsChange {
            change {
                Map<String, Map<String, List<String>>> fts = [
                    announcement: [A: ['title'], B: ['body']],
                    event       : [A: ['title'], B: ['description'], C: ['location']],
                ]
                fts.each { String table, Map<String, List<String>> buckets ->
                    String expr = buckets.collect { String weight, List<String> cols ->
                        cols.collect { String c ->
                            "setweight(to_tsvector('simple', COALESCE(${c}, '')), '${weight}')"
                        }.join(' || ')
                    }.join(' || ')
                    sql.execute("ALTER TABLE ${table} ADD COLUMN IF NOT EXISTS search_fts tsvector GENERATED ALWAYS AS (${expr}) STORED".toString())
                    sql.execute("CREATE INDEX IF NOT EXISTS idx_${table}_search_fts ON ${table} USING gin (search_fts)".toString())
                }
            }
            rollback {
                ['announcement', 'event'].each { String table ->
                    sql.execute("DROP INDEX IF EXISTS idx_${table}_search_fts".toString())
                    sql.execute("ALTER TABLE ${table} DROP COLUMN IF EXISTS search_fts".toString())
                }
            }
        }
    }
}
