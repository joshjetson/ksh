databaseChangeLog = {

    changeSet(author: "ksh", id: "create-classroom-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "classroom") }
        }
        createTable(tableName: "classroom") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "name", type: "VARCHAR(120)") { constraints(nullable: false, unique: true) }
            column(name: "description", type: "VARCHAR(1000)")
            column(name: "sort_order", type: "INTEGER")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
    }

    changeSet(author: "ksh", id: "create-classroom-staff-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "classroom_staff") }
        }
        createTable(tableName: "classroom_staff") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "staff_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "classroom_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(constraintName: "fk_classroom_staff_staff",
            baseTableName: "classroom_staff", baseColumnNames: "staff_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        addForeignKeyConstraint(constraintName: "fk_classroom_staff_classroom",
            baseTableName: "classroom_staff", baseColumnNames: "classroom_id",
            referencedTableName: "classroom", referencedColumnNames: "id")
        addUniqueConstraint(tableName: "classroom_staff", columnNames: "staff_id, classroom_id",
                            constraintName: "uq_classroom_staff")
    }

    changeSet(author: "ksh", id: "create-term-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "term") }
        }
        createTable(tableName: "term") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "name", type: "VARCHAR(80)") { constraints(nullable: false, unique: true) }
            column(name: "starts_on", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "ends_on", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "active", type: "BOOLEAN", defaultValueBoolean: false) { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
    }

    changeSet(author: "ksh", id: "create-classroom-membership-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "classroom_membership") }
        }
        createTable(tableName: "classroom_membership") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "user_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "classroom_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "term_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(constraintName: "fk_classroom_membership_user",
            baseTableName: "classroom_membership", baseColumnNames: "user_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        addForeignKeyConstraint(constraintName: "fk_classroom_membership_classroom",
            baseTableName: "classroom_membership", baseColumnNames: "classroom_id",
            referencedTableName: "classroom", referencedColumnNames: "id")
        addForeignKeyConstraint(constraintName: "fk_classroom_membership_term",
            baseTableName: "classroom_membership", baseColumnNames: "term_id",
            referencedTableName: "term", referencedColumnNames: "id")
        addUniqueConstraint(tableName: "classroom_membership", columnNames: "user_id, classroom_id, term_id",
                            constraintName: "uq_classroom_membership")
    }

    changeSet(author: "ksh", id: "create-attendance-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "attendance") }
        }
        createTable(tableName: "attendance") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "user_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "attendance_day", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "status", type: "VARCHAR(20)", defaultValue: "present") { constraints(nullable: false) }
            column(name: "note", type: "VARCHAR(500)")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(constraintName: "fk_attendance_user",
            baseTableName: "attendance", baseColumnNames: "user_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        createIndex(tableName: "attendance", indexName: "idx_attendance_day") {
            column(name: "attendance_day")
        }
    }

    changeSet(author: "ksh", id: "create-grade-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "grade") }
        }
        createTable(tableName: "grade") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") { constraints(nullable: false) }
            column(name: "user_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "term_id", type: "BIGINT") { constraints(nullable: false) }
            column(name: "subject", type: "VARCHAR(80)") { constraints(nullable: false) }
            column(name: "period", type: "VARCHAR(40)") { constraints(nullable: false) }
            column(name: "score", type: "INTEGER") { constraints(nullable: false) }
            column(name: "notes", type: "TEXT")
            column(name: "graded_by_id", type: "BIGINT")
            column(name: "date_created", type: "TIMESTAMP") { constraints(nullable: false) }
            column(name: "last_updated", type: "TIMESTAMP") { constraints(nullable: false) }
        }
        addForeignKeyConstraint(constraintName: "fk_grade_user",
            baseTableName: "grade", baseColumnNames: "user_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        addForeignKeyConstraint(constraintName: "fk_grade_term",
            baseTableName: "grade", baseColumnNames: "term_id",
            referencedTableName: "term", referencedColumnNames: "id")
        addForeignKeyConstraint(constraintName: "fk_grade_graded_by",
            baseTableName: "grade", baseColumnNames: "graded_by_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
    }

    // FTS for classroom search (same pattern as add-fts.groovy).
    changeSet(author: "ksh", id: "add-fts-classroom", dbms: "postgresql") {
        comment("search_fts generated tsvector + GIN for classroom")
        grailsChange {
            change {
                sql.execute("ALTER TABLE classroom ADD COLUMN IF NOT EXISTS search_fts tsvector GENERATED ALWAYS AS (setweight(to_tsvector('simple', COALESCE(name, '')), 'A') || setweight(to_tsvector('simple', COALESCE(description, '')), 'B')) STORED")
                sql.execute("CREATE INDEX IF NOT EXISTS idx_classroom_search_fts ON classroom USING gin (search_fts)")
            }
            rollback {
                sql.execute("DROP INDEX IF EXISTS idx_classroom_search_fts")
                sql.execute("ALTER TABLE classroom DROP COLUMN IF EXISTS search_fts")
            }
        }
    }
}
