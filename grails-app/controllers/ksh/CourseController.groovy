package ksh

import grails.plugin.springsecurity.annotation.Secured

/**
 * Serves uploaded lesson photos as bytes. Binary serving only — a sanctioned
 * non-UDA controller (docs/uda.md pillar 13), same shape as BadgeController /
 * BrandingController. permitAll because the public course catalog shows photos to
 * anonymous visitors. All course CRUD still goes through the universal endpoint.
 */
@Secured(['permitAll'])
class CourseController {

    def index() {
        redirect uri: '/'
    }

    /** GET /course/image/{id} — the lesson's uploaded photo. */
    def image() {
        Long id = params.long('id')
        Course course = id ? Course.get(id) : null
        if (!course?.image) {
            render status: 404
            return
        }
        response.contentType = course.imageContentType ?: 'application/octet-stream'
        response.setHeader('Cache-Control', 'public, max-age=86400')
        response.outputStream << course.image
        response.outputStream.flush()
    }
}
