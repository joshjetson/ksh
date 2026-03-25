databaseChangeLog = {
    include file: 'create-security-tables.groovy'
    include file: 'add-app-domains.groovy'
    include file: 'add-scorm-support.groovy'
    include file: 'add-user-profile-extended.groovy'
    include file: 'add-app-config.groovy'
}
