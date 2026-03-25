package ksh

class AppConfig {

    boolean newsfeedEnabled = true
    String coursesLabel = 'Courses'
    boolean profileUploadEnabled = true

    Date dateCreated
    Date lastUpdated

    static constraints = {
        coursesLabel nullable: false, blank: false, maxSize: 50
    }

    static mapping = {
        table 'app_config'
        coursesLabel column: 'courses_label'
        newsfeedEnabled column: 'newsfeed_enabled'
        profileUploadEnabled column: 'profile_upload_enabled'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
