databaseChangeLog = {

    changeSet(author: "ksh", id: "add-image-to-course") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "course", columnName: "image") }
        }
        addColumn(tableName: "course") {
            column(name: "image", type: "BYTEA")
            column(name: "image_content_type", type: "VARCHAR(100)")
            column(name: "image_file_name", type: "VARCHAR(255)")
        }
    }
}
