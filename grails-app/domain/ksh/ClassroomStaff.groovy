package ksh

/**
 * Assigns a teacher to a Classroom — their "home" cohort for attendance, roster, and
 * grades. Admin assigns; a teacher reads only their own assignments
 * (READ_SCOPE_FIELDS → staff), admins read all. The assigned user must be a manager
 * (ROLE_TEACHER/ROLE_ADMIN) — a student can't be a teacher.
 */
class ClassroomStaff {

    User      staff
    Classroom classroom

    Date dateCreated
    Date lastUpdated

    static constraints = {
        classroom nullable: false, unique: 'staff'   // one assignment per (staff, classroom)
        staff     nullable: false, validator: { User u, ClassroomStaff cs ->
            def roles = u?.authorities*.authority ?: []
            (roles.contains('ROLE_TEACHER') || roles.contains('ROLE_ADMIN')) ? true : 'notStaff'
        }
    }

    static mapping = {
        table 'classroom_staff'
    }

    String toString() { "ClassroomStaff ${staff?.id}->${classroom?.id}" }
}
