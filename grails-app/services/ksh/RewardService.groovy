package ksh

import grails.gorm.transactions.Transactional

/**
 * The gamification grant engine. Two entry points:
 *  - grantForCompletion(user, course): fired once when a learner first completes a
 *    course — awards the course's point reward, grants its attached badges, then
 *    re-checks global milestones.
 *  - checkMilestones(user): re-evaluates global (auto-granted) badges for a user;
 *    also called on portal load so time-based milestones (anniversaries) catch up.
 *
 * All grants are idempotent (one UserBadge per user+badge), so calling repeatedly
 * is safe.
 */
@Transactional
class RewardService {

    /** A learner just completed `course`. Award points + attached badges + milestones. */
    void grantForCompletion(User user, Course course) {
        if (!user?.id || !course?.id) return
        User u = User.get(user.id)
        if (!u) return

        Integer pts = course.pointReward ?: 0
        if (pts > 0) {
            u.points = (u.points ?: 0) + pts
            u.save(failOnError: true)
        }

        CourseReward.findAllByCourse(course).each { cr -> grant(u, cr.badge) }

        checkMilestones(u)
    }

    /** Re-evaluate every global (auto-granted) badge for this user. Idempotent. */
    void checkMilestones(User user) {
        if (!user?.id) return
        User u = User.get(user.id)
        if (!u) return

        int points = u.points ?: 0
        int completed = CourseEnrollment.countByUserAndCompletedAtIsNotNull(u)
        int years = u.dateCreated ? yearsSince(u.dateCreated) : 0

        Badge.findAllByUnlockTypeNotEqual(UnlockType.NONE).each { Badge b ->
            int t = b.unlockThreshold ?: 0
            boolean met
            switch (b.unlockType) {
                case UnlockType.POINTS:       met = points >= t; break
                case UnlockType.COURSE_COUNT: met = completed >= t; break
                case UnlockType.ANNIVERSARY:  met = years >= t; break
                default:                      met = false
            }
            if (met) grant(u, b)
        }
    }

    /** Grant one collectible to a user, at most once (unique user+badge). */
    void grant(User user, Badge badge) {
        if (!user?.id || !badge?.id) return
        if (UserBadge.findByUserAndBadge(user, badge)) return
        try {
            new UserBadge(user: user, badge: badge).save(failOnError: true)
        } catch (Exception e) {
            // unique-constraint race — already earned, nothing to do
        }
    }

    /** Whole years elapsed since `d`. */
    private int yearsSince(Date d) {
        long ms = new Date().time - d.time
        return (int) (ms / (365.25d * 24L * 60 * 60 * 1000))
    }
}
