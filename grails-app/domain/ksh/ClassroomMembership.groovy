package ksh

/**
 * A learner enrolled in a Classroom for a Term — "who's in this cohort this term."
 * Named ClassroomMembership (NOT Enrollment) to stay clearly apart from
 * CourseEnrollment, the self-paced SCORM purchase. Teachers manage the roster;
 * a learner reads their own memberships (READ_SCOPE_FIELDS → user). One row per
 * (user, classroom, term).
 */
class ClassroomMembership {

    User      user
    Classroom classroom
    Term      term

    Date dateCreated
    Date lastUpdated

    static constraints = {
        user      nullable: false
        classroom nullable: false
        term      nullable: false, unique: ['user', 'classroom']   // unique (term, user, classroom)
    }

    static mapping = {
        table 'classroom_membership'
    }

    String toString() { "ClassroomMembership ${user?.id}@${classroom?.id}/${term?.id}" }
}
