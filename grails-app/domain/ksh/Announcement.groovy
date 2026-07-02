package ksh

/**
 * A school announcement (home feed / dashboard). Posted by teachers/admin; read by
 * everyone. `author` is force-set to the creator by the controller (AUTHOR_FIELDS) —
 * never client-supplied.
 */
class Announcement {

    String  title
    String  body
    boolean pinned = false
    User    author

    Date dateCreated
    Date lastUpdated

    static constraints = {
        title  nullable: false, blank: false, maxSize: 200
        body   nullable: false, blank: false, maxSize: 5000
        author nullable: true
    }

    static mapping = {
        table 'announcement'
        body  type: 'text'
    }

    String toString() { title }
}
