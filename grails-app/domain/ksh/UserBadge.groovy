package ksh

class UserBadge {

    User user
    Badge badge
    Date earnedAt = new Date()

    Date dateCreated
    Date lastUpdated

    static constraints = {
        user unique: 'badge'
    }

    static mapping = {
        table 'user_badge'
        user column: 'user_id'
        badge column: 'badge_id'
        earnedAt column: 'earned_at'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
