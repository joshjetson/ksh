package ksh

class CourseEnrollment {

    User user
    Course course
    Date enrolledAt = new Date()
    Date completedAt
    Integer progress = 0

    Date dateCreated
    Date lastUpdated

    static constraints = {
        completedAt nullable: true
        progress min: 0, max: 100
        user unique: 'course'
    }

    static mapping = {
        table 'course_enrollment'
        user column: 'user_id'
        course column: 'course_id'
        enrolledAt column: 'enrolled_at'
        completedAt column: 'completed_at'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
