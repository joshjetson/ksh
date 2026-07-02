package ksh

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import java.text.SimpleDateFormat

class ScheduleServiceSpec extends Specification
    implements ServiceUnitTest<ScheduleService>, DataTest {

    Class[] getDomainClassesToMock() {
        [User, Course, CourseEnrollment, Event, BlackoutDate]
    }

    def setup() {
        // Unit context has no security session — stub staff status per test.
        service.springSecurityService = Stub(grails.plugin.springsecurity.SpringSecurityService) {
            getCurrentUser() >> null
        }
    }

    private static Date at(String iso) { new SimpleDateFormat('yyyy-MM-dd HH:mm').parse(iso) }

    void "monthView buckets events into the right day cells"() {
        given:
        new Event(title: 'Live class', startsAt: at('2026-07-15 18:00'), color: 'sky').save(failOnError: true, flush: true)
        new Event(title: 'Holiday', startsAt: at('2026-07-15 00:00'), allDay: true).save(failOnError: true, flush: true)

        when:
        def cal = service.monthView([year: '2026', month: '7'])
        def day15 = cal.weeks.flatten().find { it.iso == '2026-07-15' }

        then:
        cal.monthLabel == 'July 2026'
        day15.events.size() == 2
        day15.events*.title.containsAll(['Live class', 'Holiday'])
        day15.events.find { it.title == 'Live class' }.time != null
        day15.events.find { it.title == 'Holiday' }.time == null   // allDay has no time label
    }

    void "monthView marks blackout days for everyone"() {
        given:
        new BlackoutDate(blackoutDate: at('2026-07-04 00:00'), reason: 'Closed for holiday').save(failOnError: true, flush: true)

        when:
        def cal = service.monthView([year: '2026', month: '7'])
        def day4 = cal.weeks.flatten().find { it.iso == '2026-07-04' }

        then:
        day4.blackout.reason == 'Closed for holiday'
        !cal.staff   // stubbed anonymous user is not staff
        day4.enrollments == []   // enrollment layer hidden from non-staff
    }

    void "dayView returns the day's events and navigation"() {
        given:
        new Event(title: 'Speaking practice', startsAt: at('2026-07-15 10:00'), color: 'emerald').save(failOnError: true, flush: true)

        when:
        def day = service.dayView([iso: '2026-07-15'])

        then:
        day.iso == '2026-07-15'
        day.prevIso == '2026-07-14'
        day.nextIso == '2026-07-16'
        day.events*.title == ['Speaking practice']
    }

    void "yearView flags days that have events or blackouts"() {
        given:
        new Event(title: 'E', startsAt: at('2026-03-10 09:00')).save(failOnError: true, flush: true)
        new BlackoutDate(blackoutDate: at('2026-05-01 00:00'), reason: 'Closed').save(failOnError: true, flush: true)

        when:
        def yr = service.yearView([year: '2026'])
        def march = yr.months.find { it.monthNum == 3 }
        def may = yr.months.find { it.monthNum == 5 }

        then:
        yr.year == 2026
        march.weeks.flatten().find { it.iso == '2026-03-10' }.hasEvents
        may.weeks.flatten().find { it.iso == '2026-05-01' }.hasEvents
    }
}
