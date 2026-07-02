package ksh

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.web.multipart.MultipartHttpServletRequest
import javax.servlet.AsyncContext
import javax.servlet.AsyncListener
import javax.servlet.AsyncEvent

/**
 * The one controller for the authenticated portal (UDA). Session auth, role-gated.
 * Read via declarative data instructions (showView), write via generic CRUD
 * (save/update/delete/saveBatch). New features = whitelist entries + a GSP partial,
 * almost never new controller code. See docs/uda.md.
 */
@Secured(['ROLE_USER', 'ROLE_ADMIN'])
class UniversalController {

    UniversalDataService universalDataService
    ScormService scormService
    DashboardService dashboardService
    ScheduleService scheduleService
    MessageService messageService
    CommunityService communityService
    RewardService rewardService
    GrailsApplication grailsApplication
    SpringSecurityService springSecurityService

    // Cached instruction handlers map (lazy initialized)
    private Map<String, Closure> _instructionHandlers

    // ── THE CONTRACT ── (see docs/uda.md). A new feature adds an entry here + a GSP
    // partial — almost never new controller code. A domain/field/method/key not
    // listed is DENIED.
    //
    // Domains readable via data instructions (list/get/filter/exists/fts/...).
    private static final Set<String> ALLOWED_DOMAINS = [
        'Course', 'CourseEnrollment', 'Badge', 'UserBadge', 'Review', 'User', 'AppConfig',
        'DiscountCode', 'BlackoutDate', 'EnrollmentChangeRequest', 'CourseReward',
        'Announcement',   // dashboard feed — all authed read
        'Event',          // calendar — all authed read
        'FileResource',   // files library — all authed read; binary via asset
        'MediaPost',      // media feed — all authed read; binary via asset
        'UserSetting',    // self-only (OWNERSHIP + READ_SCOPE → user) — discoverable toggle
        'UserBlock',      // self-only (OWNERSHIP + READ_SCOPE → owner) — personal block list
        'CommunityPost',  // wall reads go through CommunityService (visibility rules);
                          // generic read is MANAGER_READ-only for the moderator edit form
        'PostPin',        // self-only reactions (OWNERSHIP → user)
        'PostCheer',
        'Classroom',           // cohort names — all authed read
        'ClassroomStaff',      // teacher↔classroom assignment — teacher reads own (READ_SCOPE)
        'Term',                // school terms — all authed read; admin CRUD
        'ClassroomMembership', // learner reads own memberships (READ_SCOPE); teachers all
        'Attendance',          // learner reads own rows (READ_SCOPE); teachers all
        'Grade',               // learner reads own rows (READ_SCOPE); teachers all
    ] as Set

    // Non-owned back-office domains only managers (admin/teacher) may READ via instructions.
    private static final Set<String> MANAGER_READ_DOMAINS = [
        // Wall reads go through CommunityService (visibility policy: public/staff/self
        // can't be a declarative scope). Manager-read here is only for the moderator
        // edit form (get:CommunityPost); students never read it generically.
        'CommunityPost',
    ] as Set

    private static final Set<String> ALLOWED_SERVICE_METHODS = [
        'getCmiData', 'adminStats', 'studentStats',
        'monthView', 'dayView', 'yearView',   // ScheduleService — calendar drill-in
        'channelList',    // MessageService — channels the user may see
        'channelView',    // MessageService — one channel + its messages (access-checked)
        'channelMembers', // MessageService — channel member strip (moderators/admins)
        'memberActions',  // MessageService — one member's moderation actions (moderators)
        'channelPeople',  // MessageService — discoverable people in a channel (to PM)
        'personActions',  // MessageService — one person's PM/block actions
        'dmList',         // MessageService — the current user's direct-message threads
        'startDm',        // MessageService — find-or-create a DM (write; access-checked)
        'searchMessages', // MessageService — FTS over the user's visible messages
        'searchPeople',   // MessageService — FTS over people the user may PM
        'generalWall',    // CommunityService — the public (staff too, for managers) weekly wall
        'myWall',         // CommunityService — the user's own posts + pinned posts
        'searchPosts',    // CommunityService — moderator FTS across all posts (managers only)
        'postCard',       // CommunityService — one card, re-fetched after a cheer/pin/hide
    ] as Set

    // Config values exposed via the `config:` instruction — non-sensitive only.
    private static final Set<String> ALLOWED_CONFIG_KEYS = [
    ] as Set

    // Role-aware CRUD whitelist — which domains can be created/updated/deleted and by whom.
    // A value may be either a flat List<String> (same roles for create/update/delete) or a
    // Map<operation, List<String>> for per-operation control (e.g. students may create an
    // enrollment but only staff may update/delete it — so a student can't set paymentStatus).
    private static final Map<String, Object> ALLOWED_CRUD_DOMAINS = [
        'Course'          : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'CourseEnrollment': [create: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
                             // update/delete are admin-only: enrollment status/payment is
                             // back-office, and admins bypass the row-ownership check so they
                             // can manage any student's enrollment.
                             update: ['ROLE_ADMIN'],
                             delete: ['ROLE_ADMIN']],
        'Review'          : ['ROLE_USER', 'ROLE_ADMIN'],
        'Badge'           : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'UserBadge'       : ['ROLE_ADMIN'],
        'DiscountCode'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'BlackoutDate'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'CourseReward'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'EnrollmentChangeRequest': [create: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
                                    // admin approves/denies (admins bypass row-ownership)
                                    update: ['ROLE_ADMIN'],
                                    delete: ['ROLE_ADMIN']],
        'AppConfig'       : ['ROLE_ADMIN'],
        'Announcement'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'Event'           : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'FileResource'    : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'MediaPost'       : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        // Self-service privacy — any signed-in user manages their OWN row (owner-scoped).
        'UserSetting'     : ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
        'UserBlock'       : ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
        // Messaging: admin manages channels; anyone may post a message (the Message
        // validator blocks posting into staff channels), and delete is owner-or-admin.
        'Channel'         : ['ROLE_ADMIN'],
        'Message'         : [create: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
                             delete: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN']],
        // Channel moderation (mute/ban) — moderators (community leaders) + admins.
        'ChannelMembership': ['ROLE_MODERATOR', 'ROLE_ADMIN'],
        // Community wall: any signed-in user posts; moderators/staff moderate (edit/delete).
        'CommunityPost'   : [create: ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
                             update: ['ROLE_TEACHER', 'ROLE_ADMIN', 'ROLE_MODERATOR'],
                             delete: ['ROLE_TEACHER', 'ROLE_ADMIN', 'ROLE_MODERATOR']],
        // Wall reactions — any signed-in user cheers/pins on their own behalf (owner-scoped).
        'PostPin'         : ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
        'PostCheer'       : ['ROLE_USER', 'ROLE_TEACHER', 'ROLE_ADMIN'],
        // Live classrooms: admin shapes the school; teachers run rosters/attendance/grades.
        'Classroom'           : ['ROLE_ADMIN'],
        'ClassroomStaff'      : ['ROLE_ADMIN'],
        'Term'                : ['ROLE_ADMIN'],
        'ClassroomMembership' : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'Attendance'          : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'Grade'               : ['ROLE_TEACHER', 'ROLE_ADMIN'],
    ]

    // Row-level WRITE ownership — maps domain name to the field that references the owning
    // user. On update/delete, the current user must match this field (unless ROLE_ADMIN).
    // On create, this field is force-set to the current user to prevent spoofing.
    //
    // NOTE (deliberate divergence from VOG): in KSH this map governs WRITES ONLY. Reads of
    // these domains stay world-readable — students browse everyone's Courses and Reviews.
    // Read scoping is the separate opt-in axis READ_SCOPE_FIELDS below. A domain whose rows
    // should be private on read AND owner-written goes in BOTH maps.
    private static final Map<String, String> OWNERSHIP_FIELDS = [
        'Course'          : 'creator',
        'CourseEnrollment': 'user',
        'Review'          : 'user',
        'EnrollmentChangeRequest': 'requestedBy',
        'UserSetting'     : 'user',    // force-set + owner-checked writes
        'UserBlock'       : 'owner',
        'Message'         : 'author',  // force-set author on create; delete is author-or-admin
        'PostPin'         : 'user',    // a user pins to their own wall only
        'PostCheer'       : 'user',    // a user's own cheer reaction
    ]

    // Read-scoped rows — domainName -> owner field. A non-manager sees only rows where this
    // field is them (the scope is AND-ed into every read instruction); managers see all.
    // Unlike OWNERSHIP_FIELDS this does NOT force-set the field on create or owner-check
    // writes — pair with OWNERSHIP_FIELDS when both behaviours are wanted.
    private static final Map<String, String> READ_SCOPE_FIELDS = [
        'UserSetting'         : 'user',    // a user reads only their own settings row
        'UserBlock'           : 'owner',   // a user reads only their own block list
        // A learner sees their OWN classroom records; teachers/admin see all. Writes
        // stay teacher/admin-only via ALLOWED_CRUD_DOMAINS — a learner can read but
        // never write their attendance or grades.
        'ClassroomStaff'      : 'staff',   // a teacher sees only their own assignments
        'ClassroomMembership' : 'user',
        'Attendance'          : 'user',
        'Grade'               : 'user',
    ]

    // Author attribution — domainName -> field force-set to the creator on create.
    // Like OWNERSHIP_FIELDS' create behaviour, but WITHOUT read-scoping or owner-only
    // writes: the content is shared (e.g. an announcement everyone reads) yet stamped
    // with who posted it. Anti-spoof: a client can't forge the author.
    private static final Map<String, String> AUTHOR_FIELDS = [
        'Announcement': 'author',   // stamped with who posted; content stays shared
        'MediaPost'   : 'author',
        'CommunityPost': 'author',
        'Grade'       : 'gradedBy',   // stamp the teacher who entered it
    ]

    // Authenticated binary fields — domainName -> [byte[] fields] streamable to logged-in
    // users via /universal/asset (ownership-aware). Public binaries (course photos on the
    // anonymous catalog, login branding) keep their dedicated permitAll controllers.
    private static final Map<String, List<String>> AUTHED_BINARY_FIELDS = [
        'FileResource': ['file'],
        'MediaPost'   : ['image'],
    ]

    // SSE clients — open Servlet AsyncContexts (non-blocking, no thread held per client).
    // We stream text/event-stream by hand because a Grails action that *returns* a
    // Spring SseEmitter never reaches Spring's async return-value handler (Grails renders
    // it as an empty response). broadcastEvent() fans an event out to every open context.
    private static final List<AsyncContext> sseContexts = Collections.synchronizedList([])

    /**
     * GET /
     * Index page - Dashboard
     */
    def index() {
        // Catch up time-based / threshold milestones (e.g. anniversaries) on portal load.
        rewardService.checkMilestones(springSecurityService.currentUser as User)
        [config: AppConfig.first() ?: new AppConfig()]
    }

    // ==========================================================
    // GENERIC REST API ENDPOINTS
    // ==========================================================

    // The raw list/show/count REST actions MUST apply the same read scoping as the
    // data-instruction path (readScope/canReadDomain/ownsOrManager) — otherwise they are
    // an unscoped back door to owner-private data. The UI doesn't use these, but they
    // are reachable, so they are gated identically. Managers bypass.
    def list() {
        executeQuery(true) { domainClass ->
            if (!canReadDomain(params.domainName)) return []
            universalDataService.filter(domainClass, readScope(params.domainName) ?: '', paginationParams())
        }
    }

    /**
     * GET /universal/{domainName}/{id}
     * Show specific instance
     */
    def show() {
        String domainName = params.domainName
        Long id = params.long('id')
        Class domainClass = getAllowedDomainClass(domainName)

        if (!domainClass || !id || !canReadDomain(domainName)) {
            render status: 404, text: "Domain class '${domainName}' not found or invalid ID"
            return
        }

        try {
            def instance = universalDataService.getById(domainClass, id)

            if (!instance || !ownsOrManager(domainName, instance)) {
                render status: 404, text: "${domainName} with ID ${id} not found"
                return
            }

            if (isJsonRequest()) {
                render instance as JSON
                return
            }

            [instance: instance, domainName: domainName]
        } catch (Exception e) {
            println "ERROR: Error showing ${domainName} ${id}: ${e.message}"
            render status: 500, text: "Error retrieving ${domainName}"
        }
    }

    /**
     * POST /universal/{domainName}
     * Create new instance
     */
    def save() {
        executeCrud('create') { domainClass, id ->
            // Force-set server-controlled user fields on create (anti-spoof): the owner
            // (ownership-scoped domains) and/or the author (attribution-only domains).
            // The form may send creator.id or user.id — we overwrite it after binding.
            Map override = [:]
            Long uid = (springSecurityService.currentUser as User)?.id
            String ownerField  = OWNERSHIP_FIELDS[params.domainName]
            String authorField = AUTHOR_FIELDS[params.domainName]
            if (ownerField)  override[ownerField]  = uid
            if (authorField) override[authorField] = uid
            def instance = universalDataService.save(domainClass, extractParams(), override ?: null)
            // SCORM hook: a Course saved with a package zip gets extracted + manifest-parsed.
            if (instance instanceof Course && instance.scorm) {
                scormService.extractAndParseScorm(instance)
            }
            instance
        }
    }

    /**
     * PUT /universal/{domainName}/{id}
     * Update existing instance
     */
    def update() {
        executeCrud('update') { domainClass, id ->
            def instance = universalDataService.update(domainClass, id, extractParams())
            // SCORM hook: re-uploading a package on edit wipes and re-extracts it.
            if (instance instanceof Course && instance.scorm) {
                scormService.extractAndParseScorm(instance)
            }
            instance
        }
    }

    /**
     * Generic batch create/upsert: POST a JSON `records` array + `naturalKey` (comma-
     * separated) for one `domainName`. Gated by the same create whitelist as save().
     * Used e.g. to block a multi-day calendar selection in one request
     * (domainName=BlackoutDate, naturalKey=blackoutDate) — declarative + generic, not a
     * bespoke endpoint. Idempotent when a natural key is given (re-submitting an
     * existing row updates it instead of duplicating).
     */
    def saveBatch() {
        executeCrud('create') { domainClass, id ->
            List<Map> records = params.records ? (new groovy.json.JsonSlurper().parseText(params.records.toString()) as List) : []
            List<String> naturalKey = params.naturalKey ? params.naturalKey.toString().split(',').collect { it.trim() } : []
            // Force the owner field on each record if the domain is ownership-scoped (mirror save()).
            String ownerField = OWNERSHIP_FIELDS[params.domainName]
            if (ownerField) {
                Long uid = (springSecurityService.currentUser as User).id
                records.each { it[ownerField] = uid }
            }
            universalDataService.upsertBatch(domainClass, records, naturalKey)
        }
    }

    /**
     * DELETE /universal/{domainName}/{id}
     * Delete instance
     */
    def delete() {
        executeCrud('delete') { domainClass, id ->
            universalDataService.deleteById(domainClass, id)
        }
    }

    /**
     * GET /universal/{domainName}/count
     * Get count of instances
     */
    def count() {
        executeQuery(true) { domainClass ->
            if (!canReadDomain(params.domainName)) return [count: 0]
            [count: universalDataService.filterCount(domainClass, readScope(params.domainName) ?: '')]
        }
    }

    /**
     * GET /universal/asset?domainName=&id=&field= — stream a whitelisted byte[] to a
     * signed-in user. Gated by AUTHED_BINARY_FIELDS (+ MANAGER_READ_DOMAINS via
     * canReadDomain, + row scoping via ownsOrManager). Public binaries (anonymous course
     * catalog photos, login branding) keep their dedicated permitAll controllers.
     */
    def asset() {
        String domainName = params.domainName
        String field      = params.field
        if (!(field in AUTHED_BINARY_FIELDS[domainName]) || !canReadDomain(domainName)) {
            render status: 404; return
        }
        Class domainClass = getDomainClass(domainName)
        def obj = (domainClass && params.id) ? universalDataService.getById(domainClass, params.long('id')) : null
        if (!obj || !ownsOrManager(domainName, obj)) { render status: 404; return }
        byte[] bytes = obj."${field}"
        if (!bytes) { render status: 404; return }

        response.contentType = obj."${field}ContentType" ?: 'application/octet-stream'
        response.setHeader('Cache-Control', 'private, max-age=86400')
        response.setHeader('X-Content-Type-Options', 'nosniff')
        response.outputStream << bytes
        response.outputStream.flush()
    }

    // ==========================================================
    // DECLARATIVE VIEW RENDERING
    // ==========================================================

    /**
     * GET/POST /universal/showView
     * Single render entrypoint using declarative data instructions
     *
     * Frontend declares what data it needs via URL params:
     *   data[key]=instruction
     *
     * Examples:
     *   data[user]=currentUser
     *   data[items]=list:DomainClass
     *   data[item]=get:DomainClass:itemId
     *   data[count]=count:DomainClass
     *   data[result]=service:methodName
     */
    def showView() {
        boolean isHtmx = isHtmxRequest()
        Map model = buildModelFromRequest()

        // HTMX: fragments only (template param)
        if (isHtmx || params.template) {
            if (!params.template) {
                render status: 400, text: 'HTMX request requires template'
                return
            }
            if (!isValidTemplatePath(params.template.toString())) {
                render status: 400, text: 'Invalid template path'
                return
            }
            render template: params.template, model: model
            return
        }

        // Non-HTMX: full page only (view param)
        if (!params.view) {
            render status: 400, text: 'Non-HTMX request requires view'
            return
        }

        render view: params.view, model: model
    }

    // ==========================================================
    // MODEL CONSTRUCTION (DECLARATIVE)
    // ==========================================================

    private Map buildModelFromRequest() {
        Map model = [:]

        params.findAll { k, _ -> k.startsWith('data[') }.each { key, instruction ->
            def actualInstruction = (instruction instanceof String[] || instruction instanceof List) ? instruction[0] : instruction
            model[extractModelKey(key)] = resolveInstruction(actualInstruction)
        }

        return model
    }

    // ==========================================================
    // INSTRUCTION RESOLUTION
    // ==========================================================

    private Map<String, Closure> getInstructionHandlers() {
        if (_instructionHandlers == null) {
            // Reads are scope-aware (see readScope/canReadDomain): managers see all; on a
            // READ_SCOPE_FIELDS domain a non-manager only sees their own rows;
            // MANAGER_READ_DOMAINS are manager-only. list/count are expressed as
            // filter/filterCount so scoping lives in one path.
            _instructionHandlers = [
                'list'       : { parts ->
                    def dc = getAllowedDomainClass(parts[1]); if (!dc || !canReadDomain(parts[1])) return null
                    universalDataService.filter(dc, readScope(parts[1]) ?: '', paginationParams())
                },
                'get'        : { parts ->
                    def dc = getAllowedDomainClass(parts[1]); if (!dc || !canReadDomain(parts[1])) return null
                    def inst = universalDataService.getById(dc, params.long(parts[2] ?: 'id'))
                    (inst && ownsOrManager(parts[1], inst)) ? inst : null
                },
                'count'      : { parts ->
                    def dc = getAllowedDomainClass(parts[1]); if (!dc || !canReadDomain(parts[1])) return 0
                    universalDataService.filterCount(dc, readScope(parts[1]) ?: '')
                },
                'currentUser': { parts ->
                    // Returns the logged-in user. Password (bcrypt hash) stays on the
                    // domain object but is never rendered by any template.
                    // We intentionally do NOT call discard() here — evicting the user
                    // from the Hibernate session causes conflicts when other instructions
                    // in the same request (e.g. filter with currentUserId) re-load the user.
                    def u = springSecurityService.currentUser
                    return u ? User.get(u.id) : null
                },
                'service'    : { parts -> invokeServiceMethod(parts) },
                // config:some.config.key — a whitelisted config value. Gated by
                // ALLOWED_CONFIG_KEYS; non-sensitive only.
                'config'     : { parts ->
                    String key = parts[1]
                    if (!ALLOWED_CONFIG_KEYS.contains(key)) {
                        println "SECURITY: Blocked config read attempt: ${key}"
                        return null
                    }
                    grailsApplication.config.getProperty(key)
                },
                'literal'    : { parts ->
                    def value = parts[1]
                    if (value == 'true') return true
                    if (value == 'false') return false
                    return value
                },
                // search/fts/distinct have no criteria arg, so they can't be owner-scoped —
                // allow only where reads aren't owner-private.
                'search'     : { parts ->
                    def domain = getAllowedDomainClass(parts[1])
                    if (!domain || !canReadDomain(parts[1]) || readScope(parts[1]) != null) return null
                    universalDataService.search(domain, parts[2] ?: 'name', params[parts[3] ?: 'searchTerm'], paginationParams())
                },
                'fts'        : { parts ->
                    def domain = getAllowedDomainClass(parts[1])
                    if (!domain || !canReadDomain(parts[1]) || readScope(parts[1]) != null) return null
                    universalDataService.ftsSearch(domain, parts[2], params[parts[3] ?: 'q'], paginationParams())
                },
                'param'      : { parts ->
                    params[parts[1]] ?: ''
                },
                'filter'     : { parts ->
                    def domain = getAllowedDomainClass(parts[1]); if (!domain || !canReadDomain(parts[1])) return null
                    universalDataService.filter(domain, scopedCriteria(parts[1], parts[2] ?: ''), paginationParams())
                },
                // latest:Domain:dateField:criteria — rows where dateField = max(dateField)
                'latest'     : { parts ->
                    def domain = getAllowedDomainClass(parts[1]); if (!domain || !parts[2] || !canReadDomain(parts[1])) return null
                    universalDataService.filterLatest(domain, parts[2], scopedCriteria(parts[1], parts[3] ?: ''), paginationParams())
                },
                'latestCount': { parts ->
                    def domain = getAllowedDomainClass(parts[1]); if (!domain || !parts[2] || !canReadDomain(parts[1])) return 0
                    universalDataService.filterLatestCount(domain, parts[2], scopedCriteria(parts[1], parts[3] ?: ''))
                },
                'filterCount': { parts ->
                    def domain = getAllowedDomainClass(parts[1]); if (!domain || !canReadDomain(parts[1])) return 0
                    universalDataService.filterCount(domain, scopedCriteria(parts[1], parts[2] ?: ''))
                },
                'findByOrGet': { parts ->
                    def domain = getAllowedDomainClass(parts[1]); if (!domain || !canReadDomain(parts[1])) return null
                    def inst = universalDataService.findByOrGet(domain, parts[2] ?: 'id', params[parts[3] ?: (parts[2] ?: 'id')])
                    (inst && ownsOrManager(parts[1], inst)) ? inst : null
                },
                // exists — Generic boolean check: "does a record matching these criteria exist?"
                // Eliminates the need for scriptlet queries in GSP templates.
                //
                // Format: exists:Domain:field1=value1,field2=value2
                //
                // Values are resolved in this order:
                //   1. "currentUserId" -> logged-in user's ID
                //   2. Request param name   -> params[value] (e.g. "courseId" -> params.courseId)
                //   3. Literal              -> used as-is if no param matches
                //
                // Usage examples:
                //   data[enrolled]=exists:CourseEnrollment:user.id=currentUserId,course.id=courseId
                //   data[reviewed]=exists:Review:user.id=currentUserId,course.id=courseId
                //   data[hasBadge]=exists:UserBadge:user.id=currentUserId,badge.id=badgeId
                //   data[isCreator]=exists:Course:creator.id=currentUserId,id=courseId
                'exists'     : { parts ->
                    def domain = getAllowedDomainClass(parts[1]); if (!domain || !canReadDomain(parts[1])) return false
                    universalDataService.exists(domain, scopedCriteria(parts[1], parts[2] ?: ''))
                },
                'date'       : { parts ->
                    if (parts[1] == 'today') {
                        return new Date().clearTime()
                    }
                    return null
                },
                // distinct:Domain:field — unique non-null values, ascending. Owner-
                // private domains are not exposed to non-managers via distinct.
                'distinct'   : { parts ->
                    def dc = getAllowedDomainClass(parts[1])
                    if (!dc || !parts[2] || !canReadDomain(parts[1]) || readScope(parts[1]) != null) return []
                    universalDataService.distinctValues(dc, parts[2])
                }
            ]
        }
        return _instructionHandlers
    }

    private Object resolveInstruction(String instruction) {
        def parts = instruction.split(':')
        def handler = instructionHandlers[parts[0]]

        if (!handler) {
            throw new IllegalArgumentException("Unknown data instruction: ${instruction}")
        }

        handler(parts)
    }

    // ==========================================================
    // SAFE SERVICE METHOD INVOCATION
    // ==========================================================

    private Object invokeServiceMethod(String[] parts) {
        def service = universalDataService
        String methodName

        if (parts.length >= 3) {
            String serviceName = parts[1]
            methodName = parts[2]
            service = getServiceByName(serviceName)
            if (!service) {
                throw new IllegalArgumentException("Service not found: ${serviceName}")
            }
        } else {
            methodName = parts[1]
        }

        if (!ALLOWED_SERVICE_METHODS.contains(methodName)) {
            println "SECURITY: Blocked service method access attempt: ${methodName}"
            return null
        }

        def respondsWithMap = service.metaClass.respondsTo(service, methodName, Map)
        def respondsNoArgs = service.metaClass.respondsTo(service, methodName)

        if (!respondsWithMap && !respondsNoArgs) {
            throw new IllegalArgumentException("Service method not found: ${methodName}")
        }

        if (respondsWithMap) {
            return service."${methodName}"(params)
        } else {
            return service."${methodName}"()
        }
    }

    /**
     * Get service instance by name
     * Add new services here as the application grows
     */
    private Object getServiceByName(String serviceName) {
        switch (serviceName) {
            case 'universalDataService': return universalDataService
            case 'scormService': return scormService
            case 'dashboardService': return dashboardService
            case 'scheduleService': return scheduleService
            case 'messageService': return messageService
            case 'communityService': return communityService
            default: return null
        }
    }

    // ==========================================================
    // DECLARATIVE HELPERS
    // ==========================================================

    // ── Read authorization (scope-aware reads) ────────────────────────────
    // READ_SCOPE_FIELDS domains are private to their owner on every read instruction;
    // MANAGER_READ_DOMAINS are manager-only. Managers (admin/teacher) bypass read
    // scoping. NOTE: the WRITE-side ownership bypass in executeCrud stays ROLE_ADMIN-
    // only (KSH semantics: a teacher can't edit another teacher's course).

    private boolean isManager() {
        def roles = (springSecurityService.currentUser as User)?.authorities*.authority ?: []
        roles.contains('ROLE_ADMIN') || roles.contains('ROLE_TEACHER')
    }

    private Long currentUserId() {
        (springSecurityService.currentUser?.id ?: 0L) as Long
    }

    /** Can the current user read this domain at all via instructions? */
    private boolean canReadDomain(String domainName) {
        !(domainName in MANAGER_READ_DOMAINS) || isManager()
    }

    /** Ownership criterion to AND into a non-manager's reads of a scoped domain; null when
     *  no scoping applies (manager, or unscoped domain). */
    private String readScope(String domainName) {
        if (isManager()) return null
        String ownerField = READ_SCOPE_FIELDS[domainName]
        if (ownerField) return "${ownerField}.id=${currentUserId()}"
        null
    }

    /** Resolve a criteria string and AND in the ownership scope (if any). */
    private String scopedCriteria(String domainName, String rawCriteria) {
        String resolved = resolveCriteriaValues(rawCriteria)
        String scope = readScope(domainName)
        if (!scope) return resolved
        resolved?.trim() ? "${resolved},${scope}" : scope
    }

    /** True if the fetched instance is readable by the current user (manager, owns it, or
     *  the domain is read-unscoped). Closes the get/findByOrGet/show/asset path. */
    private boolean ownsOrManager(String domainName, instance) {
        if (isManager()) return true
        String ownerField = READ_SCOPE_FIELDS[domainName]
        if (ownerField) return instance."${ownerField}"?.id == currentUserId()
        return true
    }

    /**
     * Resolve dynamic values in criteria strings before passing to the service layer.
     * Criteria format: "field1=value1,field2=value2"
     *
     * Each value is resolved in order:
     *   - "currentUserId" → the logged-in user's ID (most common for ownership/enrollment checks)
     *   - A request param name → params[value] if that param exists in the request
     *   - Otherwise kept as a literal
     *
     * Example: "user.id=currentUserId,course.id=courseId"
     *   → with user #3 logged in and params.courseId=7
     *   → resolves to "user.id=3,course.id=7"
     */
    private String resolveCriteriaValues(String criteria) {
        if (!criteria?.trim()) return criteria

        criteria.split(',').collect { criterion ->
            def eqParts = criterion.trim().split('=', 2)
            if (eqParts.length == 2) {
                def field = eqParts[0].trim()
                def value = eqParts[1].trim()

                // Resolve special tokens and param references
                if (value == 'currentUserId') {
                    def user = springSecurityService.currentUser
                    value = user?.id?.toString() ?: '0'
                } else if (value == 'activeTerm') {
                    // The current term — resolved server-side so rosters/attendance/grades
                    // can declaratively filter by `term.id=activeTerm`. 0 = none active.
                    value = Term.findByActive(true)?.id?.toString() ?: '0'
                } else if (params[value] != null) {
                    value = params[value].toString()
                }

                return "${field}=${value}"
            }
            return criterion
        }.join(',')
    }

    /**
     * Extract params including multipart file data.
     * For each uploaded file named "foo", injects:
     *   foo       -> byte[] (file bytes)
     *   fooContentType -> String (MIME type)
     *   fooFileName    -> String (original filename)
     * This lets the generic DataBinder map files to domain fields declaratively.
     * When swapping to S3, change this method to upload and store a URL instead.
     */
    private Map extractParams() {
        Map myParams = new LinkedHashMap(params)
        if (request instanceof MultipartHttpServletRequest) {
            request.fileMap.each { name, file ->
                if (file && !file.empty) {
                    myParams[name] = file.bytes
                    myParams["${name}ContentType"] = file.contentType
                    myParams["${name}FileName"] = file.originalFilename
                }
            }
        }

        return myParams
    }

    private String extractModelKey(String rawKey) {
        rawKey.replace('data[', '').replace(']', '')
    }

    /**
     * Extract pagination params from request. Frontend can pass max/offset
     * to control result size. Capped at service-level DEFAULT_MAX (100).
     */
    private Map paginationParams() {
        Map p = [:]
        if (params.max) p.max = params.int('max')
        if (params.offset) p.offset = params.int('offset')
        return p
    }

    private Class getDomainClass(String domainName) {
        if (!domainName) return null
        grailsApplication.getDomainClass("ksh.${domainName}")?.clazz
    }

    private Class getAllowedDomainClass(String domainName) {
        if (!domainName || !ALLOWED_DOMAINS.contains(domainName)) {
            println "SECURITY: Blocked domain access attempt: ${domainName}"
            return null
        }
        getDomainClass(domainName)
    }

    private boolean isValidTemplatePath(String path) {
        if (!path) return false
        if (path.contains('..')) return false
        return path.matches(/^[a-zA-Z0-9\/_-]+$/)
    }

    private boolean isHtmxRequest() {
        return request.getHeader('HX-Request') == 'true'
    }

    private boolean isJsonRequest() {
        return params.format == 'json' ||
               request.getHeader('Accept')?.contains('application/json')
    }

    // ==========================================================
    // CRUD OPERATION HELPERS
    // ==========================================================

    private void executeCrud(String operationType, Closure operation) {
        String domainName = params.domainName
        Long id = params.long('id')

        // Check CRUD whitelist. The config is either a flat List (same roles for all ops)
        // or a Map keyed by operation (create/update/delete) for per-operation control.
        def crudConfig = ALLOWED_CRUD_DOMAINS[domainName]
        if (!crudConfig) {
            println "SECURITY: Blocked CRUD ${operationType} attempt on domain: ${domainName}"
            render status: 403, text: 'Not allowed'
            return
        }
        List<String> allowedRoles = (crudConfig instanceof Map) ? (crudConfig[operationType] as List) : (crudConfig as List)
        if (!allowedRoles) {
            println "SECURITY: Operation ${operationType} not permitted on domain: ${domainName}"
            render status: 403, text: 'Not allowed'
            return
        }

        def user = springSecurityService.currentUser as User
        def userRoles = user?.authorities?.collect { it.authority } ?: []
        boolean isAdmin = userRoles.contains('ROLE_ADMIN')

        if (!allowedRoles.any { userRoles.contains(it) }) {
            println "SECURITY: User ${user?.username} lacks role for CRUD ${operationType} on ${domainName}. Has: ${userRoles}, needs one of: ${allowedRoles}"
            render status: 403, text: 'Not allowed'
            return
        }

        Class domainClass = getDomainClass(domainName)

        if (!domainClass) {
            render status: 404, text: "Domain class '${domainName}' not found"
            return
        }
        if (operationType != 'create' && !id) {
            render status: 404, text: "Invalid ID for ${domainName}"
            return
        }

        // Row-level ownership check on update/delete (admins bypass — deliberately NOT
        // all managers: a teacher must not edit another teacher's course).
        String ownerField = OWNERSHIP_FIELDS[domainName]
        if (ownerField && operationType != 'create' && !isAdmin) {
            def instance = universalDataService.getById(domainClass, id)
            if (!instance) {
                render status: 404, text: "${domainName} not found"
                return
            }
            def owner = instance."${ownerField}"
            if (owner?.id != user.id) {
                println "SECURITY: User ${user.username} attempted ${operationType} on ${domainName} #${id} owned by user #${owner?.id}"
                render status: 403, text: 'Not allowed'
                return
            }
        }

        try {
            def result = operation(domainClass, id)

            // SSE broadcast on create/update
            if (operationType != 'delete' && result) {
                broadcastEvent("${domainName}-${operationType}", result.toString())
            }

            handleCrudResponse(result, domainName, id, operationType)

        } catch (Exception e) {
            println "ERROR: Error during ${operationType} of ${domainName}: ${e.message}"
            render status: 500, text: "Error during ${operationType}"
        }
    }

    private void handleCrudResponse(def result, String domainName, Long id, String operationType) {
        boolean success = operationType == 'delete' ? result : result != null

        if (success) {
            if (isHtmxRequest()) {
                if (renderHtmxTemplate()) return
                render status: operationType == 'create' ? 201 : 200, text: "Success"
            } else if (isJsonRequest()) {
                if (operationType == 'delete') {
                    render status: 204, text: ''
                } else {
                    render status: operationType == 'create' ? 201 : 200,
                           contentType: 'application/json', text: (result as JSON).toString()
                }
            } else {
                flash.success = "${domainName} ${operationType}d successfully"
                if (operationType == 'delete') {
                    redirect action: 'list', params: [domainName: domainName]
                } else {
                    redirect action: 'show', params: [domainName: domainName, id: result.id ?: id]
                }
            }
        } else {
            if (isHtmxRequest()) {
                render status: 400, text: "Failed to ${operationType} ${domainName}"
            } else if (isJsonRequest()) {
                render status: 400, text: "Failed to ${operationType} ${domainName}"
            } else {
                flash.error = "Failed to ${operationType} ${domainName}"
                redirect action: 'list', params: [domainName: domainName]
            }
        }
    }

    private boolean renderHtmxTemplate() {
        if (!params.template) return false

        String templatePath = params.template instanceof String[] ?
            params.template[0] : params.template.toString()
        if (!isValidTemplatePath(templatePath)) return false
        Map model = buildModelFromRequest()
        // Quiet mutations (e.g. a reaction tap that re-renders just its card) skip the
        // success toast — pass quiet=true in hx-vals.
        if (!params.boolean('quiet')) response.setHeader('HX-Trigger', 'showSuccessToast')
        render template: templatePath, model: model
        return true
    }

    private void executeQuery(boolean enforceWhitelist = false, Closure operation) {
        String domainName = params.domainName
        Class domainClass = enforceWhitelist ? getAllowedDomainClass(domainName) : getDomainClass(domainName)

        if (!domainClass) {
            render status: 404, text: "Domain class '${domainName}' not found"
            return
        }

        try {
            def result = operation(domainClass)
            render contentType: 'application/json', text: result as JSON
        } catch (Exception e) {
            println "ERROR: Error querying ${domainName}: ${e.message}"
            render status: 500, text: "Error querying ${domainName}"
        }
    }

    // ==========================================================
    // SSE (Server-Sent Events) — Generic Real-Time Updates
    // ==========================================================

    /**
     * GET /universal/sse — hand-rolled text/event-stream over a Servlet AsyncContext.
     * See the sseContexts note above for why this is not a Spring SseEmitter.
     */
    def sse() {
        AsyncContext ac = request.startAsync()
        ac.timeout = 0L   // never time out the stream
        response.contentType = 'text/event-stream'
        response.characterEncoding = 'UTF-8'
        response.setHeader('Cache-Control', 'no-cache')
        response.setHeader('Connection', 'keep-alive')
        response.setHeader('X-Accel-Buffering', 'no')   // disable proxy buffering (nginx/caddy)
        try {
            def w = response.writer
            w << "event: heartbeat\ndata: connected\n\n"
            w.flush()
        } catch (Exception ignored) {
            try { ac.complete() } catch (Exception e) {}
            return
        }
        ac.addListener([
            onComplete  : { AsyncEvent e -> sseContexts.remove(ac) },
            onTimeout   : { AsyncEvent e -> sseContexts.remove(ac); try { ac.complete() } catch (Exception ex) {} },
            onError     : { AsyncEvent e -> sseContexts.remove(ac) },
            onStartAsync: { AsyncEvent e -> },
        ] as AsyncListener)
        sseContexts.add(ac)
        // No return value — the response stays open and is streamed to via broadcastEvent.
    }

    /**
     * Broadcast a generic SSE event to all connected clients
     * @param eventName the SSE event type (e.g., "Course-create", "Message-create")
     * @param data the event data string
     */
    static void broadcastEvent(String eventName, String data) {
        synchronized (sseContexts) {
            List<AsyncContext> dead = []
            sseContexts.each { AsyncContext ac ->
                try {
                    def w = ac.response.writer
                    w << "event: ${eventName}\ndata: ${data}\n\n"
                    w.flush()
                } catch (Exception e) {
                    dead << ac
                }
            }
            dead.each { AsyncContext ac -> try { ac.complete() } catch (Exception e) {}; sseContexts.remove(ac) }
        }
    }
}
