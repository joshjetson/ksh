databaseChangeLog = {

    changeSet(author: "ksh", id: "create-user-setting-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "user_setting") }
        }
        createTable(tableName: "user_setting") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "user_id", type: "BIGINT") { constraints(nullable: false, unique: true) }
            column(name: "discoverable", type: "BOOLEAN", defaultValueBoolean: false) { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(
            constraintName: "fk_user_setting_user",
            baseTableName: "user_setting", baseColumnNames: "user_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
    }

    changeSet(author: "ksh", id: "create-user-block-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "user_block") }
        }
        createTable(tableName: "user_block") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "owner_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "blocked_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(
            constraintName: "fk_user_block_owner",
            baseTableName: "user_block", baseColumnNames: "owner_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        addForeignKeyConstraint(
            constraintName: "fk_user_block_blocked",
            baseTableName: "user_block", baseColumnNames: "blocked_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        addUniqueConstraint(tableName: "user_block", columnNames: "owner_id, blocked_id",
                            constraintName: "uq_user_block_owner_blocked")
    }
}
