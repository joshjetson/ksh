package ksh

/**
 * A classroom — a live/guided cohort learners belong to, alongside their self-paced
 * SCORM courses (e.g. "Wednesday Conversation Group"). Admin-defined; read by everyone
 * (just a name). Scopes rosters, attendance, and grades.
 */
class Classroom {

    String  name
    String  description
    Integer sortOrder = 0

    Date dateCreated
    Date lastUpdated

    static constraints = {
        name        nullable: false, blank: false, unique: true, maxSize: 120
        description nullable: true,  maxSize: 1000
        sortOrder   nullable: true
    }

    static mapping = {
        table 'classroom'
        sortOrder column: 'sort_order'
    }

    String toString() { name }
}
