package ksh

import grails.gorm.transactions.ReadOnly
import grails.plugin.springsecurity.SpringSecurityService

import java.text.SimpleDateFormat

/**
 * Calendar month/day/year grids. Exposed via the `service:` data instruction
 * (building a calendar grid is custom aggregation, not a simple data instruction):
 *   data[cal]=service:scheduleService:monthView   (+ year/month params)
 *   data[day]=service:scheduleService:dayView     (+ iso param)
 *   data[cal]=service:scheduleService:yearView    (+ year param)
 *
 * Three layers per day cell:
 *   events    — school-wide Events, visible to ALL signed-in users
 *   blackout  — closure days (BlackoutDate), visible to all (rendered muted)
 *   enrollments — enrollment activity, STAFF-ONLY (empty for students)
 *
 * Returns fully-built, all-scalar structures so the GSP renders with plain g:each
 * and nothing lazy-loads after the @ReadOnly tx closes.
 */
class ScheduleService {

    SpringSecurityService springSecurityService

    @ReadOnly
    Map monthView(Map params) {
        Date today = new Date().clearTime()
        boolean staff = isStaff()

        Calendar c = Calendar.getInstance()
        c.setTime(today)
        if (params?.year)  c.set(Calendar.YEAR, (params.year as int))
        if (params?.month) c.set(Calendar.MONTH, (params.month as int) - 1)
        c.set(Calendar.DAY_OF_MONTH, 1); zeroTime(c)
        int year  = c.get(Calendar.YEAR)
        int month = c.get(Calendar.MONTH)
        String monthLabel = new SimpleDateFormat('MMMM yyyy').format(c.time)

        // Grid starts on the Sunday on/before the 1st; 6 rows × 7 = 42 cells.
        Calendar grid = (Calendar) c.clone()
        grid.add(Calendar.DAY_OF_MONTH, -(grid.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
        Date gridStart = grid.time
        Calendar gridEndCal = (Calendar) grid.clone(); gridEndCal.add(Calendar.DAY_OF_MONTH, 42)
        Date gridEnd = gridEndCal.time

        Map<String, List> eventsByDay   = bucketEvents(gridStart, gridEnd)
        Map<String, Map>  blackoutByDay = bucketBlackouts(gridStart, gridEnd)
        Map<String, List> enrollByDay   = staff ? bucketEnrollments(gridStart, gridEnd) : [:].withDefault { [] }

        SimpleDateFormat key = new SimpleDateFormat('yyyy-MM-dd')
        List weeks = []
        Calendar cur = (Calendar) grid.clone()
        for (int w = 0; w < 6; w++) {
            List week = []
            for (int d = 0; d < 7; d++) {
                String k = key.format(cur.time)
                List dayEnroll = enrollByDay[k] ?: []
                week << [iso: k, day: cur.get(Calendar.DAY_OF_MONTH),
                         inMonth: cur.get(Calendar.MONTH) == month,
                         // today only highlights in THIS month — not the padding days that
                         // spill in from adjacent months (which made every month show a "today").
                         today: cur.time == today && cur.get(Calendar.MONTH) == month,
                         events: eventsByDay[k] ?: [],
                         blackout: blackoutByDay[k],
                         enrollCount: dayEnroll.size(),
                         enrollments: dayEnroll]
                cur.add(Calendar.DAY_OF_MONTH, 1)
            }
            weeks << week
        }

        Calendar prev = (Calendar) c.clone(); prev.add(Calendar.MONTH, -1)
        Calendar next = (Calendar) c.clone(); next.add(Calendar.MONTH,  1)
        [monthLabel: monthLabel, year: year, month: month + 1, weeks: weeks, staff: staff,
         prev: [year: prev.get(Calendar.YEAR), month: prev.get(Calendar.MONTH) + 1],
         next: [year: next.get(Calendar.YEAR), month: next.get(Calendar.MONTH) + 1]]
    }

    @ReadOnly
    Map dayView(Map params) {
        // Default to today when no day is given (e.g. switching to Day view from Month/Year).
        String iso = params?.iso ?: new SimpleDateFormat('yyyy-MM-dd').format(new Date().clearTime())
        Date start = new SimpleDateFormat('yyyy-MM-dd').parse(iso)
        boolean staff = isStaff()
        Calendar e = Calendar.getInstance(); e.setTime(start); e.add(Calendar.DAY_OF_MONTH, 1)
        Calendar cal = Calendar.getInstance(); cal.setTime(start)
        SimpleDateFormat k = new SimpleDateFormat('yyyy-MM-dd')
        Calendar p = (Calendar) cal.clone(); p.add(Calendar.DAY_OF_MONTH, -1)
        Calendar n = (Calendar) cal.clone(); n.add(Calendar.DAY_OF_MONTH, 1)
        [iso  : iso,
         label: new SimpleDateFormat('EEEE, MMMM d, yyyy').format(start),
         year : cal.get(Calendar.YEAR), month: cal.get(Calendar.MONTH) + 1,
         prevIso: k.format(p.time), nextIso: k.format(n.time),
         isToday: start == new Date().clearTime(),
         staff: staff,
         events: bucketEvents(start, e.time)[iso] ?: [],
         blackout: bucketBlackouts(start, e.time)[iso],
         enrollments: staff ? (bucketEnrollments(start, e.time)[iso] ?: []) : []]
    }

    /** A year of 12 mini-month grids — each a 6×7 grid of day cells marking in-month days,
     *  today, and which days have events. Backs the `service:scheduleService:yearView`
     *  instruction (+ optional year param). */
    @ReadOnly
    Map yearView(Map params) {
        Date today = new Date().clearTime()
        Calendar c = Calendar.getInstance(); c.setTime(today)
        if (params?.year) c.set(Calendar.YEAR, params.year as int)
        int year = c.get(Calendar.YEAR)

        // Which days in the year have any event (one query for the whole year).
        Calendar ys = Calendar.getInstance(); ys.clear(); ys.set(year, 0, 1)
        Calendar ye = Calendar.getInstance(); ye.clear(); ye.set(year + 1, 0, 1)
        SimpleDateFormat key = new SimpleDateFormat('yyyy-MM-dd')
        Set<String> eventDays = [] as Set
        Event.createCriteria().list { ge('startsAt', ys.time); lt('startsAt', ye.time) }
                              .each { eventDays << key.format(it.startsAt) }
        BlackoutDate.createCriteria().list { ge('blackoutDate', ys.time); lt('blackoutDate', ye.time) }
                                     .each { eventDays << key.format(it.blackoutDate) }

        List months = []
        for (int m = 0; m < 12; m++) {
            Calendar mc = Calendar.getInstance(); mc.clear(); mc.set(year, m, 1)
            Calendar grid = (Calendar) mc.clone()
            grid.add(Calendar.DAY_OF_MONTH, -(grid.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
            List weeks = []
            Calendar cur = (Calendar) grid.clone()
            for (int w = 0; w < 6; w++) {
                List week = []
                for (int d = 0; d < 7; d++) {
                    boolean inMonth = cur.get(Calendar.MONTH) == m
                    String k = key.format(cur.time)
                    week << [day: cur.get(Calendar.DAY_OF_MONTH), iso: k, inMonth: inMonth,
                             today: cur.time == today && inMonth,
                             hasEvents: inMonth && eventDays.contains(k)]
                    cur.add(Calendar.DAY_OF_MONTH, 1)
                }
                weeks << week
            }
            months << [monthNum: m + 1, label: new SimpleDateFormat('MMMM').format(mc.time), weeks: weeks]
        }
        [year: year, months: months, prevYear: year - 1, nextYear: year + 1]
    }

    /** Events in [from, to) grouped by yyyy-MM-dd of startsAt, ordered by time. */
    private Map<String, List> bucketEvents(Date from, Date to) {
        SimpleDateFormat key = new SimpleDateFormat('yyyy-MM-dd')
        SimpleDateFormat tf  = new SimpleDateFormat('h:mm a')
        Map<String, List> byDay = [:].withDefault { [] }
        Event.createCriteria().list {
            ge('startsAt', from); lt('startsAt', to); order('startsAt', 'asc')
        }.each { ev ->
            byDay[key.format(ev.startsAt)] << [
                id: ev.id, title: ev.title, color: ev.color ?: 'rose',
                allDay: ev.allDay, time: ev.allDay ? null : tf.format(ev.startsAt),
                location: ev.location, description: ev.description]
        }
        byDay
    }

    /** Blackout (closure) days in [from, to) keyed by yyyy-MM-dd. */
    private Map<String, Map> bucketBlackouts(Date from, Date to) {
        SimpleDateFormat key = new SimpleDateFormat('yyyy-MM-dd')
        Map<String, Map> byDay = [:]
        BlackoutDate.findAllByBlackoutDateBetween(from, to).each { b ->
            byDay[key.format(b.blackoutDate)] = [id: b.id, reason: b.reason]
        }
        byDay
    }

    /** Enrollment activity in [from, to) keyed by yyyy-MM-dd (staff-only layer). */
    private Map<String, List> bucketEnrollments(Date from, Date to) {
        SimpleDateFormat key = new SimpleDateFormat('yyyy-MM-dd')
        Map<String, List> byDay = [:].withDefault { [] }
        CourseEnrollment.findAllByEnrolledAtBetween(from, to).each { e ->
            byDay[key.format(e.enrolledAt)] << [student: (e.user?.name ?: e.user?.username), course: e.course?.shortTitle]
        }
        byDay
    }

    private boolean isStaff() {
        def roles = (springSecurityService.currentUser as User)?.authorities*.authority ?: []
        return ('ROLE_ADMIN' in roles) || ('ROLE_TEACHER' in roles)
    }

    private void zeroTime(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0)
    }
}
