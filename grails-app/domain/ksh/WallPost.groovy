package ksh

class WallPost {

    User user
    User targetUser
    String message

    Date dateCreated
    Date lastUpdated

    static constraints = {
        message nullable: false, blank: false, maxSize: 5000
    }

    static mapping = {
        table 'wall_post'
        user column: 'user_id'
        targetUser column: 'target_user_id'
        message type: 'text'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
