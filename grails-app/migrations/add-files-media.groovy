databaseChangeLog = {

    changeSet(author: "ksh", id: "create-file-resource-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "file_resource") }
        }
        createTable(tableName: "file_resource") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "title", type: "VARCHAR(200)") { constraints(nullable: false) }
            column(name: "category", type: "VARCHAR(80)")
            column(name: "file", type: "BYTEA")
            column(name: "file_content_type", type: "VARCHAR(255)")
            column(name: "file_file_name", type: "VARCHAR(255)")
            column(name: "external_url", type: "VARCHAR(1000)")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
    }

    changeSet(author: "ksh", id: "create-media-post-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "media_post") }
        }
        createTable(tableName: "media_post") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "caption", type: "VARCHAR(1000)")
            column(name: "image", type: "BYTEA") { constraints(nullable: false) }
            column(name: "image_content_type", type: "VARCHAR(255)")
            column(name: "image_file_name", type: "VARCHAR(255)")
            column(name: "author_id", type: "BIGINT")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(
            constraintName: "fk_media_post_author",
            baseTableName: "media_post", baseColumnNames: "author_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
    }

    // FTS for the new searchable tables (same pattern as add-fts.groovy).
    changeSet(author: "ksh", id: "add-fts-files-media", dbms: "postgresql") {
        comment("search_fts generated tsvector + GIN for file_resource and media_post")
        grailsChange {
            change {
                Map<String, Map<String, List<String>>> fts = [
                    file_resource: [A: ['title'], B: ['category']],
                    media_post   : [A: ['caption']],
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
                ['file_resource', 'media_post'].each { String table ->
                    sql.execute("DROP INDEX IF EXISTS idx_${table}_search_fts".toString())
                    sql.execute("ALTER TABLE ${table} DROP COLUMN IF EXISTS search_fts".toString())
                }
            }
        }
    }
}
