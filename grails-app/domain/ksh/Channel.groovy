package ksh

/**
 * A messaging channel (Discord-style). Admin-managed. `visibility` gates access:
 * 'school' (everyone) or 'staff' (teachers/admin only); 'dm' is a private 2-person
 * thread. Channels are read through the access-checked MessageService — NOT the
 * generic read instructions — so a student can never enumerate staff channels.
 */
class Channel {

    String  name
    String  description
    String  visibility = 'school'   // 'school' | 'staff' | 'dm' (private 2-person)
    Integer sortOrder = 0

    Date dateCreated
    Date lastUpdated

    static constraints = {
        name        nullable: false, blank: false, maxSize: 80
        description nullable: true,  maxSize: 300
        visibility  inList: ['school', 'staff', 'dm']
        sortOrder   nullable: true
    }

    static mapping = {
        table 'channel'
        sortOrder column: 'sort_order'
    }

    String toString() { name }
}
