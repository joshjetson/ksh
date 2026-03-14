package ksh

import grails.gorm.transactions.Transactional
import grails.databinding.DataBinder
import grails.databinding.SimpleMapDataBindingSource
import javax.persistence.PersistenceException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Transactional
class UniversalDataService {

    DataBinder grailsWebDataBinder

    // HTML time inputs send "HH:mm" format
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

    // ====================================================================
    // USER-SPECIFIC METHODS FOR SPRING SECURITY INTEGRATION
    // ====================================================================

    /**
     * Update user's role by removing all existing roles and assigning new role
     */
    def updateUserRole(User user, Role newRole) {
        try {
            if (user && newRole) {
                // Remove all existing user roles
                UserRole.findAllByUser(user).each { userRole ->
                    userRole.delete(failOnError: true)
                }
                // Assign new role
                UserRole.create(user, newRole)
                return true
            }
        } catch (Exception e) {
            println "ERROR: Error updating user role for user ${user?.username}: ${e.message}"
            return false
        }
        return false
    }

    /**
     * Create a new user with specified role
     */
    def createUserWithRole(Map params, Role role) {
        try {
            def user = save(User, params)
            if (user && role) {
                UserRole.create(user as User, role)
                return user
            }
        } catch (Exception e) {
            println "ERROR: " + ("Error creating user with role: ${e.message}")
            return null
        }
        return null
    }

    /**
     * Delete user and all associated roles
     */
    def deleteUser(User user) {
        try {
            if (user) {
                // Remove all user roles first
                UserRole.findAllByUser(user).each { userRole ->
                    userRole.delete(failOnError: true)
                }
                // Delete the user
                user.delete(failOnError: true)
                return true
            }
        } catch (Exception e) {
            println "ERROR: " + ("Error deleting user ${user?.username}: ${e.message}")
            return false
        }
        return false
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
     * @param field - the field to search by (e.g., "qrCode", "email")
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
     * Generic filter for any domain class with criteria support
     * @param domainClass - the domain to filter
     * @param criteria - comma-separated field=value pairs (e.g., "present=true,status=active")
     *                   Special values: "today" for date fields means current date
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

            int max = Math.min((queryParams.max ?: DEFAULT_MAX) as int, DEFAULT_MAX)
            int offset = (queryParams.offset ?: 0) as int
            def today = new Date().clearTime()
            def tomorrow = today + 1
            def weekAgo = today - 7

            return domainClass.createCriteria().list(max: max, offset: offset) {
                criteria.split(',').each { criterion ->
                    def parts = criterion.trim().split('=')
                    if (parts.length == 2) {
                        def field = parts[0].trim()
                        def value = parts[1].trim()

                        // Handle special values for dates
                        if (value == 'today') {
                            ge(field, today)
                            lt(field, tomorrow)
                        } else if (value == 'week') {
                            ge(field, weekAgo)
                            lt(field, tomorrow)
                        } else if (value == 'true') {
                            eq(field, true)
                        } else if (value == 'false') {
                            eq(field, false)
                        } else if (value.isNumber()) {
                            eq(field, value.toLong())
                        } else {
                            eq(field, value)
                        }
                    }
                }
            }
        } catch (Exception e) {
            println "ERROR: Error filtering ${domainClass?.simpleName}: ${e.message}"
            return []
        }
    }

    /**
     * Generic filter COUNT for any domain class with criteria support
     * More efficient than filter().size() as it uses SQL COUNT
     * @param domainClass - the domain to filter
     * @param criteria - comma-separated field=value pairs (e.g., "present=true,status=active")
     *                   Special values: "today" for date fields means current date
     * @return Count of matching domain instances
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

            def today = new Date().clearTime()
            def tomorrow = today + 1
            def weekAgo = today - 7

            return domainClass.createCriteria().count {
                criteria.split(',').each { criterion ->
                    def parts = criterion.trim().split('=')
                    if (parts.length == 2) {
                        def field = parts[0].trim()
                        def value = parts[1].trim()

                        // Handle special values for dates
                        if (value == 'today') {
                            ge(field, today)
                            lt(field, tomorrow)
                        } else if (value == 'week') {
                            ge(field, weekAgo)
                            lt(field, tomorrow)
                        } else if (value == 'true') {
                            eq(field, true)
                        } else if (value == 'false') {
                            eq(field, false)
                        } else if (value.isNumber()) {
                            eq(field, value.toLong())
                        } else {
                            eq(field, value)
                        }
                    }
                }
            }
        } catch (Exception e) {
            println "ERROR: Error filterCount ${domainClass?.simpleName}: ${e.message}"
            return 0
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

            return domainClass.createCriteria().list(max: max, offset: offset) {
                or {
                    fieldList.each { field ->
                        ilike(field, "%${searchTerm}%")
                    }
                }
            }
        } catch (Exception e) {
            println "ERROR: " + ("Error searching ${domainClass?.simpleName}: ${e.message}")
            return []
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
     *   data[hasPost]=exists:WallPost:author.id=3                       -> true/false
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
     * Save new instance - completely agnostic
     */
    def save(Class domainClass, Map params) {
        try {
            def instance = domainClass.newInstance()
            if (instance) {
                updateProperties(instance, params)
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