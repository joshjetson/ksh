databaseChangeLog = {

    changeSet(author: "ksh", id: "add-branding-columns-to-app-config") {
        preConditions(onFail: "MARK_RAN") {
            not { columnExists(tableName: "app_config", columnName: "site_title") }
        }
        addColumn(tableName: "app_config") {
            column(name: "logo_text", type: "VARCHAR(20)", defaultValue: "한") {
                constraints(nullable: false)
            }
            column(name: "site_title", type: "VARCHAR(100)", defaultValue: "Korean School House") {
                constraints(nullable: false)
            }
            column(name: "site_subtitle", type: "VARCHAR(200)", defaultValue: "한국어 학교")
            column(name: "background_image", type: "BYTEA")
            column(name: "background_image_content_type", type: "VARCHAR(100)")
            column(name: "background_image_file_name", type: "VARCHAR(255)")
        }
    }
}
