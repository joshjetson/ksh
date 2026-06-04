databaseChangeLog = {

    changeSet(author: "ksh", id: "create-conversation-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "conversation") }
        }
        createTable(tableName: "conversation") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "student_id", type: "BIGINT") {
                constraints(nullable: false, unique: true)
            }
            column(name: "subject", type: "VARCHAR(200)")
            column(name: "last_message_at", type: "TIMESTAMP")
            column(name: "awaiting_reply", type: "BOOLEAN", defaultValueBoolean: false) {
                constraints(nullable: false)
            }
            column(name: "date_created", type: "TIMESTAMP") {
                constraints(nullable: false)
            }
            column(name: "last_updated", type: "TIMESTAMP") {
                constraints(nullable: false)
            }
        }
        addForeignKeyConstraint(
            constraintName: "fk_conversation_student",
            baseTableName: "conversation", baseColumnNames: "student_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
    }

    changeSet(author: "ksh", id: "create-message-table") {
        preConditions(onFail: "MARK_RAN") {
            not { tableExists(tableName: "message") }
        }
        createTable(tableName: "message") {
            column(name: "id", type: "BIGINT", autoIncrement: true) {
                constraints(primaryKey: true, nullable: false)
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "conversation_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "sender_id", type: "BIGINT") {
                constraints(nullable: false)
            }
            column(name: "body", type: "TEXT") {
                constraints(nullable: false)
            }
            column(name: "read_at", type: "TIMESTAMP")
            column(name: "date_created", type: "TIMESTAMP") {
                constraints(nullable: false)
            }
        }
        addForeignKeyConstraint(
            constraintName: "fk_message_conversation",
            baseTableName: "message", baseColumnNames: "conversation_id",
            referencedTableName: "conversation", referencedColumnNames: "id")
        addForeignKeyConstraint(
            constraintName: "fk_message_sender",
            baseTableName: "message", baseColumnNames: "sender_id",
            referencedTableName: "app_user", referencedColumnNames: "id")
        createIndex(indexName: "idx_message_conversation", tableName: "message") {
            column(name: "conversation_id")
        }
    }
}
