package ksh

/**
 * Links a Course to a Badge it grants on completion. A course can have many
 * (multiple badges/avatars per course). Managed as a first-class domain via the
 * universal CRUD endpoint (attach = save, detach = delete) — no many-to-many
 * binding gymnastics. The grant engine reads these in RewardService.
 */
class CourseReward {

    Course course
    Badge badge

    Date dateCreated

    static constraints = {
        course nullable: false
        badge nullable: false, unique: 'course'   // a course grants each badge at most once
    }

    static mapping = {
        table 'course_reward'
        course column: 'course_id'
        badge column: 'badge_id'
        dateCreated column: 'date_created'
    }
}
