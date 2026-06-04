databaseChangeLog = {

    changeSet(author: "ksh", id: "add-scorm-slide-count-to-course") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "course", columnName: "scorm_slide_count") }
        }
        addColumn(tableName: "course") {
            column(name: "scorm_slide_count", type: "INTEGER")
        }
    }
}
