package ksh

/**
 * How a *global* (auto-granted) collectible is earned. The grant engine
 * (RewardService.checkMilestones) switches on this, comparing the user's stat
 * against the badge's unlockThreshold. Add a value + a case to introduce a new
 * milestone type without touching the schema — kept deliberately open-ended.
 *
 * NONE = not a global milestone (course-attached or manually granted only).
 */
enum UnlockType {

    NONE,          // not auto-granted
    POINTS,        // reach `unlockThreshold` total points
    COURSE_COUNT,  // complete `unlockThreshold` courses
    ANNIVERSARY    // `unlockThreshold` years since joining the platform

    // Future: STREAK, REFERRALS, PERFECT_SCORES, …

    String getLabel() {
        switch (this) {
            case POINTS:       return 'Points threshold'
            case COURSE_COUNT: return 'Courses completed'
            case ANNIVERSARY:  return 'Account anniversary (years)'
            default:           return 'Not a milestone'
        }
    }
}
