databaseChangeLog = {

    changeSet(author: "ksh", id: "add-scorm-columns-to-course") {
        addColumn(tableName: "course") {
            column(name: "scorm", type: "BYTEA")
            column(name: "scorm_content_type", type: "VARCHAR(100)")
            column(name: "scorm_file_name", type: "VARCHAR(255)")
            column(name: "scorm_launch_url", type: "VARCHAR(500)")
        }
    }

    changeSet(author: "ksh", id: "create-scorm-cmi-data-table") {
        createTable(tableName: "scorm_cmi_data") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: false, primaryKey: true, primaryKeyName: "scormCmiDataPK")
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
            column(name: "cmi_key", type: "VARCHAR(255)") {
                constraints(nullable: false)
            }
            column(name: "cmi_value", type: "TEXT")
            column(name: "date_created", type: "TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP")
        }

        addUniqueConstraint(
            tableName: "scorm_cmi_data",
            columnNames: "user_id, course_id, cmi_key",
            constraintName: "uk_scorm_cmi_user_course_key"
        )

        addForeignKeyConstraint(
            baseTableName: "scorm_cmi_data",
            baseColumnNames: "user_id",
            constraintName: "fk_scorm_cmi_user",
            referencedTableName: "app_user",
            referencedColumnNames: "id"
        )

        addForeignKeyConstraint(
            baseTableName: "scorm_cmi_data",
            baseColumnNames: "course_id",
            constraintName: "fk_scorm_cmi_course",
            referencedTableName: "course",
            referencedColumnNames: "id"
        )
    }
}
