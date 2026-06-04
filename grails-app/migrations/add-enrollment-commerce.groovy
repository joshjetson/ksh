databaseChangeLog = {

    changeSet(author: "ksh", id: "create-discount-code-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "discount_code") }
        }
        createTable(tableName: "discount_code") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "code", type: "VARCHAR(40)") { constraints(nullable: false, unique: true) }
            column(name: "kind", type: "VARCHAR(20)", defaultValue: "PERCENT") { constraints(nullable: false) }
            column(name: "value", type: "NUMERIC(19,2)", defaultValueNumeric: 0) { constraints(nullable: false) }
            column(name: "active", type: "BOOLEAN", defaultValueBoolean: true) { constraints(nullable: false) }
            column(name: "starts_on", type: "TIMESTAMP")
            column(name: "expires_on", type: "TIMESTAMP")
            column(name: "usage_limit", type: "INTEGER")
            column(name: "times_used", type: "INTEGER", defaultValueNumeric: 0) { constraints(nullable: false) }
            column(name: "min_amount", type: "NUMERIC(19,2)")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
    }

    changeSet(author: "ksh", id: "add-commerce-to-course-enrollment") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "course_enrollment", columnName: "status") }
        }
        addColumn(tableName: "course_enrollment") {
            column(name: "status", type: "VARCHAR(20)", defaultValue: "ACTIVE") {
                constraints(nullable: false)
            }
            column(name: "payment_status", type: "VARCHAR(20)", defaultValue: "UNPAID") {
                constraints(nullable: false)
            }
            column(name: "base_amount", type: "NUMERIC(19,2)")
            column(name: "total_amount", type: "NUMERIC(19,2)")
            column(name: "discount_code_id", type: "BIGINT")
            column(name: "notes", type: "TEXT")
        }
        addForeignKeyConstraint(
            constraintName: "fk_enrollment_discount_code",
            baseTableName: "course_enrollment", baseColumnNames: "discount_code_id",
            referencedTableName: "discount_code", referencedColumnNames: "id")
    }
}
