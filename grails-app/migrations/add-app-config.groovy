databaseChangeLog = {

    changeSet(author: "ksh", id: "create-app-config-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "app_config") }
        }
        createTable(tableName: "app_config") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "appConfigPK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "newsfeed_enabled", type: "BOOLEAN", defaultValueBoolean: true) {
                constraints(nullable: false)
            }
            column(name: "courses_label", type: "VARCHAR(50)", defaultValue: "Courses") {
                constraints(nullable: false)
            }
            column(name: "profile_upload_enabled", type: "BOOLEAN", defaultValueBoolean: true) {
                constraints(nullable: false)
            }
            column(name: "date_created", type: "TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP")
        }
    }
}
