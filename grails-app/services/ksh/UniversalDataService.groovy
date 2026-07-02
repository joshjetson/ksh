package ksh

import grails.gorm.transactions.Transactional
import grails.databinding.DataBinder
import grails.databinding.SimpleMapDataBindingSource
import groovy.sql.Sql
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import javax.persistence.PersistenceException
import javax.sql.DataSource
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * The agnostic data engine behind UDA. Knows nothing about any specific domain —
 * every method takes a domain Class and operates generically. The
 * UniversalController (authenticated portal) and PublicController (anonymous
 * landing) both delegate here. See docs/uda.md.
 */
@Transactional
class UniversalDataService {

    DataBinder grailsWebDataBinder
    DataSource dataSource   // injected by Grails — needed for the raw-SQL FTS path

    // The BCrypt encoder bean. Encoding happens HERE, at the service chokepoint,
    // because domain beforeInsert hooks rely on injected services that are NOT
    // wired into instances created via newInstance()/new — a hook would silently
    // no-op on programmatic creation and store raw passwords.
    @Autowired
    PasswordEncoder passwordEncoder

    // HTML time inputs send "HH:mm" format
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    /** BCrypt-encode a raw password; pass through values that are already hashed
     *  (idempotent — safe whether the caller pre-encoded or not). */
    private String encodeIfRaw(String pw) {
        (pw && (pw.startsWith('$2a$') || pw.startsWith('$2b$') || pw.startsWith('$2y$'))) ? pw : passwordEncoder.encode(pw)
    }

    // ====================================================================
    // USER-SPECIFIC METHODS FOR SPRING SECURITY INTEGRATION
    // ====================================================================

    /**
     * Create a new user and assign roles. ROLE_USER (learner) is always assigned;
     * additional roles from the list are added when they exist.
     */
    def createUserWithRoles(Map params, List<String> roleAuthorities) {
        try {
            // Encode the password explicitly (the domain hook can't be relied on for
            // programmatically-created instances — see passwordEncoder note above).
            if (params.password) {
                params = new LinkedHashMap(params)
                params.password = encodeIfRaw(params.password.toString())
            }
            def user = save(User, params)
            if (!user) return null

            assignRoles(user as User, roleAuthorities)
            return user
        } catch (Exception e) {
            println "ERROR: Error creating user with roles: ${e.message}"
            return null
        }
    }

    /**
     * Set a user's password (BCrypt-encoded). No-op for a blank password, so an
     * "edit user" form can leave the field empty to keep the current password.
     */
    boolean updatePassword(User user, String rawPassword) {
        try {
            if (!user || !rawPassword?.trim()) return false
            user.password = encodeIfRaw(rawPassword.trim())
            user.save(failOnError: true)
            return true
        } catch (Exception e) {
            println "ERROR: Error updating password for ${user?.username}: ${e.message}"
            return false
        }
    }

    /**
     * Replace all roles on a user. The BASE_ROLE is always re-assigned by assignRoles.
     */
    def replaceUserRoles(User user, List<String> roleAuthorities) {
        try {
            if (!user) return false
            // flush:true on delete is REQUIRED — UserRole's composite-key uniqueness
            // validator (userRole.exists) counts existing rows, so without flushing
            // the old rows the re-created ROLE_USER would fail validation silently
            // and the user would lose their base role on any role change.
            UserRole.findAllByUser(user).each { it.delete(flush: true) }
            assignRoles(user, roleAuthorities)
            return true
        } catch (Exception e) {
            println "ERROR: Error replacing roles for user ${user?.username}: ${e.message}"
            return false
        }
    }

    /**
     * Delete user and all associated roles
     */
    def deleteUser(User user) {
        try {
            if (user) {
                UserRole.findAllByUser(user).each { it.delete(failOnError: true) }
                user.delete(failOnError: true)
                return true
            }
        } catch (Exception e) {
            println "ERROR: Error deleting user ${user?.username}: ${e.message}"
            return false
        }
        return false
    }

    // Base role every authenticated user gets (KSH: learners are the base user).
    private static final String BASE_ROLE = 'ROLE_USER'

    /**
     * Assign roles to a user — generic: the BASE_ROLE is always granted, plus any
     * requested authorities that exist. (No hardcoded per-app role list.)
     */
    private void assignRoles(User user, List<String> roleAuthorities) {
        Set<String> wanted = ([BASE_ROLE] + (roleAuthorities ?: [])) as Set
        wanted.each { String authority ->
            Role role = Role.findByAuthority(authority)
            if (role) UserRole.create(user, role)
        }
    }

    // ====================================================================
    // CORE CRUD OPERATIONS - COMPLETELY AGNOSTIC
    // ====================================================================

    /**
     * Get instance by ID - works with any domain class
     */
    def getById(Class domainClass, Long id) {
        try {
            return domainClass.get(id)
        } catch (Exception e) {
            println "ERROR: " + ("Error retrieving ${domainClass.simpleName} with ID ${id}: ${e.message}")
            return null
        }
    }

    // Default pagination cap — prevents unbounded queries from returning massive result sets.
    // Override per-request by passing max/offset params from the frontend.
    private static final int DEFAULT_MAX = 100

    /**
     * List all instances - works with any domain class
     * @param domainClass - the domain to list
     * @param queryParams - optional map with 'max' and 'offset' for pagination
     */
    List list(Class domainClass, Map queryParams = [:]) {
        try {
            int max = Math.min((queryParams.max ?: DEFAULT_MAX) as int, DEFAULT_MAX)
            int offset = (queryParams.offset ?: 0) as int
            return domainClass.list(max: max, offset: offset)
        } catch (Exception e) {
            println "ERROR: " + ("Error listing ${domainClass.simpleName}: ${e.message}")
            return []
        }
    }

    /**
     * Count all instances - works with any domain class
     */
    Integer count(Class domainClass) {
        try {
            return domainClass.count()
        } catch (Exception e) {
            println "ERROR: " + ("Error counting ${domainClass.simpleName}: ${e.message}")
            return 0
        }
    }

    /**
     * Find by field OR get by ID - tries dynamic finder first, then get() as fallback
     * @param domainClass - the domain to search
     * @param field - the field to search by (e.g., "shortTitle", "email")
     * @param value - the value to search for (could be the field value OR an ID)
     * @return The found instance or null
     */
    def findByOrGet(Class domainClass, String field, def value) {
        try {
            if (!domainClass || !value) {
                return null
            }

            // Capitalize first letter for dynamic finder method name
            def methodName = "findBy${field.capitalize()}"

            // Try dynamic finder first
            def result = domainClass."${methodName}"(value)
            if (result) {
                return result
            }

            // Fallback to get by ID
            if (value.toString().isNumber()) {
                return domainClass.get(value.toString().toLong())
            }

            return null
        } catch (Exception e) {
            println "ERROR: " + ("Error in findByOrGet for ${domainClass?.simpleName}: ${e.message}")
            return null
        }
    }

    /**
     * Build the per-criterion closure that drives both `filter` and `filterCount`.
     *
     * Supports:
     *   - Direct fields:        `field=value`
     *   - Nested ID lookups:    `assoc.id=value`        (Hibernate uses FK column, no join)
     *   - Nested non-ID paths:  `assoc.field=value`     (creates alias + INNER JOIN)
     *
     * Special values: `today` / `week` (date range), `yyyy-MM-dd` (whole calendar
     * day), `true` / `false` (boolean), `null` (IS NULL), `in:a|b|c` (membership),
     * numeric (Long), else string equality.
     */
    private Closure buildCriteriaBlock(String criteria) {
        def today    = new Date().clearTime()
        def tomorrow = today + 1
        def weekAgo  = today - 7

        Closure block = {
            Map<String, String> aliases = [:]
            criteria.split(',').each { criterion ->
                def parts = criterion.trim().split('=')
                if (parts.length != 2) return
                def field = parts[0].trim()
                if (!field.contains('.')) return
                def segs = field.split('\\.')
                if (segs.length == 2 && segs[1] != 'id' && !aliases.containsKey(segs[0])) {
                    String alias = "_${segs[0]}"
                    aliases[segs[0]] = alias
                    createAlias(segs[0], alias)
                }
            }

            criteria.split(',').each { criterion ->
                def parts = criterion.trim().split('=')
                if (parts.length != 2) return
                def field = parts[0].trim()
                def value = parts[1].trim()

                def segs = field.split('\\.')
                if (segs.length == 2 && segs[1] != 'id' && aliases[segs[0]]) {
                    field = "${aliases[segs[0]]}.${segs[1]}"
                }

                if      (value == 'today') { ge(field, today);   lt(field, tomorrow) }
                else if (value == 'week')  { ge(field, weekAgo); lt(field, tomorrow) }
                else if (value == 'true')  { eq(field, true) }
                else if (value == 'false') { eq(field, false) }
                else if (value == 'null')  { isNull(field) }
                // field=in:a|b|c → membership test. An empty set uses a sentinel that
                // matches nothing, so a scoped read with no allowed IDs sees no rows.
                else if (value.startsWith('in:')) {
                    def items = value.substring(3).split('\\|').findAll { it }
                                     .collect { it.isLong() ? it.toLong() : it }
                    inList(field, items ?: [-1L])
                }
                else if (value ==~ /\d{4}-\d{2}-\d{2}/) {
                    // A calendar day (yyyy-MM-dd) → match the whole day on a date/timestamp field.
                    def d = new java.text.SimpleDateFormat('yyyy-MM-dd').parse(value)
                    ge(field, d); lt(field, d + 1)
                }
                else if (value.isNumber()) { eq(field, value.toLong()) }
                else                       { eq(field, value) }
            }
        }
        block.resolveStrategy = Closure.DELEGATE_FIRST
        return block
    }

    /**
     * Generic filter for any domain class with criteria support
     * @param domainClass - the domain to filter
     * @param criteria - comma-separated field=value pairs (e.g., "present=true,status=active")
     * @return List of matching domain instances
     */
    List filter(Class domainClass, String criteria, Map queryParams = [:]) {
        try {
            if (!domainClass) {
                println "ERROR: " + ("Filter called with null domainClass")
                return []
            }
            if (!criteria?.trim()) {
                return list(domainClass, queryParams)
            }
            int max    = Math.min((queryParams.max ?: DEFAULT_MAX) as int, DEFAULT_MAX)
            int offset = (queryParams.offset ?: 0) as int
            return domainClass.createCriteria().list(max: max, offset: offset, buildCriteriaBlock(criteria))
        } catch (Exception e) {
            println "ERROR: Error filtering ${domainClass?.simpleName}: ${e.message}"
            return []
        }
    }

    /**
     * Rows where `dateField` equals max(`dateField`) matching the criteria —
     * "show the most recent snapshot." Backs the `latest:` data instruction.
     */
    List filterLatest(Class domainClass, String dateField, String criteria, Map queryParams = [:]) {
        if (!domainClass || !dateField) return []
        try {
            def latestValue = _maxDateForCriteria(domainClass, dateField, criteria)
            if (latestValue == null) return []

            int max    = Math.min((queryParams.max ?: DEFAULT_MAX) as int, DEFAULT_MAX)
            int offset = (queryParams.offset ?: 0) as int

            Closure criteriaBlock = criteria?.trim() ? buildCriteriaBlock(criteria) : null
            return domainClass.createCriteria().list(max: max, offset: offset) {
                eq(dateField, latestValue)
                if (criteriaBlock) {
                    criteriaBlock.delegate = delegate
                    criteriaBlock.resolveStrategy = Closure.DELEGATE_FIRST
                    criteriaBlock()
                }
            }
        } catch (Exception e) {
            println "[filterLatest] ERROR for ${domainClass?.simpleName}.${dateField}: ${e.message}"
            return []
        }
    }

    /**
     * Count companion for `filterLatest`. Backs the `latestCount:` data instruction.
     */
    int filterLatestCount(Class domainClass, String dateField, String criteria) {
        if (!domainClass || !dateField) return 0
        try {
            def latestValue = _maxDateForCriteria(domainClass, dateField, criteria)
            if (latestValue == null) return 0

            Closure criteriaBlock = criteria?.trim() ? buildCriteriaBlock(criteria) : null
            return domainClass.createCriteria().get {
                projections { rowCount() }
                eq(dateField, latestValue)
                if (criteriaBlock) {
                    criteriaBlock.delegate = delegate
                    criteriaBlock.resolveStrategy = Closure.DELEGATE_FIRST
                    criteriaBlock()
                }
            } as int
        } catch (Exception e) {
            println "[filterLatestCount] ERROR for ${domainClass?.simpleName}.${dateField}: ${e.message}"
            return 0
        }
    }

    /**
     * Generic filter COUNT for any domain class with criteria support
     * More efficient than filter().size() as it uses SQL COUNT
     */
    int filterCount(Class domainClass, String criteria) {
        try {
            if (!domainClass) {
                println "ERROR: filterCount called with null domainClass"
                return 0
            }
            if (!criteria?.trim()) {
                return count(domainClass)
            }
            return domainClass.createCriteria().count(buildCriteriaBlock(criteria))
        } catch (Exception e) {
            println "ERROR: Error filterCount ${domainClass?.simpleName}: ${e.message}"
            return 0
        }
    }

    /**
     * Distinct values of a single field on a domain. Backs the `distinct:`
     * data instruction. Excludes NULLs, sorts ascending.
     */
    List distinctValues(Class domainClass, String fieldName) {
        if (!domainClass || !fieldName) return []
        try {
            return domainClass.createCriteria().list {
                projections {
                    distinct(fieldName)
                }
                isNotNull(fieldName)
                order(fieldName, 'asc')
            }
        } catch (Exception e) {
            println "ERROR: distinctValues for ${domainClass?.simpleName}.${fieldName}: ${e.message}"
            return []
        }
    }

    /**
     * Generic search across any domain class
     * @param domainClass - the domain to search
     * @param fields - comma-separated field names to search (e.g., "name" or "firstName,lastName")
     * @param searchTerm - the search term
     * @return List of matching domain instances, or all if no search term
     */
    List search(Class domainClass, String fields, String searchTerm, Map queryParams = [:]) {
        try {
            if (!domainClass) {
                println "ERROR: " + ("Search called with null domainClass")
                return []
            }

            if (!searchTerm?.trim()) {
                return list(domainClass, queryParams)
            }

            int max = Math.min((queryParams.max ?: DEFAULT_MAX) as int, DEFAULT_MAX)
            int offset = (queryParams.offset ?: 0) as int
            def fieldList = fields.split(',')*.trim()
            def escaped = escapeLikeWildcards(searchTerm)

            return domainClass.createCriteria().list(max: max, offset: offset) {
                or {
                    fieldList.each { field ->
                        ilike(field, "%${escaped}%")
                    }
                }
            }
        } catch (Exception e) {
            println "ERROR: " + ("Error searching ${domainClass?.simpleName}: ${e.message}")
            return []
        }
    }

    /**
     * Postgres full-text search via a domain's TSVECTOR column (see the FTS
     * migration). Returns domain instances ordered by ts_rank desc.
     * Backs the `fts:Domain:column:paramName` data instruction.
     *
     * SECURITY: `ftsColumn` is interpolated (Postgres can't parameterize column
     * names); validated against `\w+` as defense in depth. The term is always a
     * bound `?` parameter.
     */
    List ftsSearch(Class domainClass, String ftsColumn, String term, Map queryParams = [:]) {
        if (!domainClass) return []
        if (!ftsColumn || !(ftsColumn ==~ /\w+/)) {
            println "ERROR: ftsSearch invalid ftsColumn: ${ftsColumn}"
            return []
        }
        if (!term?.trim()) {
            return list(domainClass, queryParams)
        }

        String tsQuery = term.trim()
                .replaceAll(/[^\w\s]/, ' ')
                .split(/\s+/)
                .findAll { it }
                .collect { "${it}:*" }
                .join(' & ')
        if (!tsQuery) return []

        int max    = Math.min((queryParams.max ?: DEFAULT_MAX) as int, DEFAULT_MAX)
        int offset = (queryParams.offset ?: 0) as int

        String table = tableNameFor(domainClass)
        Sql sql = new Sql(dataSource)
        try {
            String idQuery = """
                SELECT id
                FROM ${table}
                WHERE ${ftsColumn} @@ to_tsquery('simple', ?)
                ORDER BY ts_rank(${ftsColumn}, to_tsquery('simple', ?)) DESC
                LIMIT ? OFFSET ?
            """
            List<Long> ids = sql.rows(idQuery, [tsQuery, tsQuery, max, offset])
                                .collect { it.id as Long }
            if (!ids) return []
            Map byId = domainClass.findAllByIdInList(ids).collectEntries { [(it.id): it] }
            return ids.collect { byId[it] }.findAll { it != null }
        } catch (Exception e) {
            println "ERROR: ftsSearch ${domainClass?.simpleName}.${ftsColumn}: ${e.message}"
            return []
        } finally {
            sql.close()
        }
    }

    /**
     * CamelCase domain → snake_case table name. `User` maps to `app_user` (the security
     * plugin reserves `user`); User IS FTS-searched (people search), so this exception
     * must be honoured here, not just assumed-never-hit.
     */
    private String tableNameFor(Class domainClass) {
        if (domainClass.simpleName == 'User') return 'app_user'
        domainClass.simpleName.replaceAll(/([a-z])([A-Z])/, '$1_$2').toLowerCase()
    }

    /**
     * Shared helper for filterLatest + filterLatestCount: max value of `dateField`
     * for rows matching `criteria`, via one indexed projection query.
     */
    private def _maxDateForCriteria(Class domainClass, String dateField, String criteria) {
        Closure criteriaBlock = criteria?.trim() ? buildCriteriaBlock(criteria) : null
        return domainClass.createCriteria().get {
            projections { max(dateField) }
            if (criteriaBlock) {
                criteriaBlock.delegate = delegate
                criteriaBlock.resolveStrategy = Closure.DELEGATE_FIRST
                criteriaBlock()
            }
        }
    }

    /**
     * Check if a record exists matching the given criteria - returns true/false.
     * Reuses the same criteria format as filter (comma-separated field=value pairs).
     *
     * This is the generic "does this thing exist?" check. Use it anywhere you need
     * a boolean answer about a record's existence instead of querying in the view layer.
     *
     * Examples from the frontend (via data instructions):
     *   data[enrolled]=exists:CourseEnrollment:user.id=3,course.id=7     -> true/false
     *   data[reviewed]=exists:Review:user.id=3,course.id=7              -> true/false
     *   data[hasBadge]=exists:UserBadge:user.id=3,badge.id=1            -> true/false
     *
     * @param domainClass - the domain to check
     * @param criteria - comma-separated field=value pairs (e.g. "user.id=3,course.id=7")
     * @return true if at least one matching record exists, false otherwise
     */
    boolean exists(Class domainClass, String criteria) {
        try {
            if (!domainClass || !criteria?.trim()) {
                return false
            }
            return filterCount(domainClass, criteria) > 0
        } catch (Exception e) {
            println "ERROR: Error checking exists for ${domainClass?.simpleName}: ${e.message}"
            return false
        }
    }

    /**
     * Bulk upsert by natural key. The single primitive every batch write path
     * delegates to. Returns [inserted: N, updated: M].
     */
    Map upsertBatch(Class domainClass, List<Map> records, List<String> naturalKeyFields) {
        int inserted = 0
        int updated  = 0
        String finder = naturalKeyFields ? "findBy" + naturalKeyFields.collect { it.capitalize() }.join("And") : null

        records.each { Map r ->
            // Bind to a transient instance FIRST so natural-key values are typed (a Date,
            // not the raw "yyyy-MM-dd" string) — otherwise the finder won't match and a
            // unique-constrained key (e.g. BlackoutDate) would throw on a duplicate.
            def instance = domainClass.newInstance()
            updateProperties(instance, r)

            def existing = null
            if (finder) {
                def values = naturalKeyFields.collect { instance."${it}" }
                if (values.any { it == null || (it instanceof CharSequence && !it.toString().trim()) }) {
                    println "WARNING: upsertBatch skipped record with null/empty natural key ${naturalKeyFields}: ${values}"
                    return
                }
                existing = domainClass.invokeMethod(finder, values as Object[])
            }

            if (existing) {
                updateProperties(existing, r)
                existing.save(failOnError: true)
                updated++
            } else {
                instance.save(failOnError: true)
                inserted++
            }
        }

        [inserted: inserted, updated: updated]
    }

    /**
     * Save new instance - completely agnostic
     * @param ownershipOverride - optional map of field name -> user ID to force after binding.
     *   Used to prevent ownership spoofing on create (e.g. forged creator.id in hidden fields).
     *   Uses User.load() to get a Hibernate proxy — no DB hit, no session conflict.
     */
    def save(Class domainClass, Map params, Map ownershipOverride = null) {
        try {
            def instance = domainClass.newInstance()
            if (instance) {
                updateProperties(instance, params)
                if (ownershipOverride) {
                    ownershipOverride.each { field, userId ->
                        instance."${field}" = User.load(userId)
                    }
                }
                instance.save(failOnError: true)
                return instance
            }
        } catch (PersistenceException e) {
            println "ERROR: " + ("Persistence error saving ${domainClass.simpleName}: ${e.message}")
            return null
        } catch (Exception e) {
            println "ERROR: " + ("Error saving ${domainClass.simpleName}: ${e.message}")
            return null
        }
        return null
    }

    /**
     * Update existing instance - completely agnostic
     */
    def update(Class domainClass, Long id, Map params) {
        try {
            def instance = getById(domainClass, id)
            if (instance) {
                updateProperties(instance, params)
                instance.save(failOnError: true)
                return instance
            } else {
                println "WARNING: " + ("${domainClass.simpleName} with ID ${id} not found for update")
            }
        } catch (PersistenceException e) {
            println "ERROR: " + ("Persistence error updating ${domainClass.simpleName} with ID ${id}: ${e.message}")
            return null
        } catch (Exception e) {
            println "ERROR: " + ("Error updating ${domainClass.simpleName} with ID ${id}: ${e.message}")
            return null
        }
        return null
    }

    /**
     * Delete instance by ID - completely agnostic
     */
    def deleteById(Class domainClass, Long id) {
        try {
            def instance = getById(domainClass, id)
            if (instance) {
                instance.delete(failOnError: true)
                return true
            } else {
                println "WARNING: " + ("${domainClass.simpleName} with ID ${id} not found for deletion")
                return false
            }
        } catch (Exception e) {
            println "ERROR: " + ("Error deleting ${domainClass.simpleName} with ID ${id}: ${e.message}")
            return false
        }
    }

    // ====================================================================
    // PRIVATE HELPER METHODS
    // ====================================================================

    /**
     * Bind properties from params to instance, excluding system fields
     */
    private void updateProperties(instance, Map params) {
        def excludedProperties = ['id', 'version', 'dateCreated', 'lastUpdated']

        // Preprocess params to convert LocalTime strings before binding
        Map processedParams = preprocessLocalTimeFields(instance, params)

        def bindingSource = new SimpleMapDataBindingSource(processedParams)
        grailsWebDataBinder.bind(instance, bindingSource, null, null, excludedProperties)
    }

    /**
     * Convert LocalTime string values ("HH:mm") to LocalTime objects
     * Inspects the domain class to find LocalTime fields and converts matching params
     */
    private Map preprocessLocalTimeFields(instance, Map params) {
        Map processed = new HashMap(params)

        // Find all LocalTime fields in the domain class
        instance.class.declaredFields.findAll {
            it.type == LocalTime && !it.synthetic
        }.each { field ->
            def value = params[field.name]
            if (value instanceof String && value.trim()) {
                processed[field.name] = parseLocalTime(value.trim())
            }
        }

        return processed
    }

    /**
     * Escape SQL LIKE wildcards in user input so they are treated as literals.
     * Prevents users from crafting expensive queries with % or _ patterns.
     */
    private String escapeLikeWildcards(String input) {
        if (!input) return input
        return input.replace('%', '\\%').replace('_', '\\_')
    }

    /**
     * Parse a time string to LocalTime
     * Handles "HH:mm" format from HTML time inputs
     */
    private LocalTime parseLocalTime(String timeStr) {
        if (!timeStr) return null

        try {
            // HTML time inputs send "HH:mm" format
            if (timeStr.length() == 5 && timeStr.contains(':')) {
                return LocalTime.parse(timeStr, TIME_FORMATTER)
            }
            // Fall back to ISO format (HH:mm:ss)
            return LocalTime.parse(timeStr)
        } catch (DateTimeParseException e) {
            println "WARNING: Failed to parse LocalTime from '${timeStr}': ${e.message}"
            return null
        }
    }
}
