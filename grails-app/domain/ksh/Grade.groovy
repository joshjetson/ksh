package ksh

/**
 * A graded item for a learner — a subject score in a grading period of a term.
 * Teachers record and manage them; a learner reads only their own
 * (READ_SCOPE_FIELDS → user). `gradedBy` is force-set to the teacher who entered it
 * (AUTHOR_FIELDS). Multiple grades per subject/period are allowed (assignments,
 * tests); "basic metrics" average them in the view.
 */
class Grade {

    User    user
    Term    term
    String  subject       // 'Speaking', 'Listening', 'Grammar', ...
    String  period        // 'Q1' | 'Q2' | 'Q3' | 'Q4' | 'Midterm' | 'Final'
    Integer score         // 0–100
    String  notes
    User    gradedBy

    Date dateCreated
    Date lastUpdated

    static constraints = {
        user     nullable: false
        term     nullable: false
        subject  nullable: false, blank: false, maxSize: 80
        period   nullable: false, blank: false, maxSize: 40
        score    nullable: false, min: 0, max: 100
        notes    nullable: true,  maxSize: 1000
        gradedBy nullable: true
    }

    static mapping = {
        table 'grade'
        notes type: 'text'
    }

    String toString() { "${subject} ${score}%" }
}
