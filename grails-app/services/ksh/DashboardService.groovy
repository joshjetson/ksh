package ksh

import grails.gorm.transactions.ReadOnly
import grails.plugin.springsecurity.SpringSecurityService

import java.text.SimpleDateFormat

/**
 * Read-only metric aggregation for the dashboards. Each method returns a plain
 * Map of scalars/lists (no lazy GORM proxies) so the GSP can render it after the
 * transaction closes. Invoked via the UDA `service:` instruction
 * (`service:dashboardService:adminStats`) — whitelisted in
 * UniversalController.ALLOWED_SERVICE_METHODS.
 */
class DashboardService {

    SpringSecurityService springSecurityService

    /**
     * Back-office metrics. Staff only (ROLE_ADMIN / ROLE_TEACHER) — returns null otherwise,
     * so a non-staff caller simply gets no data.
     * @param params.period one of: week | month | year | all (default month)
     */
    @ReadOnly
    Map adminStats(Map params) {
        User me = springSecurityService.currentUser as User
        if (!me || !isStaff(me)) {
            return null
        }

        String period = (params?.period ?: 'month').toString()
        Map spec = periodSpec(period)
        Date from = spec.from as Date

        // --- Enrollments ---
        int enrollAll = CourseEnrollment.count()
        int enrollInPeriod = CourseEnrollment.createCriteria().count {
            if (from) ge('dateCreated', from)
        } as int
        int completed = CourseEnrollment.countByCompletedAtIsNotNull()
        int active = enrollAll - completed
        int completionRate = enrollAll > 0 ? Math.round(completed * 100.0d / enrollAll) as int : 0
        Number avgProgRaw = CourseEnrollment.createCriteria().get { projections { avg('progress') } } as Number
        int avgProgress = avgProgRaw ? Math.round(avgProgRaw.doubleValue()) as int : 0

        // --- Catalog & people ---
        int courseCount = Course.count()
        int studentCount = (CourseEnrollment.executeQuery('select count(distinct e.user.id) from CourseEnrollment e')[0] ?: 0) as int

        // --- Revenue, in K-credits (Course.costKCredits summed over enrollments) ---
        BigDecimal creditsAll = enrollmentCredits(null)
        BigDecimal creditsPeriod = enrollmentCredits(from)

        // --- Top courses by enrollment count (with credits earned) ---
        String topHql = """select c.shortTitle, count(e.id), coalesce(sum(c.costKCredits), 0)
                           from CourseEnrollment e join e.course c
                           ${from ? 'where e.dateCreated >= :from' : ''}
                           group by c.id, c.shortTitle order by count(e.id) desc"""
        def topRows = from ? CourseEnrollment.executeQuery(topHql, [from: from], [max: 6])
                           : CourseEnrollment.executeQuery(topHql, [max: 6])
        List topCourses = topRows.collect { [name: it[0], enrollments: (it[1] as int), credits: it[2]] }

        // --- Enrollment trend (oldest → newest buckets) ---
        List trend = buildTrend(spec)

        // --- Messages needing a reply ---
        int awaiting = Conversation.countByAwaitingReply(true)
        List awaitingList = Conversation.findAllByAwaitingReply(true, [sort: 'lastMessageAt', order: 'desc', max: 8]).collect {
            [id: it.id, student: (it.student?.name ?: it.student?.username)]
        }

        // --- Engagement ---
        int badgesAwarded = UserBadge.count()
        int reviewCount = Review.count()
        Number avgRatingRaw = Review.createCriteria().get { projections { avg('starRating') } } as Number
        BigDecimal avgRating = avgRatingRaw ? (Math.round(avgRatingRaw.doubleValue() * 10) / 10.0d) : 0

        return [
            period      : period,
            periodLabel : spec.label,
            enrollments : [all: enrollAll, inPeriod: enrollInPeriod, active: active,
                           completed: completed, completionRate: completionRate, avgProgress: avgProgress],
            catalog     : [courses: courseCount, students: studentCount],
            credits     : [all: creditsAll, inPeriod: creditsPeriod],
            topCourses  : topCourses,
            trend       : trend,
            messages    : [awaiting: awaiting, awaitingList: awaitingList],
            engagement  : [badges: badgesAwarded, reviews: reviewCount, avgRating: avgRating]
        ]
    }

    /**
     * A student's own snapshot for the overview dashboard.
     */
    @ReadOnly
    Map studentStats(Map params) {
        User me = springSecurityService.currentUser as User
        if (!me) {
            return null
        }
        List<CourseEnrollment> mine = CourseEnrollment.findAllByUser(me)
        int total = mine.size()
        int completed = mine.count { it.completedAt != null } as int
        int avgProgress = total ? Math.round(mine.sum { it.progress ?: 0 } / (double) total) as int : 0
        int badges = UserBadge.countByUser(me)
        Conversation convo = Conversation.findByStudent(me)
        int messageCount = convo ? Message.countByConversation(convo) : 0

        return [
            name        : (me.name ?: me.username),
            enrollments : [total: total, completed: completed, active: total - completed, avgProgress: avgProgress],
            badges      : badges,
            messages    : messageCount
        ]
    }

    // ==================== helpers ====================

    private boolean isStaff(User u) {
        def roles = u?.authorities*.authority ?: []
        return ('ROLE_ADMIN' in roles) || ('ROLE_TEACHER' in roles)
    }

    /** Sum of course cost (K-credits) over enrollments, optionally since `from`. */
    private BigDecimal enrollmentCredits(Date from) {
        String hql = """select coalesce(sum(c.costKCredits), 0) from CourseEnrollment e join e.course c
                        ${from ? 'where e.dateCreated >= :from' : ''}"""
        def result = from ? CourseEnrollment.executeQuery(hql, [from: from]) : CourseEnrollment.executeQuery(hql)
        return (result[0] ?: 0) as BigDecimal
    }

    /** period → [from, label, buckets, step, fmt] used for both window totals and the trend chart. */
    private Map periodSpec(String period) {
        switch (period) {
            case 'week':  return [from: daysAgo(7),   label: 'last 7 days',    buckets: 7,  step: 'day',     fmt: 'EEE']
            case 'year':  return [from: daysAgo(365), label: 'last 12 months', buckets: 12, step: 'month',   fmt: 'MMM']
            case 'all':   return [from: null,         label: 'all time',       buckets: 12, step: 'month',   fmt: 'MMM']
            case 'month':
            default:      return [from: daysAgo(30),  label: 'last 30 days',   buckets: 6,  step: 'fiveday', fmt: 'M/d']
        }
    }

    private Date daysAgo(int n) {
        (new Date() - n).clearTime()
    }

    /** Build oldest→newest [label, count] buckets of enrollment counts. */
    private List buildTrend(Map spec) {
        int n = spec.buckets as int
        String step = spec.step
        SimpleDateFormat sdf = new SimpleDateFormat(spec.fmt as String)
        List out = []
        for (int i = n - 1; i >= 0; i--) {
            Date start, end
            if (step == 'day') {
                start = (new Date() - i).clearTime()
                end = start + 1
            } else if (step == 'fiveday') {
                start = (new Date() - (i * 5 + 4)).clearTime()
                end = (new Date() - (i * 5)).clearTime() + 1
            } else { // month
                Calendar cal = Calendar.instance
                cal.time = new Date().clearTime()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -i)
                start = cal.time
                Calendar cal2 = (Calendar) cal.clone()
                cal2.add(Calendar.MONTH, 1)
                end = cal2.time
            }
            int cnt = CourseEnrollment.createCriteria().count {
                ge('dateCreated', start)
                lt('dateCreated', end)
            } as int
            out << [label: sdf.format(start), count: cnt]
        }
        return out
    }
}
