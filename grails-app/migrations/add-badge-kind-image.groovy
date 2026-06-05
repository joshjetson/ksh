databaseChangeLog = {

    changeSet(author: "ksh", id: "add-kind-and-image-to-badge") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "badge", columnName: "kind") }
        }
        addColumn(tableName: "badge") {
            column(name: "kind", type: "VARCHAR(20)", defaultValue: "BADGE") {
                constraints(nullable: false)
            }
            column(name: "image", type: "BYTEA")
            column(name: "image_content_type", type: "VARCHAR(100)")
            column(name: "image_file_name", type: "VARCHAR(255)")
        }
    }
}
