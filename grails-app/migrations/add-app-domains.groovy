databaseChangeLog = {

    changeSet(author: "ksh", id: "add-user-profile-fields") {
        addColumn(tableName: "app_user") {
            column(name: "name", type: "VARCHAR(255)")
            column(name: "email", type: "VARCHAR(255)")
            column(name: "phone_number", type: "VARCHAR(255)")
            column(name: "avatar", type: "VARCHAR(255)")
            column(name: "role_type", type: "VARCHAR(255)", defaultValue: "learner") {
                constraints(nullable: false)
            }
            column(name: "k_credits", type: "INTEGER", defaultValueNumeric: 0) {
                constraints(nullable: false)
            }
            column(name: "points", type: "INTEGER", defaultValueNumeric: 0) {
                constraints(nullable: false)
            }
        }
    }

    changeSet(author: "ksh", id: "create-course-table") {
        createTable(tableName: "course") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "coursePK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "long_title", type: "VARCHAR(255)") {
                constraints(nullable: false)
            }
            column(name: "short_title", type: "VARCHAR(255)") {
                constraints(nullable: false)
            }
            column(name: "thumbnail_small", type: "VARCHAR(255)")
            column(name: "thumbnail_large", type: "VARCHAR(255)")
            column(name: "long_description", type: "TEXT")
            column(name: "short_description", type: "TEXT")
            column(name: "tags", type: "TEXT")
            column(name: "cost_k_credits", type: "INTEGER", defaultValueNumeric: 0) {
                constraints(nullable: false)
            }
            column(name: "badge_reward", type: "VARCHAR(255)")
            column(name: "point_reward", type: "INTEGER", defaultValueNumeric: 0) {
                constraints(nullable: false)
            }
            column(name: "creator_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "date_created", type: "TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP")
        }

        addForeignKeyConstraint(
            baseTableName: "course",
            baseColumnNames: "creator_id",
            constraintName: "fk_course_creator",
            referencedTableName: "app_user",
            referencedColumnNames: "id"
        )
    }

    changeSet(author: "ksh", id: "create-course-enrollment-table") {
        createTable(tableName: "course_enrollment") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "courseEnrollmentPK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "course_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "enrolled_at", type: "TIMESTAMP") {
                constraints(nullable: false)
            }
            column(name: "completed_at", type: "TIMESTAMP")
            column(name: "progress", type: "INTEGER", defaultValueNumeric: 0) {
                constraints(nullable: false)
            }
            column(name: "date_created", type: "TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP")
        }

        addUniqueConstraint(
            tableName: "course_enrollment",
            columnNames: "user_id, course_id",
            constraintName: "uk_course_enrollment_user_course"
        )

        addForeignKeyConstraint(
            baseTableName: "course_enrollment",
            baseColumnNames: "user_id",
            constraintName: "fk_enrollment_user",
            referencedTableName: "app_user",
            referencedColumnNames: "id"
        )

        addForeignKeyConstraint(
            baseTableName: "course_enrollment",
            baseColumnNames: "course_id",
            constraintName: "fk_enrollment_course",
            referencedTableName: "course",
            referencedColumnNames: "id"
        )
    }

    changeSet(author: "ksh", id: "create-badge-table") {
        createTable(tableName: "badge") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "badgePK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: false)
            }
            column(name: "description", type: "TEXT")
            column(name: "icon", type: "VARCHAR(255)")
            column(name: "requirements", type: "TEXT")
            column(name: "date_created", type: "TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP")
        }
    }

    changeSet(author: "ksh", id: "create-user-badge-table") {
        createTable(tableName: "user_badge") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "userBadgePK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "badge_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "earned_at", type: "TIMESTAMP") {
                constraints(nullable: false)
            }
            column(name: "date_created", type: "TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP")
        }

        addUniqueConstraint(
            tableName: "user_badge",
            columnNames: "user_id, badge_id",
            constraintName: "uk_user_badge_user_badge"
        )

        addForeignKeyConstraint(
            baseTableName: "user_badge",
            baseColumnNames: "user_id",
            constraintName: "fk_user_badge_user",
            referencedTableName: "app_user",
            referencedColumnNames: "id"
        )

        addForeignKeyConstraint(
            baseTableName: "user_badge",
            baseColumnNames: "badge_id",
            constraintName: "fk_user_badge_badge",
            referencedTableName: "badge",
            referencedColumnNames: "id"
        )
    }

    changeSet(author: "ksh", id: "create-review-table") {
        createTable(tableName: "review") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "reviewPK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "course_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "star_rating", type: "INTEGER") {
                constraints(nullable: false)
            }
            column(name: "title", type: "VARCHAR(255)") {
                constraints(nullable: false)
            }
            column(name: "text", type: "TEXT")
            column(name: "date_created", type: "TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP")
        }

        addUniqueConstraint(
            tableName: "review",
            columnNames: "user_id, course_id",
            constraintName: "uk_review_user_course"
        )

        addForeignKeyConstraint(
            baseTableName: "review",
            baseColumnNames: "user_id",
            constraintName: "fk_review_user",
            referencedTableName: "app_user",
            referencedColumnNames: "id"
        )

        addForeignKeyConstraint(
            baseTableName: "review",
            baseColumnNames: "course_id",
            constraintName: "fk_review_course",
            referencedTableName: "course",
            referencedColumnNames: "id"
        )
    }

    changeSet(author: "ksh", id: "create-wall-post-table") {
        createTable(tableName: "wall_post") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "wallPostPK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "target_user_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "message", type: "TEXT") {
                constraints(nullable: false)
            }
            column(name: "date_created", type: "TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP")
        }

        addForeignKeyConstraint(
            baseTableName: "wall_post",
            baseColumnNames: "user_id",
            constraintName: "fk_wall_post_user",
            referencedTableName: "app_user",
            referencedColumnNames: "id"
        )

        addForeignKeyConstraint(
            baseTableName: "wall_post",
            baseColumnNames: "target_user_id",
            constraintName: "fk_wall_post_target_user",
            referencedTableName: "app_user",
            referencedColumnNames: "id"
        )
    }
}
