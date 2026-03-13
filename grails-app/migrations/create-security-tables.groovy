databaseChangeLog = {

    changeSet(author: "ksh", id: "create-hibernate-sequence") {
        createSequence(sequenceName: "hibernate_sequence", startValue: "1", incrementBy: "1")
    }

    changeSet(author: "ksh", id: "create-role-table") {
        createTable(tableName: "role") {
            column(autoIncrement: true, name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "rolePK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "authority", type: "VARCHAR(255)") {
                constraints(nullable: false, unique: true)
            }
        }
    }

    changeSet(author: "ksh", id: "create-user-table") {
        createTable(tableName: "app_user") {
            column(autoIncrement: true, name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "userPK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "username", type: "VARCHAR(255)") {
                constraints(nullable: false, unique: true)
            }
            column(name: "password", type: "VARCHAR(255)") {
                constraints(nullable: false)
            }
            column(name: "enabled", type: "BOOLEAN") {
                constraints(nullable: false)
            }
            column(name: "account_expired", type: "BOOLEAN") {
                constraints(nullable: false)
            }
            column(name: "account_locked", type: "BOOLEAN") {
                constraints(nullable: false)
            }
            column(name: "password_expired", type: "BOOLEAN") {
                constraints(nullable: false)
            }
        }
    }

    changeSet(author: "ksh", id: "create-user-role-table") {
        createTable(tableName: "user_role") {
            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "role_id", type: "BIGINT") {
                constraints(nullable: false)
            }
        }

        addPrimaryKey(
            tableName: "user_role",
            columnNames: "user_id, role_id",
            constraintName: "user_rolePK"
        )

        addForeignKeyConstraint(
            baseTableName: "user_role",
            baseColumnNames: "user_id",
            constraintName: "fk_user_role_user",
            referencedTableName: "app_user",
            referencedColumnNames: "id"
        )

        addForeignKeyConstraint(
            baseTableName: "user_role",
            baseColumnNames: "role_id",
            constraintName: "fk_user_role_role",
            referencedTableName: "role",
            referencedColumnNames: "id"
        )
    }
}
