package ksh

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

/**
 * Classroom-cluster baselines: the single-active Term invariant, membership
 * uniqueness, the teacher-only ClassroomStaff validator, and saveBatch attendance
 * idempotence (the roster day-save path).
 */
@Integration
@Rollback
class ClassroomIntegrationSpec extends Specification {

    UniversalDataService universalDataService

    User teacher
    User learner
    Classroom room
    Term term

    def setup() {
        def roleUser = Role.findOrSaveWhere(authority: 'ROLE_USER')
        def roleTeacher = Role.findOrSaveWhere(authority: 'ROLE_TEACHER')
        teacher = new User(username: "ct_${System.nanoTime()}", password: 'pass123', roleType: 'teacher').save(failOnError: true, flush: true)
        learner = new User(username: "cl_${System.nanoTime()}", password: 'pass123', roleType: 'learner').save(failOnError: true, flush: true)
        UserRole.create(teacher, roleTeacher)
        UserRole.create(learner, roleUser, true)
        room = new Classroom(name: "Room ${System.nanoTime()}").save(failOnError: true, flush: true)
        term = new Term(name: "Term ${System.nanoTime()}", startsOn: new Date() - 30, endsOn: new Date() + 60, active: true).save(failOnError: true, flush: true)
    }

    void "only one term is active at a time"() {
        when: "a second term is created active"
        def t2 = new Term(name: "Next ${System.nanoTime()}", startsOn: new Date() + 61, endsOn: new Date() + 120, active: true).save(failOnError: true, flush: true)

        then: "the previous active term was deactivated by the beforeInsert hook"
        Term.countByActive(true) == 1
        Term.findByActive(true).id == t2.id
    }

    void "classroom membership is unique per user, classroom and term"() {
        expect:
        universalDataService.save(ClassroomMembership, [user: [id: learner.id], classroom: [id: room.id], term: [id: term.id]]) != null
        universalDataService.save(ClassroomMembership, [user: [id: learner.id], classroom: [id: room.id], term: [id: term.id]]) == null
    }

    void "a student cannot be assigned as classroom staff"() {
        expect:
        universalDataService.save(ClassroomStaff, [staff: [id: learner.id], classroom: [id: room.id]]) == null
        universalDataService.save(ClassroomStaff, [staff: [id: teacher.id], classroom: [id: room.id]]) != null
    }

    void "saveBatch attendance is idempotent on the user+day natural key"() {
        given:
        String today = new Date().format('yyyy-MM-dd')

        when: "the same day is submitted twice with a corrected status"
        def r1 = universalDataService.upsertBatch(Attendance,
            [[user: [id: learner.id], day: today, status: 'absent']], ['user', 'day'])
        def r2 = universalDataService.upsertBatch(Attendance,
            [[user: [id: learner.id], day: today, status: 'present']], ['user', 'day'])

        then: "one row, updated in place"
        r1 == [inserted: 1, updated: 0]
        r2 == [inserted: 0, updated: 1]
        Attendance.countByUser(learner) == 1
        Attendance.findByUser(learner).status == 'present'
    }

    void "grade constraints hold and gradedBy stamps"() {
        when:
        def g = universalDataService.save(Grade,
            [user: [id: learner.id], term: [id: term.id], subject: 'Speaking', period: 'Q1', score: '88'],
            [gradedBy: teacher.id])
        def bad = universalDataService.save(Grade,
            [user: [id: learner.id], term: [id: term.id], subject: 'Speaking', period: 'Q1', score: '140'])

        then:
        g != null
        g.gradedBy.id == teacher.id
        bad == null   // score capped at 100
    }
}
