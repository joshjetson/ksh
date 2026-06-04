databaseChangeLog = {

    changeSet(author: "ksh", id: "create-blackout-date-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "blackout_date") }
        }
        createTable(tableName: "blackout_date") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "blackout_date", type: "TIMESTAMP") { constraints(nullable: false, unique: true) }
            column(name: "reason", type: "VARCHAR(200)")
            column(name: "all_day", type: "BOOLEAN", defaultValueBoolean: true) { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
    }

    changeSet(author: "ksh", id: "create-enrollment-change-request-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "enrollment_change_request") }
        }
        createTable(tableName: "enrollment_change_request") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "enrollment_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "requested_by_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "kind", type: "VARCHAR(20)", defaultValue: "WITHDRAW") { constraints(nullable: false) }
            column(name: "requested_date", type: "TIMESTAMP")
            column(name: "message", type: "TEXT") { constraints(nullable: false) }
            column(name: "status", type: "VARCHAR(20)", defaultValue: "PENDING") { constraints(nullable: false) }
            column(name: "response_message", type: "TEXT")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(
            constraintName: "fk_change_request_enrollment",
            baseTableName: "enrollment_change_request", baseColumnNames: "enrollment_id",
            referencedTableName: "course_enrollment", referencedColumnNames: "id")
        addForeignKeyConstraint(
            constraintName: "fk_change_request_user",
            baseTableName: "enrollment_change_request", baseColumnNames: "requested_by_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
    }
}
