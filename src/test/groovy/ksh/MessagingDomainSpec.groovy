package ksh

import grails.testing.gorm.DataTest
import spock.lang.Specification

/**
 * Validator baselines for the channel-messaging domains. There is no authenticated
 * security context in unit tests, so Message.posterIsManager() is false and
 * posterId() is null — which is exactly the "anonymous/non-manager poster"
 * perspective the staff-channel gate must reject.
 */
class MessagingDomainSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {
        [User, Role, UserRole, Channel, ChannelMembership, Message]
    }

    User user

    def setup() {
        user = new User(username: 'chatter', password: 'pass123', roleType: 'learner').save(failOnError: true, flush: true)
    }

    void "message posts to a school channel"() {
        given:
        def ch = new Channel(name: 'Lounge', visibility: 'school').save(failOnError: true, flush: true)

        expect:
        new Message(channel: ch, author: user, body: 'hello').save(flush: true) != null
    }

    void "message to a staff channel is rejected for a non-manager poster"() {
        given:
        def ch = new Channel(name: 'Staff room', visibility: 'staff').save(failOnError: true, flush: true)

        when:
        def msg = new Message(channel: ch, author: user, body: 'intruding')
        msg.save(flush: true)

        then:
        msg.hasErrors()
        msg.errors['channel']?.code == 'channel.staffOnly'
    }

    void "channel rejects an unknown visibility"() {
        expect:
        new Channel(name: 'X', visibility: 'secret').save() == null
    }

    void "membership defaults to active and is unique per channel and user"() {
        given:
        def ch = new Channel(name: 'Lounge', visibility: 'school').save(failOnError: true, flush: true)

        expect:
        new ChannelMembership(channel: ch, user: user).save(flush: true) != null
        new ChannelMembership(channel: ch, user: user, status: 'muted').save(flush: true) == null
    }

    void "a moderator cannot mute an admin"() {
        given:
        def ch = new Channel(name: 'Lounge', visibility: 'school').save(failOnError: true, flush: true)
        def adminRole = new Role(authority: 'ROLE_ADMIN').save(failOnError: true, flush: true)
        def admin = new User(username: 'boss', password: 'pass123', roleType: 'admin').save(failOnError: true, flush: true)
        UserRole.create(admin, adminRole, true)

        when:
        def cm = new ChannelMembership(channel: ch, user: admin, status: 'muted')
        cm.save(flush: true)

        then:
        cm.hasErrors()
        cm.errors['user']?.code == 'cannotModerateAdmin'
    }
}
