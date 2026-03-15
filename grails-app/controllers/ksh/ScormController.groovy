package ksh

import grails.converters.JSON
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured

@Secured(['ROLE_USER', 'ROLE_ADMIN'])
class ScormController {

    SpringSecurityService springSecurityService
    ScormService scormService

    private static final Map<String, String> MIME_TYPES = [
        'html': 'text/html',
        'htm' : 'text/html',
        'js'  : 'application/javascript',
        'css' : 'text/css',
        'json': 'application/json',
        'xml' : 'application/xml',
        'xsd' : 'application/xml',
        'dtd' : 'application/xml-dtd',
        'png' : 'image/png',
        'jpg' : 'image/jpeg',
        'jpeg': 'image/jpeg',
        'gif' : 'image/gif',
        'svg' : 'image/svg+xml',
        'webp': 'image/webp',
        'ico' : 'image/x-icon',
        'mp3' : 'audio/mpeg',
        'mp4' : 'video/mp4',
        'webm': 'video/webm',
        'ogg' : 'audio/ogg',
        'wav' : 'audio/wav',
        'woff': 'font/woff',
        'woff2': 'font/woff2',
        'ttf' : 'font/ttf',
        'eot' : 'application/vnd.ms-fontobject',
        'swf' : 'application/x-shockwave-flash',
        'pdf' : 'application/pdf',
        'txt' : 'text/plain'
    ]
    def index() {
        redirect uri: '/'
    }

    /**
     * GET /scorm/player/{id}
     * Full-page SCORM player
     */
    def player() {
        def course = Course.get(params.long('id'))
        if (!course?.scormLaunchUrl) {
            println "SCORM: Player requested for invalid/missing course ID ${params.id}"
            render status: 404, text: 'Course not found or has no SCORM content'
            return
        }

        def user = springSecurityService.currentUser as User
        def enrollment = CourseEnrollment.findByUserAndCourse(user, course)
        if (!enrollment) {
            println "SECURITY: User ${user?.username} attempted SCORM access to course ${course.id} without enrollment"
            render status: 403, text: 'Not enrolled in this course'
            return
        }

        def cmiData = scormService.getCmiData(user, course)

        [course: course, user: user, enrollment: enrollment, cmiData: cmiData]
    }

    /**
     * GET /scorm/content/{courseId}/**
     * Serve extracted SCORM files from disk
     */
    def content() {
        Long courseId = params.long('courseId')
        if (!courseId) {
            render status: 400, text: 'Invalid course ID'
            return
        }

        // Get the file path from the URL after /scorm/content/{courseId}/
        String requestPath = request.forwardURI
        String prefix = "/scorm/content/${courseId}/"
        int prefixIndex = requestPath.indexOf(prefix)
        if (prefixIndex < 0) {
            render status: 404, text: 'File not found'
            return
        }
        String filePath = requestPath.substring(prefixIndex + prefix.length())

        if (!filePath || filePath.contains('..')) {
            println "SECURITY: Path traversal attempt on SCORM content: ${filePath}"
            render status: 400, text: 'Invalid path'
            return
        }

        def baseDir = new File("uploads/scorm/${courseId}").canonicalPath
        def file = new File("uploads/scorm/${courseId}", filePath)
        if (!file.canonicalPath.startsWith(baseDir)) {
            println "SECURITY: Path traversal via canonical path on SCORM content: ${filePath}"
            render status: 400, text: 'Invalid path'
            return
        }
        if (!file.exists() || file.isDirectory()) {
            render status: 404, text: 'File not found'
            return
        }

        // Determine MIME type
        String ext = filePath.contains('.') ? filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase() : ''
        String mimeType = MIME_TYPES[ext] ?: 'application/octet-stream'

        // Cache SCORM assets aggressively — they never change once extracted.
        // ETag based on file size + last modified so browsers skip re-downloading.
        String etag = "\"${file.length()}-${file.lastModified()}\""
        if (request.getHeader('If-None-Match') == etag) {
            render status: 304
            return
        }

        response.contentType = mimeType
        response.setHeader('Cache-Control', 'public, max-age=86400')
        response.setHeader('ETag', etag)
        file.withInputStream { is ->
            response.outputStream << is
        }
    }

    /**
     * GET /api/scorm/{courseId}/cmi
     * Return CMI data as JSON
     */
    def getCmiData() {
        def course = Course.get(params.long('courseId'))
        def user = springSecurityService.currentUser as User
        if (!course || !user) {
            render status: 404, text: 'Not found'
            return
        }

        def data = scormService.getCmiData(user, course)
        render data as JSON
    }

    /**
     * POST /api/scorm/{courseId}/cmi
     * Save CMI data from the player
     */
    def saveCmiData() {
        def course = Course.get(params.long('courseId'))
        def user = springSecurityService.currentUser as User
        if (!course || !user) {
            render status: 404, text: 'Not found'
            return
        }

        Map cmiData = request.JSON as Map
        scormService.saveCmiData(user, course, cmiData)
        render status: 200, text: 'OK'
    }
}
