package ksh

/**
 * A learner's attendance for one day (live/guided sessions). Recorded by teachers;
 * a learner reads only their own rows (READ_SCOPE_FIELDS → user), writes are
 * teacher/admin. A classroom's day is recorded in one request via saveBatch
 * (natural key: user,day) so re-submitting updates rather than duplicates.
 */
class Attendance {

    User   user
    Date   day
    String status = 'present'   // present | absent | tardy | excused
    String note

    Date dateCreated
    Date lastUpdated

    static constraints = {
        user   nullable: false
        day    nullable: false
        status inList: ['present', 'absent', 'tardy', 'excused']
        note   nullable: true, maxSize: 500
    }

    static mapping = {
        table 'attendance'
        day column: 'attendance_day'   // `day` is a reserved word in H2 (test env)
    }

    String toString() { "${user} — ${day?.format('yyyy-MM-dd')}: ${status}" }
}
