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

    User creator

    Date dateCreated
    Date lastUpdated

    static constraints = {
        longTitle nullable: false, blank: false
        shortTitle nullable: false, blank: false
        thumbnailSmall nullable: true
        thumbnailLarge nullable: true
        longDescription nullable: true
        shortDescription nullable: true
        tags nullable: true
        badgeReward nullable: true
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
        creator column: 'creator_id'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
