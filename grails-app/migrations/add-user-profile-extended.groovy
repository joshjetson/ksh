databaseChangeLog = {

    changeSet(author: "ksh", id: "add-user-extended-fields") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "app_user", columnName: "first_name") }
        }
        addColumn(tableName: "app_user") {
            column(name: "first_name", type: "VARCHAR(255)")
            column(name: "last_name", type: "VARCHAR(255)")
            column(name: "title", type: "VARCHAR(255)")
            column(name: "country", type: "VARCHAR(255)")
            column(name: "date_of_birth", type: "DATE")
        }
    }
}
