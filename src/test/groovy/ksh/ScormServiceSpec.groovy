package ksh

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class ScormServiceSpec extends Specification
    implements ServiceUnitTest<ScormService>, DataTest {

    Class[] getDomainClassesToMock() {
        [User, Course, CourseEnrollment, ScormCmiData]
    }

    User student
    User teacherUser
    Course course

    def setup() {
        teacherUser = new User(username: 'teacher1', password: 'pass', roleType: 'teacher').save(failOnError: true, flush: true)
        student = new User(username: 'student1', password: 'pass', roleType: 'learner').save(failOnError: true, flush: true)
        course = new Course(shortTitle: 'C1', longTitle: 'Course 1', costKCredits: 0, pointReward: 0, creator: teacherUser).save(failOnError: true, flush: true)
    }

    // ====================================================================
    // getCmiData BASELINES
    // ====================================================================

    void "getCmiData returns defaults for new user"() {
        when:
        def data = service.getCmiData(student, course)

        then:
        data['cmi.core.lesson_status'] == 'not attempted'
        data['cmi.core.student_id'] == student.id.toString()
        data['cmi.core.student_name'] == student.username
        data['cmi.core.credit'] == 'credit'
        data['cmi.core.entry'] == 'ab-initio'
    }

    void "getCmiData returns stored values over defaults"() {
        given:
        new ScormCmiData(user: student, course: course, cmiKey: 'cmi.core.lesson_status', cmiValue: 'incomplete').save(failOnError: true, flush: true)

        when:
        def data = service.getCmiData(student, course)

        then:
        data['cmi.core.lesson_status'] == 'incomplete'
    }

    void "getCmiData returns user name if set"() {
        given:
        student.name = 'Test Student'
        student.save(failOnError: true, flush: true)

        when:
        def data = service.getCmiData(student, course)

        then:
        data['cmi.core.student_name'] == 'Test Student'
    }

    // ====================================================================
    // saveCmiData BASELINES
    // ====================================================================

    void "saveCmiData persists new CMI values"() {
        given:
        new CourseEnrollment(user: student, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(student, course, ['cmi.core.lesson_status': 'incomplete', 'cmi.core.score.raw': '75'])

        then:
        ScormCmiData.findByUserAndCourseAndCmiKey(student, course, 'cmi.core.lesson_status')?.cmiValue == 'incomplete'
        ScormCmiData.findByUserAndCourseAndCmiKey(student, course, 'cmi.core.score.raw')?.cmiValue == '75'
    }

    void "saveCmiData updates existing values"() {
        given:
        new CourseEnrollment(user: student, course: course).save(failOnError: true, flush: true)
        new ScormCmiData(user: student, course: course, cmiKey: 'cmi.core.score.raw', cmiValue: '50').save(failOnError: true, flush: true)

        when:
        service.saveCmiData(student, course, ['cmi.core.score.raw': '90'])

        then:
        ScormCmiData.findByUserAndCourseAndCmiKey(student, course, 'cmi.core.score.raw')?.cmiValue == '90'
        ScormCmiData.countByUserAndCourseAndCmiKey(student, course, 'cmi.core.score.raw') == 1  // no duplicate
    }

    void "saveCmiData marks enrollment complete on passed"() {
        given:
        def enrollment = new CourseEnrollment(user: student, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(student, course, ['cmi.core.lesson_status': 'passed'])

        then:
        def updated = CourseEnrollment.get(enrollment.id)
        updated.progress == 100
        updated.completedAt != null
    }

    void "saveCmiData marks enrollment complete on completed"() {
        given:
        def enrollment = new CourseEnrollment(user: student, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(student, course, ['cmi.core.lesson_status': 'completed'])

        then:
        def updated = CourseEnrollment.get(enrollment.id)
        updated.progress == 100
        updated.completedAt != null
    }

    void "saveCmiData does not re-complete already completed enrollment"() {
        given:
        def completedDate = new Date() - 5
        def enrollment = new CourseEnrollment(user: student, course: course, progress: 100, completedAt: completedDate).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(student, course, ['cmi.core.lesson_status': 'completed'])

        then:
        def updated = CourseEnrollment.get(enrollment.id)
        updated.completedAt == completedDate  // original date preserved
    }

    void "saveCmiData does not mark complete on incomplete status"() {
        given:
        def enrollment = new CourseEnrollment(user: student, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(student, course, ['cmi.core.lesson_status': 'incomplete'])

        then:
        def updated = CourseEnrollment.get(enrollment.id)
        updated.progress == 0
        updated.completedAt == null
    }

    void "saveCmiData sets entry to resume after save"() {
        given:
        new CourseEnrollment(user: student, course: course).save(failOnError: true, flush: true)

        when:
        service.saveCmiData(student, course, ['cmi.core.lesson_status': 'incomplete'])

        then:
        ScormCmiData.findByUserAndCourseAndCmiKey(student, course, 'cmi.core.entry')?.cmiValue == 'resume'
    }
}
