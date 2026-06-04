package ksh

class AppConfig {

    boolean newsfeedEnabled = true
    String coursesLabel = 'Courses'
    boolean profileUploadEnabled = true

    // Branding
    String logoText = '한'
    String siteTitle = 'Korean School House'
    String siteSubtitle = '한국어 학교'
    byte[] backgroundImage
    String backgroundImageContentType
    String backgroundImageFileName

    Date dateCreated
    Date lastUpdated

    static constraints = {
        coursesLabel nullable: false, blank: false, maxSize: 50
        logoText nullable: false, blank: false, maxSize: 20
        siteTitle nullable: false, blank: false, maxSize: 100
        siteSubtitle nullable: true, maxSize: 200
        backgroundImage nullable: true, maxSize: 10485760  // 10 MB
        backgroundImageContentType nullable: true, maxSize: 100
        backgroundImageFileName nullable: true, maxSize: 255
    }

    static mapping = {
        table 'app_config'
        coursesLabel column: 'courses_label'
        newsfeedEnabled column: 'newsfeed_enabled'
        profileUploadEnabled column: 'profile_upload_enabled'
        logoText column: 'logo_text'
        siteTitle column: 'site_title'
        siteSubtitle column: 'site_subtitle'
        backgroundImage column: 'background_image', sqlType: 'bytea', lazy: true
        backgroundImageContentType column: 'background_image_content_type'
        backgroundImageFileName column: 'background_image_file_name'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
