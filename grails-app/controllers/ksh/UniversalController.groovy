package ksh

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.http.MediaType
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.text.SimpleDateFormat

@Secured(['ROLE_USER', 'ROLE_ADMIN'])
class UniversalController {

    UniversalDataService universalDataService
    ScormService scormService
    GrailsApplication grailsApplication
    SpringSecurityService springSecurityService

    // Cached instruction handlers map (lazy initialized)
    private Map<String, Closure> _instructionHandlers

    // Whitelists — only these domains and service methods can be resolved via data instructions
    private static final Set<String> ALLOWED_DOMAINS = [
        'Course', 'CourseEnrollment', 'Badge', 'UserBadge', 'Review', 'WallPost'
    ] as Set

    private static final Set<String> ALLOWED_SERVICE_METHODS = [
        'getCmiData'
    ] as Set

    // Role-aware CRUD whitelist — which domains can be created/updated/deleted and by whom
    private static final Map<String, List<String>> ALLOWED_CRUD_DOMAINS = [
        'Course'          : ['ROLE_TEACHER', 'ROLE_ADMIN'],
        'CourseEnrollment': ['ROLE_USER', 'ROLE_ADMIN'],
        'Review'          : ['ROLE_USER', 'ROLE_ADMIN'],
        'WallPost'        : ['ROLE_USER', 'ROLE_ADMIN'],
        'Badge'           : ['ROLE_ADMIN'],
        'UserBadge'       : ['ROLE_ADMIN']
    ]

    // Row-level ownership — maps domain name to the field that references the owning user.
    // On update/delete, the current user must match this field (unless ROLE_ADMIN).
    // On create, this field is force-set to the current user to prevent spoofing.
    private static final Map<String, String> OWNERSHIP_FIELDS = [
        'Course'          : 'creator',
        'CourseEnrollment': 'user',
        'Review'          : 'user',
        'WallPost'        : 'user'
    ]


    // SSE Client Management
    private static final List<PrintWriter> sseClients = Collections.synchronizedList([])

    /**
     * GET /
     * Index page - Dashboard
     */
    def index() {
        [:]
    }

    // ==========================================================
    // GENERIC REST API ENDPOINTS
    // ==========================================================

    /**
     * GET /api/universal/{domainName}
     * REST API endpoint - returns JSON list of all instances
     */
    def list() {
        executeQuery { domainClass -> universalDataService.list(domainClass) }
    }

    /**
     * GET /universal/{domainName}/{id}
     * Show specific instance
     */
    def show() {
        String domainName = params.domainName
        Long id = params.long('id')
        Class domainClass = getDomainClass(domainName)

        if (!domainClass || !id) {
            render status: 404, text: "Domain class '${domainName}' not found or invalid ID"
            return
        }

        try {
            def instance = universalDataService.getById(domainClass, id)

            if (!instance) {
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
            def instance = universalDataService.save(domainClass, extractParams())
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
            if (instance instanceof Course && instance.scorm) {
                scormService.extractAndParseScorm(instance)
            }
            instance
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
        executeQuery { domainClass -> [count: universalDataService.count(domainClass)] }
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
            _instructionHandlers = [
                'list'       : { parts -> def dc = getAllowedDomainClass(parts[1]); dc ? universalDataService.list(dc, paginationParams()) : null },
                'get'        : { parts -> def dc = getAllowedDomainClass(parts[1]); dc ? universalDataService.getById(dc, params.long(parts[2] ?: 'id')) : null },
                'count'      : { parts -> def dc = getAllowedDomainClass(parts[1]); dc ? universalDataService.count(dc) : null },
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
                'literal'    : { parts ->
                    def value = parts[1]
                    if (value == 'true') return true
                    if (value == 'false') return false
                    return value
                },
                'search'     : { parts ->
                    def domain = getAllowedDomainClass(parts[1])
                    if (!domain) return null
                    def fields = parts[2] ?: 'name'
                    def searchParamName = parts[3] ?: 'searchTerm'
                    def searchTerm = params[searchParamName]
                    universalDataService.search(domain, fields, searchTerm, paginationParams())
                },
                'param'      : { parts ->
                    params[parts[1]] ?: ''
                },
                'filter'     : { parts ->
                    def domain = getAllowedDomainClass(parts[1])
                    if (!domain) return null
                    def criteria = resolveCriteriaValues(parts[2] ?: '')
                    universalDataService.filter(domain, criteria, paginationParams())
                },
                'filterCount': { parts ->
                    def domain = getAllowedDomainClass(parts[1])
                    if (!domain) return null
                    def criteria = resolveCriteriaValues(parts[2] ?: '')
                    universalDataService.filterCount(domain, criteria)
                },
                'findByOrGet': { parts ->
                    def domain = getAllowedDomainClass(parts[1])
                    if (!domain) return null
                    def field = parts[2] ?: 'id'
                    def paramName = parts[3] ?: field
                    def value = params[paramName]
                    universalDataService.findByOrGet(domain, field, value)
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
                    def domain = getAllowedDomainClass(parts[1])
                    if (!domain) return false
                    def rawCriteria = parts[2] ?: ''
                    def resolvedCriteria = resolveCriteriaValues(rawCriteria)
                    universalDataService.exists(domain, resolvedCriteria)
                },
                'date'       : { parts ->
                    if (parts[1] == 'today') {
                        return new Date().clearTime()
                    }
                    return null
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
            default: return null
        }
    }

    // ==========================================================
    // DECLARATIVE HELPERS
    // ==========================================================

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

        // Check CRUD whitelist
        def allowedRoles = ALLOWED_CRUD_DOMAINS[domainName]
        if (!allowedRoles) {
            println "SECURITY: Blocked CRUD ${operationType} attempt on domain: ${domainName}"
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

        // Row-level ownership check on update/delete (admins bypass)
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
        Map model = buildModelFromRequest()
        response.setHeader('HX-Trigger', 'showSuccessToast')
        render template: templatePath, model: model
        return true
    }

    private void executeQuery(Closure operation) {
        String domainName = params.domainName
        Class domainClass = getDomainClass(domainName)

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
    // SSE (Server-Sent Events) - Generic Real-Time Updates
    // ==========================================================

    /**
     * GET /universal/sse
     * SSE endpoint for real-time updates
     */
    def sse() {
        response.contentType = MediaType.TEXT_EVENT_STREAM_VALUE
        response.characterEncoding = "UTF-8"
        response.setHeader("Cache-Control", "no-cache")
        response.setHeader("Connection", "keep-alive")
        response.setHeader("Access-Control-Allow-Origin", "*")

        PrintWriter writer = response.writer
        sseClients.add(writer)

        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat('HH:mm:ss')
            String timestamp = timeFormat.format(new Date())
            writer.write("event: heartbeat\ndata: Connected: ${timestamp}\n\n")
            writer.flush()

            int heartbeatCount = 0
            while (!Thread.currentThread().isInterrupted() && heartbeatCount < 120) {
                try {
                    Thread.sleep(30000)
                    heartbeatCount++
                    timestamp = timeFormat.format(new Date())
                    writer.write("event: heartbeat\ndata: Heartbeat: ${timestamp}\n\n")
                    writer.flush()
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt()
                    break
                } catch (Exception e) {
                    break
                }
            }

        } catch (Exception e) {
            println "DEBUG: SSE client disconnected: ${e.message}"
        } finally {
            sseClients.remove(writer)
            try {
                writer.close()
            } catch (Exception ignored) {}
        }
    }

    /**
     * Broadcast a generic SSE event to all connected clients
     * @param eventName the SSE event type (e.g., "Student-create", "notification")
     * @param data the event data string
     */
    static void broadcastEvent(String eventName, String data) {
        synchronized(sseClients) {
            sseClients.removeAll { client ->
                try {
                    client.write("event: ${eventName}\ndata: ${data}\n\n")
                    client.flush()
                    return false
                } catch (Exception e) {
                    println "Removing dead SSE client: ${e.message}"
                    return true
                }
            }
        }
    }
}
