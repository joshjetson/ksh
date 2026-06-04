package ksh

import grails.gorm.transactions.ReadOnly
import grails.plugin.springsecurity.SpringSecurityService

import java.text.SimpleDateFormat

/**
 * Builds the admin calendar — a 6-week month grid with enrollment activity and
 * blackout (closure) days overlaid. Returns only scalars/lists (no lazy proxies)
 * so the GSP renders after the read-only transaction closes. Invoked via
 * `service:scheduleService:monthView` (whitelisted, staff only).
 */
class ScheduleService {

    SpringSecurityService springSecurityService

    @ReadOnly
    Map monthView(Map params) {
        User me = springSecurityService.currentUser as User
        if (!me || !isStaff(me)) {
            return null
        }

        Calendar now = Calendar.instance
        int year = (params?.year ?: now.get(Calendar.YEAR)) as int
        int month = (params?.month ?: (now.get(Calendar.MONTH) + 1)) as int   // 1-based

        Calendar first = Calendar.instance
        first.clear()
        first.set(year, month - 1, 1)

        // Grid starts on the Sunday on or before the 1st, runs 42 days (6 weeks).
        Calendar gridStart = (Calendar) first.clone()
        gridStart.add(Calendar.DAY_OF_MONTH, -(gridStart.get(Calendar.DAY_OF_WEEK) - 1))
        Calendar gridEnd = (Calendar) gridStart.clone()
        gridEnd.add(Calendar.DAY_OF_MONTH, 42)

        def enrollments = CourseEnrollment.findAllByEnrolledAtBetween(gridStart.time, gridEnd.time)
        def blackouts = BlackoutDate.findAllByBlackoutDateBetween(gridStart.time, gridEnd.time)

        SimpleDateFormat iso = new SimpleDateFormat('yyyy-MM-dd')
        Map<String, List> enrollByDay = [:].withDefault { [] }
        enrollments.each { e -> enrollByDay[iso.format(e.enrolledAt)] << e }
        Map<String, Map> blackoutByDay = [:]
        blackouts.each { b -> blackoutByDay[iso.format(b.blackoutDate)] = [id: b.id, reason: b.reason] }

        String todayIso = iso.format(new Date())

        List weeks = []
        Calendar cur = (Calendar) gridStart.clone()
        for (int w = 0; w < 6; w++) {
            List days = []
            for (int d = 0; d < 7; d++) {
                String key = iso.format(cur.time)
                List dayEnroll = enrollByDay[key]
                days << [
                    iso        : key,
                    day        : cur.get(Calendar.DAY_OF_MONTH),
                    inMonth    : (cur.get(Calendar.MONTH) == month - 1),
                    today      : (key == todayIso),
                    enrollCount: dayEnroll.size(),
                    enrollments: dayEnroll.collect { [student: (it.user?.name ?: it.user?.username), course: it.course?.shortTitle] },
                    blackout   : blackoutByDay[key]
                ]
                cur.add(Calendar.DAY_OF_MONTH, 1)
            }
            weeks << days
        }

        SimpleDateFormat monthFmt = new SimpleDateFormat('MMMM yyyy')
        Calendar prev = (Calendar) first.clone(); prev.add(Calendar.MONTH, -1)
        Calendar next = (Calendar) first.clone(); next.add(Calendar.MONTH, 1)

        return [
            monthLabel: monthFmt.format(first.time),
            year      : year,
            month     : month,
            weeks     : weeks,
            prev      : [year: prev.get(Calendar.YEAR), month: prev.get(Calendar.MONTH) + 1],
            next      : [year: next.get(Calendar.YEAR), month: next.get(Calendar.MONTH) + 1]
        ]
    }

    private boolean isStaff(User u) {
        def roles = u?.authorities*.authority ?: []
        return ('ROLE_ADMIN' in roles) || ('ROLE_TEACHER' in roles)
    }
}
