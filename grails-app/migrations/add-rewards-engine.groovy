databaseChangeLog = {

    changeSet(author: "ksh", id: "add-unlock-rule-to-badge") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "badge", columnName: "unlock_type") }
        }
        addColumn(tableName: "badge") {
            column(name: "unlock_type", type: "VARCHAR(20)", defaultValue: "NONE") {
                constraints(nullable: false)
            }
            column(name: "unlock_threshold", type: "INTEGER")
        }
    }

    changeSet(author: "ksh", id: "create-course-reward-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "course_reward") }
        }
        createTable(tableName: "course_reward") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "course_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "badge_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(
            constraintName: "fk_course_reward_course",
            baseTableName: "course_reward", baseColumnNames: "course_id",
            referencedTableName: "course", referencedColumnNames: "id")
        addForeignKeyConstraint(
            constraintName: "fk_course_reward_badge",
            baseTableName: "course_reward", baseColumnNames: "badge_id",
            referencedTableName: "badge", referencedColumnNames: "id")
        addUniqueConstraint(
            constraintName: "uq_course_reward",
            tableName: "course_reward", columnNames: "course_id, badge_id")
    }

    changeSet(author: "ksh", id: "add-date-created-to-app-user") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "app_user", columnName: "date_created") }
        }
        addColumn(tableName: "app_user") {
            column(name: "date_created", type: "TIMESTAMP")
        }
        // Backfill existing accounts so anniversary math has a baseline.
        update(tableName: "app_user") {
            column(name: "date_created", valueComputed: "CURRENT_TIMESTAMP")
            where "date_created IS NULL"
        }
    }
}
