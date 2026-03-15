package ksh

class Course {

    String longTitle
    String shortTitle
    String thumbnailSmall
    String thumbnailLarge
    String longDescription
    String shortDescription
    String tags
    Integer costKCredits = 0
    String badgeReward
    Integer pointReward = 0
    byte[] scorm
    String scormContentType
    String scormFileName
    String scormLaunchUrl

    User creator

    Date dateCreated
    Date lastUpdated

    static constraints = {
        longTitle nullable: false, blank: false, maxSize: 255
        shortTitle nullable: false, blank: false, maxSize: 255
        thumbnailSmall nullable: true, maxSize: 500
        thumbnailLarge nullable: true, maxSize: 500
        longDescription nullable: true, maxSize: 5000
        shortDescription nullable: true, maxSize: 1000
        tags nullable: true, maxSize: 500
        badgeReward nullable: true, maxSize: 255
        scorm nullable: true, maxSize: 524288000
        scormContentType nullable: true, maxSize: 100
        scormFileName nullable: true, maxSize: 255
        scormLaunchUrl nullable: true, maxSize: 500
        creator nullable: false
    }

    static mapping = {
        longTitle column: 'long_title'
        shortTitle column: 'short_title'
        thumbnailSmall column: 'thumbnail_small'
        thumbnailLarge column: 'thumbnail_large'
        longDescription column: 'long_description', type: 'text'
        shortDescription column: 'short_description', type: 'text'
        tags type: 'text'
        costKCredits column: 'cost_k_credits'
        badgeReward column: 'badge_reward'
        pointReward column: 'point_reward'
        scorm column: 'scorm', sqlType: 'bytea'
        scormContentType column: 'scorm_content_type'
        scormFileName column: 'scorm_file_name'
        scormLaunchUrl column: 'scorm_launch_url'
        creator column: 'creator_id'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
