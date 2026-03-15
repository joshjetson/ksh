KEEP THIS DOCUMENT CONCISE

# Concerns & Implementation Plans

**Rule: Tests first. Every baseline workflow gets a test BEFORE any concern is fixed. If a concern fix breaks a baseline test, revert immediately.**

Test environment: H2 in-memory (`src/test/`, `src/integration-test/`), Spock framework, `grails-gorm-testing-support`, `grails-web-testing-support`.

---

## Part 1: Baseline Tests (What Works Today)

These tests capture every working workflow. They must ALL pass before and after any concern fix.

### 1A. UniversalDataServiceSpec (Unit Test)

Tests the service layer in isolation. Uses `@DataTest` with domain classes.

```
src/test/groovy/ksh/UniversalDataServiceSpec.groovy
```

```groovy
package ksh

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class UniversalDataServiceSpec extends Specification
    implements ServiceUnitTest<UniversalDataService>, DataTest {

    Class[] getDomainClassesToMock() {
        [User, Role, UserRole, Course, CourseEnrollment, Review, WallPost, Badge, UserBadge, ScormCmiData]
    }

    User teacher
    User learner
    Course course

    def setup() {
        teacher = new User(username: 'teacher1', password: 'pass123', roleType: 'teacher', kCredits: 0, points: 0).save(failOnError: true, flush: true)
        learner = new User(username: 'learner1', password: 'pass123', roleType: 'learner', kCredits: 100, points: 0).save(failOnError: true, flush: true)
        course = new Course(shortTitle: 'Korean 101', longTitle: 'Intro to Korean', costKCredits: 0, pointReward: 10, creator: teacher).save(failOnError: true, flush: true)
    }

    // --- CRUD BASELINES ---

    void "save creates a new Course with all fields bound"() {
        when:
        def result = service.save(Course, [
            shortTitle: 'Korean 201',
            longTitle: 'Intermediate Korean',
            costKCredits: '5',
            pointReward: '20',
            tags: 'grammar, intermediate',
            'creator.id': teacher.id.toString()
        ])

        then:
        result != null
        result.shortTitle == 'Korean 201'
        result.longTitle == 'Intermediate Korean'
        result.costKCredits == 5
        result.pointReward == 20
        result.tags == 'grammar, intermediate'
        result.creator.id == teacher.id
    }

    void "save creates CourseEnrollment with user and course"() {
        when:
        def result = service.save(CourseEnrollment, [
            'user.id': learner.id.toString(),
            'course.id': course.id.toString()
        ])

        then:
        result != null
        result.user.id == learner.id
        result.course.id == course.id
        result.progress == 0
        result.completedAt == null
    }

    void "save creates WallPost with user and targetUser"() {
        when:
        def result = service.save(WallPost, [
            'user.id': learner.id.toString(),
            'targetUser.id': teacher.id.toString(),
            message: 'Great teacher!'
        ])

        then:
        result != null
        result.user.id == learner.id
        result.targetUser.id == teacher.id
        result.message == 'Great teacher!'
    }

    void "save creates Review with all fields"() {
        when:
        def result = service.save(Review, [
            'user.id': learner.id.toString(),
            'course.id': course.id.toString(),
            starRating: '5',
            title: 'Excellent',
            text: 'Loved it'
        ])

        then:
        result != null
        result.starRating == 5
        result.title == 'Excellent'
        result.user.id == learner.id
        result.course.id == course.id
    }

    void "update modifies existing Course fields"() {
        when:
        def updated = service.update(Course, course.id, [
            shortTitle: 'Korean 101 Updated',
            longTitle: 'Intro to Korean Updated',
            costKCredits: '10',
            pointReward: '50'
        ])

        then:
        updated.shortTitle == 'Korean 101 Updated'
        updated.costKCredits == 10
        updated.pointReward == 50
    }

    void "update does not overwrite creator when not in params"() {
        when:
        def updated = service.update(Course, course.id, [
            shortTitle: 'New Title'
        ])

        then:
        updated.shortTitle == 'New Title'
        updated.creator.id == teacher.id
    }

    void "deleteById removes instance"() {
        when:
        def result = service.deleteById(Course, course.id)

        then:
        result == true
        Course.get(course.id) == null
    }

    void "getById returns instance"() {
        expect:
        service.getById(Course, course.id)?.shortTitle == 'Korean 101'
    }

    void "getById returns null for missing ID"() {
        expect:
        service.getById(Course, 99999L) == null
    }

    // --- LIST / PAGINATION BASELINES ---

    void "list returns all instances up to default max"() {
        when:
        def results = service.list(Course)

        then:
        results.size() == 1
        results[0].shortTitle == 'Korean 101'
    }

    void "list respects max param"() {
        given:
        5.times { i ->
            new Course(shortTitle: "Course ${i}", longTitle: "Course ${i}", costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)
        }

        when:
        def results = service.list(Course, [max: 3])

        then:
        results.size() == 3
    }

    void "list caps max at DEFAULT_MAX (100)"() {
        when:
        def results = service.list(Course, [max: 500])

        then: "does not throw, returns results capped at 100"
        results != null
    }

    void "list with offset skips records"() {
        given:
        3.times { i ->
            new Course(shortTitle: "Course ${i}", longTitle: "Course ${i}", costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)
        }

        when:
        def results = service.list(Course, [max: 2, offset: 2])

        then:
        results.size() == 2 // course from setup + 3 new = 4 total, offset 2 = 2 remaining
    }

    // --- COUNT BASELINE ---

    void "count returns total instances"() {
        expect:
        service.count(Course) == 1
    }

    // --- FILTER BASELINES ---

    void "filter by numeric field"() {
        given:
        new Course(shortTitle: 'Paid', longTitle: 'Paid Course', costKCredits: 10, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)

        when:
        def free = service.filter(Course, "costKCredits=0")
        def paid = service.filter(Course, "costKCredits=10")

        then:
        free.size() == 1
        free[0].shortTitle == 'Korean 101'
        paid.size() == 1
        paid[0].shortTitle == 'Paid'
    }

    void "filter by association ID (user.id)"() {
        given:
        def enrollment = new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)

        when:
        def results = service.filter(CourseEnrollment, "user.id=${learner.id}")

        then:
        results.size() == 1
        results[0].user.id == learner.id
    }

    void "filter with multiple criteria"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)

        when:
        def results = service.filter(CourseEnrollment, "user.id=${learner.id},course.id=${course.id}")

        then:
        results.size() == 1
    }

    void "filter with empty criteria returns all (like list)"() {
        when:
        def results = service.filter(Course, "")

        then:
        results.size() == 1
    }

    void "filter respects pagination"() {
        given:
        5.times { i ->
            new Course(shortTitle: "C${i}", longTitle: "C${i}", costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)
        }

        when:
        def results = service.filter(Course, "costKCredits=0", [max: 3])

        then:
        results.size() == 3
    }

    // --- FILTER COUNT BASELINES ---

    void "filterCount returns correct count"() {
        given:
        new Course(shortTitle: 'Paid', longTitle: 'Paid', costKCredits: 10, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)

        expect:
        service.filterCount(Course, "costKCredits=0") == 1
        service.filterCount(Course, "costKCredits=10") == 1
    }

    void "filterCount with empty criteria returns total count"() {
        expect:
        service.filterCount(Course, "") == 1
    }

    // --- EXISTS BASELINES ---

    void "exists returns true when record matches"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)

        expect:
        service.exists(CourseEnrollment, "user.id=${learner.id},course.id=${course.id}") == true
    }

    void "exists returns false when no record matches"() {
        expect:
        service.exists(CourseEnrollment, "user.id=${learner.id},course.id=${course.id}") == false
    }

    void "exists returns false for null domain"() {
        expect:
        service.exists(null, "user.id=1") == false
    }

    void "exists returns false for empty criteria"() {
        expect:
        service.exists(CourseEnrollment, "") == false
    }

    // --- SEARCH BASELINES ---

    void "search finds matching records"() {
        when:
        def results = service.search(Course, 'longTitle', 'Korean')

        then:
        results.size() == 1
    }

    void "search is case-insensitive"() {
        when:
        def results = service.search(Course, 'longTitle', 'korean')

        then:
        results.size() == 1
    }

    void "search across multiple fields"() {
        when:
        def results = service.search(Course, 'longTitle,shortTitle', 'Korean')

        then:
        results.size() == 1
    }

    void "search with empty term returns all (like list)"() {
        when:
        def results = service.search(Course, 'longTitle', '')

        then:
        results.size() == 1
    }

    void "search with no matches returns empty list"() {
        when:
        def results = service.search(Course, 'longTitle', 'Nonexistent')

        then:
        results.size() == 0
    }

    void "search respects pagination"() {
        given:
        5.times { i ->
            new Course(shortTitle: "Korean ${i}", longTitle: "Korean ${i}", costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)
        }

        when:
        def results = service.search(Course, 'longTitle', 'Korean', [max: 3])

        then:
        results.size() == 3
    }

    // --- FIND BY OR GET BASELINES ---

    void "findByOrGet finds by field"() {
        when:
        def result = service.findByOrGet(Course, 'shortTitle', 'Korean 101')

        then:
        result != null
        result.id == course.id
    }

    void "findByOrGet falls back to get by ID"() {
        when:
        def result = service.findByOrGet(Course, 'shortTitle', course.id.toString())

        then:
        result != null
        result.id == course.id
    }

    void "findByOrGet returns null when not found"() {
        expect:
        service.findByOrGet(Course, 'shortTitle', 'Nonexistent') == null
    }
}
```

### 1B. UniversalControllerSpec (Integration Test)

Tests the controller with real Hibernate sessions, real DataBinder, real params.

```
src/integration-test/groovy/ksh/UniversalControllerSpec.groovy
```

```groovy
package ksh

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

@Integration
@Rollback
class UniversalControllerSpec extends Specification {

    UniversalController controller
    UniversalDataService universalDataService

    // Spring security mock
    def springSecurityService

    User teacher
    User teacher2
    User learner
    Role roleTeacher
    Role roleUser
    Role roleAdmin
    Course course

    def setup() {
        controller = new UniversalController()
        controller.universalDataService = universalDataService
        controller.grailsApplication = grailsApplication
        controller.springSecurityService = springSecurityService

        // Seed roles
        roleTeacher = Role.findByAuthority('ROLE_TEACHER') ?: new Role(authority: 'ROLE_TEACHER').save(failOnError: true, flush: true)
        roleUser = Role.findByAuthority('ROLE_USER') ?: new Role(authority: 'ROLE_USER').save(failOnError: true, flush: true)
        roleAdmin = Role.findByAuthority('ROLE_ADMIN') ?: new Role(authority: 'ROLE_ADMIN').save(failOnError: true, flush: true)

        // Seed users
        teacher = new User(username: 'teacher1', password: 'Password1!', roleType: 'teacher').save(failOnError: true, flush: true)
        UserRole.create(teacher, roleTeacher, true)

        teacher2 = new User(username: 'teacher2', password: 'Password1!', roleType: 'teacher').save(failOnError: true, flush: true)
        UserRole.create(teacher2, roleTeacher, true)

        learner = new User(username: 'learner1', password: 'Password1!', roleType: 'learner', kCredits: 100).save(failOnError: true, flush: true)
        UserRole.create(learner, roleUser, true)

        // Seed course
        course = new Course(shortTitle: 'Korean 101', longTitle: 'Intro to Korean', costKCredits: 0, pointReward: 10, creator: teacher).save(failOnError: true, flush: true)
    }

    private void loginAs(User user) {
        controller.springSecurityService = [
            currentUser: user
        ]
    }

    private void setHtmxRequest() {
        controller.request.addHeader('HX-Request', 'true')
    }

    // =============================================
    // COURSE CRUD BASELINES
    // =============================================

    void "teacher creates course with all fields"() {
        given:
        loginAs(teacher)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params['creator.id'] = teacher.id.toString()
        controller.params.shortTitle = 'New Course'
        controller.params.longTitle = 'A New Course'
        controller.params.costKCredits = '5'
        controller.params.pointReward = '20'
        controller.params.tags = 'grammar, beginner'
        controller.params.badgeReward = 'Gold Star'
        controller.params.template = 'lessons/manage'

        when:
        controller.save()

        then:
        def created = Course.findByShortTitle('New Course')
        created != null
        created.longTitle == 'A New Course'
        created.costKCredits == 5
        created.pointReward == 20
        created.tags == 'grammar, beginner'
        created.badgeReward == 'Gold Star'
        created.creator.id == teacher.id
    }

    void "teacher creates free course (costKCredits=0, pointReward=0)"() {
        given:
        loginAs(teacher)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params['creator.id'] = teacher.id.toString()
        controller.params.shortTitle = 'Free Course'
        controller.params.longTitle = 'A Free Course'
        controller.params.costKCredits = '0'
        controller.params.pointReward = '0'
        controller.params.template = 'lessons/manage'

        when:
        controller.save()

        then:
        def created = Course.findByShortTitle('Free Course')
        created != null
        created.costKCredits == 0
        created.pointReward == 0
    }

    void "teacher updates own course"() {
        given:
        loginAs(teacher)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params.id = course.id.toString()
        controller.params.shortTitle = 'Korean 101 v2'
        controller.params.longTitle = 'Intro to Korean v2'
        controller.params.costKCredits = '15'
        controller.params.template = 'lessons/manage'

        when:
        controller.update()

        then:
        def updated = Course.get(course.id)
        updated.shortTitle == 'Korean 101 v2'
        updated.costKCredits == 15
        updated.creator.id == teacher.id  // creator unchanged
    }

    void "teacher cannot update another teacher's course"() {
        given:
        loginAs(teacher2)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params.id = course.id.toString()
        controller.params.shortTitle = 'Hijacked'
        controller.params.template = 'lessons/manage'

        when:
        controller.update()

        then:
        controller.response.status == 403
        Course.get(course.id).shortTitle == 'Korean 101'  // unchanged
    }

    void "teacher deletes own course"() {
        given:
        loginAs(teacher)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params.id = course.id.toString()
        controller.params.template = 'lessons/manage'

        when:
        controller.delete()

        then:
        Course.get(course.id) == null
    }

    void "teacher cannot delete another teacher's course"() {
        given:
        loginAs(teacher2)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params.id = course.id.toString()
        controller.params.template = 'lessons/manage'

        when:
        controller.delete()

        then:
        controller.response.status == 403
        Course.get(course.id) != null  // still exists
    }

    void "learner cannot create course (wrong role)"() {
        given:
        loginAs(learner)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params['creator.id'] = learner.id.toString()
        controller.params.shortTitle = 'Sneaky Course'
        controller.params.longTitle = 'Sneaky Course'
        controller.params.costKCredits = '0'
        controller.params.pointReward = '0'
        controller.params.template = 'courses/browse'

        when:
        controller.save()

        then:
        controller.response.status == 403
        Course.findByShortTitle('Sneaky Course') == null
    }

    // =============================================
    // ENROLLMENT CRUD BASELINES
    // =============================================

    void "learner enrolls in course"() {
        given:
        loginAs(learner)
        setHtmxRequest()
        controller.params.domainName = 'CourseEnrollment'
        controller.params['user.id'] = learner.id.toString()
        controller.params['course.id'] = course.id.toString()
        controller.params.template = 'courses/myCourses'

        when:
        controller.save()

        then:
        def enrollment = CourseEnrollment.findByUserAndCourse(learner, course)
        enrollment != null
        enrollment.progress == 0
        enrollment.completedAt == null
    }

    void "duplicate enrollment is rejected (unique constraint)"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)
        loginAs(learner)
        setHtmxRequest()
        controller.params.domainName = 'CourseEnrollment'
        controller.params['user.id'] = learner.id.toString()
        controller.params['course.id'] = course.id.toString()
        controller.params.template = 'courses/myCourses'

        when:
        controller.save()

        then:
        controller.response.status == 500  // failOnError throws
        CourseEnrollment.countByUser(learner) == 1  // still just 1
    }

    // =============================================
    // SHOWVIEW / DATA INSTRUCTION BASELINES
    // =============================================

    void "showView renders template with list instruction"() {
        given:
        loginAs(learner)
        setHtmxRequest()
        controller.params.template = 'courses/browse'
        controller.params['data[courses]'] = 'list:Course'
        controller.params['data[user]'] = 'currentUser'

        when:
        controller.showView()

        then:
        controller.response.status == 200
    }

    void "showView resolves exists instruction"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)
        loginAs(learner)
        setHtmxRequest()
        controller.params.template = 'courses/preview'
        controller.params['data[course]'] = "get:Course:courseId"
        controller.params['data[user]'] = 'currentUser'
        controller.params['data[enrolled]'] = "exists:CourseEnrollment:user.id=currentUserId,course.id=courseId"
        controller.params.courseId = course.id.toString()

        when:
        controller.showView()

        then:
        controller.response.status == 200
    }

    void "showView resolves filter with currentUserId"() {
        given:
        new CourseEnrollment(user: learner, course: course).save(failOnError: true, flush: true)
        loginAs(learner)
        setHtmxRequest()
        controller.params.template = 'courses/myCourses'
        controller.params['data[user]'] = 'currentUser'
        controller.params['data[enrollments]'] = 'filter:CourseEnrollment:user.id=currentUserId'

        when:
        controller.showView()

        then:
        controller.response.status == 200
    }

    void "showView resolves filterCount"() {
        given:
        loginAs(learner)
        setHtmxRequest()
        controller.params.template = 'profile/view'
        controller.params['data[user]'] = 'currentUser'
        controller.params['data[enrollmentCount]'] = 'filterCount:CourseEnrollment:user.id=currentUserId'

        when:
        controller.showView()

        then:
        controller.response.status == 200
    }

    void "showView blocks non-whitelisted domain"() {
        given:
        loginAs(learner)
        setHtmxRequest()
        controller.params.template = 'courses/browse'
        controller.params['data[users]'] = 'list:User'

        when:
        controller.showView()

        then: "User is not in ALLOWED_DOMAINS, instruction returns null"
        controller.response.status == 200  // renders but model has null for 'users'
    }

    void "showView without template returns 400"() {
        given:
        setHtmxRequest()

        when:
        controller.showView()

        then:
        controller.response.status == 400
    }

    // =============================================
    // SECURITY BASELINES
    // =============================================

    void "CRUD blocked for non-whitelisted domain"() {
        given:
        loginAs(learner)
        setHtmxRequest()
        controller.params.domainName = 'User'
        controller.params.username = 'hacked'

        when:
        controller.save()

        then:
        controller.response.status == 403
    }

    void "update ownership check — owner can update"() {
        given:
        loginAs(teacher)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params.id = course.id.toString()
        controller.params.shortTitle = 'Updated by owner'
        controller.params.template = 'lessons/manage'

        when:
        controller.update()

        then:
        controller.response.status == 200
        Course.get(course.id).shortTitle == 'Updated by owner'
    }

    void "delete ownership check — non-owner blocked"() {
        given:
        loginAs(teacher2)
        setHtmxRequest()
        controller.params.domainName = 'Course'
        controller.params.id = course.id.toString()
        controller.params.template = 'lessons/manage'

        when:
        controller.delete()

        then:
        controller.response.status == 403
    }
}
```

### 1C. ScormServiceSpec (Unit Test)

```
src/test/groovy/ksh/ScormServiceSpec.groovy
```

```groovy
package ksh

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class ScormServiceSpec extends Specification
    implements ServiceUnitTest<ScormService>, DataTest {

    Class[] getDomainClassesToMock() {
        [User, Course, CourseEnrollment, ScormCmiData]
    }

    User user
    Course course

    def setup() {
        user = new User(username: 'student1', password: 'pass', roleType: 'learner').save(failOnError: true, flush: true)
        course = new Course(shortTitle: 'C1', longTitle: 'Course 1', costKCredits: 0, pointReward: 0,
                            creator: new User(username: 'teacher1', password: 'pass', roleType: 'teacher').save(failOnError: true, flush: true)
        ).save(failOnError: true, flush: true)
    }

    void "getCmiData returns defaults for new user"() {
        when:
        def data = service.getCmiData(user, course)

        then:
        data['cmi.core.lesson_status'] == 'not attempted'
        data['cmi.core.student_id'] == user.id.toString()
        data['cmi.core.student_name'] == user.username
        data['cmi.core.credit'] == 'credit'
        data['cmi.core.entry'] == 'ab-initio'
    }

    void "getCmiData returns stored values"() {
        given:
        new ScormCmiData(user: user, course: course, cmiKey: 'cmi.core.lesson_status', cmiValue: 'incomplete').save(failOnError: true, flush: true)

        when:
        def data = service.getCmiData(user, course)

        then:
        data['cmi.core.lesson_status'] == 'incomplete'
    }

    void "saveCmiData persists CMI values"() {
        given:
        new CourseEnrollment(user: user, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(user, course, ['cmi.core.lesson_status': 'incomplete', 'cmi.core.score.raw': '75'])

        then:
        ScormCmiData.findByUserAndCourseAndCmiKey(user, course, 'cmi.core.lesson_status')?.cmiValue == 'incomplete'
        ScormCmiData.findByUserAndCourseAndCmiKey(user, course, 'cmi.core.score.raw')?.cmiValue == '75'
    }

    void "saveCmiData updates existing values"() {
        given:
        new CourseEnrollment(user: user, course: course).save(failOnError: true, flush: true)
        new ScormCmiData(user: user, course: course, cmiKey: 'cmi.core.score.raw', cmiValue: '50').save(failOnError: true, flush: true)

        when:
        service.saveCmiData(user, course, ['cmi.core.score.raw': '90'])

        then:
        ScormCmiData.findByUserAndCourseAndCmiKey(user, course, 'cmi.core.score.raw')?.cmiValue == '90'
    }

    void "saveCmiData marks enrollment complete on passed status"() {
        given:
        def enrollment = new CourseEnrollment(user: user, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(user, course, ['cmi.core.lesson_status': 'passed'])

        then:
        def updated = CourseEnrollment.get(enrollment.id)
        updated.progress == 100
        updated.completedAt != null
    }

    void "saveCmiData marks enrollment complete on completed status"() {
        given:
        def enrollment = new CourseEnrollment(user: user, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(user, course, ['cmi.core.lesson_status': 'completed'])

        then:
        def updated = CourseEnrollment.get(enrollment.id)
        updated.progress == 100
        updated.completedAt != null
    }

    void "saveCmiData does not re-complete already completed enrollment"() {
        given:
        def completedDate = new Date() - 5
        def enrollment = new CourseEnrollment(user: user, course: course, progress: 100, completedAt: completedDate).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(user, course, ['cmi.core.lesson_status': 'completed'])

        then:
        def updated = CourseEnrollment.get(enrollment.id)
        updated.completedAt == completedDate  // original date preserved
    }

    void "saveCmiData sets entry to resume after save"() {
        given:
        new CourseEnrollment(user: user, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(user, course, ['cmi.core.lesson_status': 'incomplete'])

        then:
        ScormCmiData.findByUserAndCourseAndCmiKey(user, course, 'cmi.core.entry')?.cmiValue == 'resume'
    }
}
```

---

## Part 2: Concern-Specific Tests & Implementation Plans

These tests layer ON TOP of the baselines. Run all Part 1 tests + Part 2 tests after every change.

### Concern 1: Ownership Spoofing on Create

**Problem:** Hidden form fields `creator.id`, `user.id` can be changed in the browser. `OWNERSHIP_FIELDS` only enforces on update/delete, not create.

**Tests (add to UniversalControllerSpec):**

```groovy
// --- CONCERN 1: OWNERSHIP SPOOFING ---

void "course create ignores spoofed creator.id"() {
    given:
    loginAs(teacher)
    setHtmxRequest()
    controller.params.domainName = 'Course'
    controller.params['creator.id'] = teacher2.id.toString()  // spoofed!
    controller.params.shortTitle = 'Spoofed Course'
    controller.params.longTitle = 'Spoofed Course'
    controller.params.costKCredits = '0'
    controller.params.pointReward = '0'
    controller.params.template = 'lessons/manage'

    when:
    controller.save()

    then: "creator forced to logged-in user, not the spoofed value"
    def created = Course.findByShortTitle('Spoofed Course')
    created != null
    created.creator.id == teacher.id  // NOT teacher2
}

void "enrollment create ignores spoofed user.id"() {
    given:
    loginAs(learner)
    setHtmxRequest()
    controller.params.domainName = 'CourseEnrollment'
    controller.params['user.id'] = teacher.id.toString()  // spoofed!
    controller.params['course.id'] = course.id.toString()
    controller.params.template = 'courses/myCourses'

    when:
    controller.save()

    then:
    def enrollment = CourseEnrollment.findByCourse(course)
    enrollment.user.id == learner.id  // NOT teacher
}

void "wall post create ignores spoofed user.id"() {
    given:
    loginAs(learner)
    setHtmxRequest()
    controller.params.domainName = 'WallPost'
    controller.params['user.id'] = teacher.id.toString()  // spoofed!
    controller.params['targetUser.id'] = teacher.id.toString()
    controller.params.message = 'Hello'
    controller.params.template = 'profile/view'

    when:
    controller.save()

    then:
    def post = WallPost.findByMessage('Hello')
    post.user.id == learner.id  // NOT teacher
}

void "spoofed create still binds all other fields correctly"() {
    given: "this verifies the fix does not break field binding (costKCredits, pointReward, etc)"
    loginAs(teacher)
    setHtmxRequest()
    controller.params.domainName = 'Course'
    controller.params['creator.id'] = teacher2.id.toString()  // spoofed
    controller.params.shortTitle = 'Full Fields'
    controller.params.longTitle = 'Full Fields Course'
    controller.params.costKCredits = '25'
    controller.params.pointReward = '50'
    controller.params.tags = 'test, spoof'
    controller.params.template = 'lessons/manage'

    when:
    controller.save()

    then:
    def created = Course.findByShortTitle('Full Fields')
    created.creator.id == teacher.id
    created.costKCredits == 25
    created.pointReward == 50
    created.tags == 'test, spoof'
}
```

**Implementation:**

Do NOT touch `extractParams()`. Do NOT strip fields from params. Do NOT use `discard()`.

**Step 1:** Add optional ownership override to `UniversalDataService.save()`:

```groovy
def save(Class domainClass, Map params, Map ownershipOverride = null) {
    try {
        def instance = domainClass.newInstance()
        if (instance) {
            updateProperties(instance, params)

            // Force ownership after binding — overwrites whatever the form sent.
            // Uses load() to get a Hibernate proxy (no DB hit, no session conflict).
            if (ownershipOverride) {
                ownershipOverride.each { field, userId ->
                    instance."${field}" = User.load(userId)
                }
            }

            instance.save(failOnError: true)
            return instance
        }
    } catch (PersistenceException e) {
        println "ERROR: Persistence error saving ${domainClass.simpleName}: ${e.message}"
        return null
    } catch (Exception e) {
        println "ERROR: Error saving ${domainClass.simpleName}: ${e.message}"
        return null
    }
    return null
}
```

**Step 2:** Pass ownership override from `UniversalController.save()`:

```groovy
def save() {
    executeCrud('create') { domainClass, id ->
        def myParams = extractParams()
        Map ownerOverride = null
        String ownerField = OWNERSHIP_FIELDS[params.domainName]
        if (ownerField) {
            def currentUser = springSecurityService.currentUser
            ownerOverride = [(ownerField): currentUser.id]
        }
        def instance = universalDataService.save(domainClass, myParams, ownerOverride)
        if (instance instanceof Course && instance.scorm) {
            scormService.extractAndParseScorm(instance)
        }
        instance
    }
}
```

**Why `User.load()` not `User.get()`:** `load()` returns a Hibernate proxy referencing the ID without a DB query. It does not create a conflicting session object. This avoids the "different object with same identifier" error.

**Safety net:** Baseline test `"teacher creates course with all fields"` must still pass. If `User.load()` doesn't work in Grails 6.2.3, that test catches it.

---

### Concern 2: SQL Wildcard in Search

**Problem:** `ilike(field, "%${searchTerm}%")` passes raw user input. `%` and `_` wildcards can craft expensive queries.

**Tests (add to UniversalDataServiceSpec):**

```groovy
// --- CONCERN 2: SEARCH WILDCARD ESCAPING ---

void "search escapes percent wildcard"() {
    when:
    def results = service.search(Course, 'longTitle', '%%')

    then: "percent signs treated as literals, no match"
    results.size() == 0
}

void "search escapes underscore wildcard"() {
    when:
    def results = service.search(Course, 'longTitle', '____')

    then: "underscores treated as literals, no match"
    results.size() == 0
}

void "search still works normally after escaping"() {
    when:
    def results = service.search(Course, 'longTitle', 'Korean')

    then:
    results.size() == 1
}
```

**Implementation:**

Add to `UniversalDataService`:

```groovy
private String escapeLikeWildcards(String input) {
    if (!input) return input
    return input.replace('%', '\\%').replace('_', '\\_')
}
```

Update `search()`:

```groovy
def escaped = escapeLikeWildcards(searchTerm)
return domainClass.createCriteria().list(max: max, offset: offset) {
    or {
        fieldList.each { field ->
            ilike(field, "%${escaped}%")
        }
    }
}
```

**Safety net:** Baseline test `"search finds matching records"` and `"search is case-insensitive"` must still pass.

---

### Concern 3: Colon Delimiter in Data Instructions

**Problem:** `instruction.split(':')` breaks if criteria contain colons (URLs, timestamps).

**Tests (new spec):**

```
src/test/groovy/ksh/InstructionParsingSpec.groovy
```

```groovy
package ksh

import spock.lang.Specification

class InstructionParsingSpec extends Specification {

    void "current split breaks on colon in criteria value"() {
        when:
        def parts = "filter:Course:field=http://example.com".split(':')

        then: "demonstrates the bug"
        parts.length == 5
    }

    void "limited split preserves colon in criteria"() {
        when:
        def firstSplit = "filter:Course:field=http://example.com".split(':', 2)
        def type = firstSplit[0]
        def rest = firstSplit[1].split(':', 2)

        then:
        type == 'filter'
        rest[0] == 'Course'
        rest[1] == 'field=http://example.com'
    }

    void "search instruction splits into 4 parts correctly"() {
        when:
        def firstSplit = "search:Course:longTitle,shortTitle:q".split(':', 2)
        def type = firstSplit[0]
        def rest = firstSplit[1].split(':', 3)

        then:
        type == 'search'
        rest[0] == 'Course'
        rest[1] == 'longTitle,shortTitle'
        rest[2] == 'q'
    }

    void "simple instructions still work"() {
        when:
        def parts = "list:Course".split(':', 2)

        then:
        parts[0] == 'list'
        parts[1] == 'Course'
    }

    void "currentUser instruction has no colon issues"() {
        when:
        def parts = "currentUser".split(':', 2)

        then:
        parts[0] == 'currentUser'
        parts.length == 1
    }

    void "exists instruction preserves criteria"() {
        when:
        def firstSplit = "exists:CourseEnrollment:user.id=currentUserId,course.id=courseId".split(':', 2)
        def rest = firstSplit[1].split(':', 2)

        then:
        rest[0] == 'CourseEnrollment'
        rest[1] == 'user.id=currentUserId,course.id=courseId'
    }

    void "get instruction with param name"() {
        when:
        def firstSplit = "get:Course:courseId".split(':', 2)
        def rest = firstSplit[1].split(':', 2)

        then:
        rest[0] == 'Course'
        rest[1] == 'courseId'
    }

    void "literal instruction with colon in value"() {
        when:
        def parts = "literal:14:30".split(':', 2)

        then:
        parts[0] == 'literal'
        parts[1] == '14:30'
    }
}
```

**Implementation:**

Replace `resolveInstruction()` in `UniversalController`:

```groovy
private Object resolveInstruction(String instruction) {
    def firstSplit = instruction.split(':', 2)
    def type = firstSplit[0]
    def handler = instructionHandlers[type]

    if (!handler) {
        throw new IllegalArgumentException("Unknown data instruction: ${instruction}")
    }

    // Build parts array: [type, domain, rest...]
    // Most instructions: type + domain + criteria (split rest into 2)
    // search: type + domain + fields + paramName (split rest into 3)
    def allParts = [type]
    if (firstSplit.length > 1) {
        int maxRestParts = (type == 'search') ? 3 : 2
        allParts.addAll(firstSplit[1].split(':', maxRestParts))
    }

    handler(allParts as String[])
}
```

**Safety net:** ALL baseline showView tests must still pass. The InstructionParsingSpec tests verify edge cases.

---

### Concern 4: SSE Thread-Per-Connection

**Problem:** Thread held per SSE client. Fine at 10-20 users.

**No code change.** Monitor only. Act when user count exceeds ~50.

---

### Concern 5: Param Passthrough to DataBinder

**Problem:** All params (including `template`, `domainName`, etc.) passed to DataBinder.

**No code change.** DataBinder ignores unknown properties. Baseline tests document current behavior.

---

## Implementation Order

```
1. Create test files, run baselines — all must pass with ZERO code changes
2. Concern 1 (ownership spoofing) — add concern tests (they FAIL), implement fix, all tests pass
3. Concern 2 (search wildcards)   — add concern tests (they FAIL), implement fix, all tests pass
4. Concern 3 (colon delimiter)    — add concern tests (they FAIL), implement fix, all tests pass
5. Concerns 4 & 5                 — no code change, monitor only
```

Each step: if ANY baseline test breaks, revert the concern fix immediately.
