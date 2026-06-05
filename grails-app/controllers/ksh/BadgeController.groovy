package ksh

import grails.plugin.springsecurity.annotation.Secured

/**
 * Serves uploaded badge/collectible artwork as bytes. Binary serving only —
 * a sanctioned non-UDA controller (docs/uda.md pillar 13), same shape as
 * AvatarController / BrandingController. All badge CRUD still goes through the
 * universal endpoint.
 */
@Secured(['ROLE_USER', 'ROLE_ADMIN', 'ROLE_TEACHER'])
class BadgeController {

    def index() {
        redirect uri: '/'
    }

    /** GET /badge/image/{id} — the collectible's uploaded image. */
    def image() {
        Long id = params.long('id')
        Badge badge = id ? Badge.get(id) : null
        if (!badge?.image) {
            render status: 404
            return
        }
        response.contentType = badge.imageContentType ?: 'application/octet-stream'
        response.setHeader('Cache-Control', 'public, max-age=86400')
        response.outputStream << badge.image
        response.outputStream.flush()
    }
}
