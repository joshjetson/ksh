package ksh

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

/**
 * Access-rule baselines for channel messaging: PM eligibility (canPm via startDm),
 * DM find-or-create idempotence, channel visibility gates, and the dashboard's
 * awaiting-reply derivation. Uses SpringSecurityUtils.doWithAuth to run each call
 * as a specific user (MessageService reads springSecurityService.currentUser).
 */
@Integration
@Rollback
class MessageServiceIntegrationSpec extends Specification {

    MessageService messageService
    DashboardService dashboardService

    User teacher
    User studentA
    User studentB

    def setup() {
        def roleUser = Role.findOrSaveWhere(authority: 'ROLE_USER')
        def roleTeacher = Role.findOrSaveWhere(authority: 'ROLE_TEACHER')
        teacher  = mkUser('t')
        studentA = mkUser('a')
        studentB = mkUser('b')
        [teacher, studentA, studentB].each { UserRole.create(it, roleUser) }
        UserRole.create(teacher, roleTeacher, true)
    }

    private User mkUser(String tag) {
        new User(username: "msg_${tag}_${System.nanoTime()}", password: 'pass123', roleType: 'learner')
            .save(failOnError: true, flush: true)
    }

    private Map asUser(User u, Closure work) {
        SpringSecurityUtils.doWithAuth(u.username) { work() } as Map
    }

    void "students cannot DM unless both are discoverable"() {
        expect: "neither discoverable"
        asUser(studentA) { messageService.startDm([targetUserId: studentB.id]) } == null

        when: "both flip discoverable on"
        new UserSetting(user: studentA, discoverable: true).save(failOnError: true, flush: true)
        new UserSetting(user: studentB, discoverable: true).save(failOnError: true, flush: true)
        def view = asUser(studentA) { messageService.startDm([targetUserId: studentB.id]) }

        then:
        view != null
        view.channel.isDm
    }

    void "a teacher can always DM a student, and startDm is idempotent"() {
        when:
        def v1 = asUser(teacher) { messageService.startDm([targetUserId: studentA.id]) }
        def v2 = asUser(teacher) { messageService.startDm([targetUserId: studentA.id]) }

        then: "same channel both times — no duplicate DM threads"
        v1 != null
        v1.channel.id == v2.channel.id
    }

    void "a block kills student-to-student DMs even when both are discoverable"() {
        given:
        new UserSetting(user: studentA, discoverable: true).save(failOnError: true, flush: true)
        new UserSetting(user: studentB, discoverable: true).save(failOnError: true, flush: true)
        new UserBlock(owner: studentB, blocked: studentA).save(failOnError: true, flush: true)

        expect: "symmetric — the blocked side can't start one either"
        asUser(studentA) { messageService.startDm([targetUserId: studentB.id]) } == null
        asUser(studentB) { messageService.startDm([targetUserId: studentA.id]) } == null
    }

    void "staff channels are invisible to students"() {
        given:
        def staffCh = new Channel(name: 'Staff room', visibility: 'staff').save(failOnError: true, flush: true)

        expect:
        asUser(studentA) { messageService.channelView([channelId: staffCh.id]) } == null
        asUser(teacher)  { messageService.channelView([channelId: staffCh.id]) } != null
        asUser(studentA) { messageService.channelList([:]) }.channels.every { it.visibility != 'staff' }
    }

    void "a banned member cannot open the channel and a muted one cannot post"() {
        given:
        def ch = new Channel(name: 'Lounge', visibility: 'school').save(failOnError: true, flush: true)
        new ChannelMembership(channel: ch, user: studentA, status: 'banned').save(failOnError: true, flush: true)
        new ChannelMembership(channel: ch, user: studentB, status: 'muted').save(failOnError: true, flush: true)

        expect:
        asUser(studentA) { messageService.channelView([channelId: ch.id]) } == null
        with(asUser(studentB) { messageService.channelView([channelId: ch.id]) }) {
            muted
            !canPost
        }
    }

    void "dashboard awaiting-reply counts DMs whose last message is from the student"() {
        given: "a DM where the student spoke last, and one where the teacher did"
        def dm1 = mkDm(teacher, studentA)
        def dm2 = mkDm(teacher, studentB)
        SpringSecurityUtils.doWithAuth(studentA.username) {
            new Message(channel: dm1, author: studentA, body: 'help please').save(failOnError: true, flush: true)
        }
        SpringSecurityUtils.doWithAuth(studentB.username) {
            new Message(channel: dm2, author: studentB, body: 'question').save(failOnError: true, flush: true)
        }
        SpringSecurityUtils.doWithAuth(teacher.username) {
            new Message(channel: dm2, author: teacher, body: 'answered!').save(failOnError: true, flush: true)
        }

        when:
        def stats = asUser(teacher) { dashboardService.adminStats([:]) }

        then: "only the unanswered thread awaits a reply"
        stats.messages.awaiting == 1
        stats.messages.awaitingList*.id == [dm1.id]
    }

    private Channel mkDm(User x, User y) {
        def dm = new Channel(name: 'Direct message', visibility: 'dm').save(failOnError: true, flush: true)
        new ChannelMembership(channel: dm, user: x).save(failOnError: true, flush: true)
        new ChannelMembership(channel: dm, user: y).save(failOnError: true, flush: true)
        dm
    }
}
