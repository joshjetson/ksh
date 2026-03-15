package ksh

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

/**
 * Integration tests for queries that require a real database.
 * The GORM DataTest mock doesn't support nested property paths
 * in createCriteria (e.g. "user.id=X"), so these tests use H2.
 */
@Integration
@Rollback
class UniversalDataServiceIntegrationSpec extends Specification {

    UniversalDataService universalDataService

    User teacher
    User learner
    Course course

    def setup() {
        teacher = new User(username: "teacher_${System.nanoTime()}", password: 'pass123', roleType: 'teacher', kCredits: 0, points: 0).save(failOnError: true, flush: true)
        learner = new User(username: "learner_${System.nanoTime()}", password: 'pass123', roleType: 'learner', kCredits: 100, points: 0).save(failOnError: true, flush: true)
        course = new Course(shortTitle: 'Korean 101', longTitle: 'Intro to Korean', costKCredits: 0, pointReward: 10, creator: teacher).save(failOnError: true, flush: true)
    }

    // ====================================================================
    // FILTER BY ASSOCIATION — requires real DB
    // ====================================================================

    void "filter by user.id on CourseEnrollment"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)

        when:
        def results = universalDataService.filter(CourseEnrollment, "user.id=${learner.id}")

        then:
        results.size() == 1
        results[0].user.id == learner.id
    }

    void "filter by multiple association criteria"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)

        when:
        def results = universalDataService.filter(CourseEnrollment, "user.id=${learner.id},course.id=${course.id}")

        then:
        results.size() == 1
    }

    void "filter by creator.id on Course"() {
        when:
        def results = universalDataService.filter(Course, "creator.id=${teacher.id}")

        then:
        results.size() >= 1
        results.every { it.creator.id == teacher.id }
    }

    void "filter returns empty for non-matching association"() {
        when:
        def results = universalDataService.filter(CourseEnrollment, "user.id=${learner.id}")

        then: "no enrollment exists"
        results.size() == 0
    }

    // ====================================================================
    // EXISTS BY ASSOCIATION — requires real DB
    // ====================================================================

    void "exists returns true for matching enrollment"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)

        expect:
        universalDataService.exists(CourseEnrollment, "user.id=${learner.id},course.id=${course.id}") == true
    }

    void "exists returns false when no enrollment"() {
        expect:
        universalDataService.exists(CourseEnrollment, "user.id=${learner.id},course.id=${course.id}") == false
    }

    void "exists works for Course by creator"() {
        expect:
        universalDataService.exists(Course, "creator.id=${teacher.id}") == true
        universalDataService.exists(Course, "creator.id=${learner.id}") == false
    }

    // ====================================================================
    // FILTER COUNT BY ASSOCIATION — requires real DB
    // ====================================================================

    void "filterCount by association"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)

        expect:
        universalDataService.filterCount(CourseEnrollment, "user.id=${learner.id}") == 1
    }

    void "filterCount returns 0 for no match"() {
        expect:
        universalDataService.filterCount(CourseEnrollment, "user.id=${learner.id}") == 0
    }

    // ====================================================================
    // SAVE WITH DOTTED KEYS — tests real GrailsWebDataBinder behavior
    // In the real app, forms send 'creator.id' as a dotted key.
    // The DataBinder with SimpleMapDataBindingSource handles this.
    // ====================================================================

    void "save Course with nested map for creator"() {
        when:
        def result = universalDataService.save(Course, [
            shortTitle: 'Integration Test Course',
            longTitle: 'Integration Test Course',
            costKCredits: '5',
            pointReward: '10',
            creator: [id: teacher.id]
        ])

        then:
        result != null
        result.shortTitle == 'Integration Test Course'
        result.costKCredits == 5
        result.pointReward == 10
        result.creator.id == teacher.id
    }

    void "save CourseEnrollment with nested maps"() {
        when:
        def result = universalDataService.save(CourseEnrollment, [
            user: [id: learner.id],
            course: [id: course.id]
        ])

        then:
        result != null
        result.user.id == learner.id
        result.course.id == course.id
        result.progress == 0
    }

    void "save WallPost with nested maps"() {
        when:
        def result = universalDataService.save(WallPost, [
            user: [id: learner.id],
            targetUser: [id: teacher.id],
            message: 'Integration test post'
        ])

        then:
        result != null
        result.user.id == learner.id
        result.targetUser.id == teacher.id
        result.message == 'Integration test post'
    }

    void "save Review with nested maps"() {
        when:
        def result = universalDataService.save(Review, [
            user: [id: learner.id],
            course: [id: course.id],
            starRating: '4',
            title: 'Good course'
        ])

        then:
        result != null
        result.starRating == 4
        result.user.id == learner.id
        result.course.id == course.id
    }

    // ====================================================================
    // CONCERN 1: OWNERSHIP OVERRIDE ON CREATE
    // These test that save() with ownershipOverride forces the correct
    // owner regardless of what the form params say.
    // ====================================================================

    void "ownership override forces creator on Course"() {
        when: "params say creator is learner, but override says teacher"
        def result = universalDataService.save(Course, [
            shortTitle: 'Spoofed Course',
            longTitle: 'Spoofed Course',
            costKCredits: '0',
            pointReward: '0',
            creator: [id: learner.id]  // spoofed
        ], [creator: teacher.id])      // override

        then: "creator is forced to teacher"
        result != null
        result.creator.id == teacher.id
        result.shortTitle == 'Spoofed Course'
        result.costKCredits == 0
    }

    void "ownership override forces user on CourseEnrollment"() {
        when: "params say user is teacher, but override says learner"
        def result = universalDataService.save(CourseEnrollment, [
            user: [id: teacher.id],    // spoofed
            course: [id: course.id]
        ], [user: learner.id])         // override

        then:
        result != null
        result.user.id == learner.id
        result.course.id == course.id
    }

    void "ownership override forces user on WallPost"() {
        when:
        def result = universalDataService.save(WallPost, [
            user: [id: teacher.id],    // spoofed
            targetUser: [id: teacher.id],
            message: 'Spoofed post'
        ], [user: learner.id])         // override

        then:
        result != null
        result.user.id == learner.id
        result.message == 'Spoofed post'
    }

    void "ownership override does not break other fields"() {
        when:
        def result = universalDataService.save(Course, [
            shortTitle: 'Full Fields',
            longTitle: 'Full Fields Course',
            costKCredits: '25',
            pointReward: '50',
            tags: 'test, override',
            badgeReward: 'Gold Star',
            creator: [id: learner.id]  // spoofed
        ], [creator: teacher.id])      // override

        then: "all fields bind correctly, only creator is overridden"
        result != null
        result.creator.id == teacher.id
        result.costKCredits == 25
        result.pointReward == 50
        result.tags == 'test, override'
        result.badgeReward == 'Gold Star'
    }

    void "save without ownership override still works (backwards compatible)"() {
        when:
        def result = universalDataService.save(Course, [
            shortTitle: 'No Override',
            longTitle: 'No Override',
            costKCredits: '0',
            pointReward: '0',
            creator: [id: teacher.id]
        ])

        then:
        result != null
        result.creator.id == teacher.id
    }

    // ====================================================================
    // CONCERN 2: SEARCH WILDCARD ESCAPING — requires real DB
    // GORM DataTest mock doesn't respect escape sequences in ilike.
    // ====================================================================

    void "search escapes percent wildcard"() {
        when: "search term is just percent signs"
        def results = universalDataService.search(Course, 'longTitle', '%%')

        then: "treated as literal, no match"
        results.size() == 0
    }

    void "search escapes underscore wildcard"() {
        when: "search term is just underscores"
        def results = universalDataService.search(Course, 'longTitle', '____')

        then: "treated as literal, no match"
        results.size() == 0
    }

    void "search still works after escaping"() {
        given: "a course with a unique title"
        new Course(shortTitle: 'Xylophone99', longTitle: 'Xylophone99 Unique', costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)

        when: "normal search term"
        def results = universalDataService.search(Course, 'longTitle', 'Xylophone99')

        then:
        results.size() == 1
        results[0].longTitle == 'Xylophone99 Unique'
    }

    void "search still case insensitive after escaping"() {
        given:
        new Course(shortTitle: 'Zephyr42', longTitle: 'Zephyr42 Unique', costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)

        when:
        def results = universalDataService.search(Course, 'longTitle', 'zephyr42')

        then:
        results.size() == 1
        results[0].longTitle == 'Zephyr42 Unique'
    }

    // ====================================================================
    // UPDATE preserves associations
    // ====================================================================

    void "update Course preserves creator when not in params"() {
        when:
        def updated = universalDataService.update(Course, course.id, [
            shortTitle: 'Updated Title'
        ])

        then:
        updated.shortTitle == 'Updated Title'
        updated.creator.id == teacher.id
    }

    void "update Course changes simple fields without breaking"() {
        when:
        def updated = universalDataService.update(Course, course.id, [
            shortTitle: 'New Short',
            longTitle: 'New Long',
            costKCredits: '25',
            pointReward: '99',
            tags: 'updated, test'
        ])

        then:
        updated.shortTitle == 'New Short'
        updated.longTitle == 'New Long'
        updated.costKCredits == 25
        updated.pointReward == 99
        updated.tags == 'updated, test'
        updated.creator.id == teacher.id
    }
}
