package ksh

import grails.plugin.springsecurity.annotation.Secured

/**
 * Serves admin-uploaded branding assets (currently the login-page background image).
 * Public — readable without authentication so it can render on the login page.
 */
@Secured(['permitAll'])
class BrandingController {

    /**
     * GET /branding/background
     * Streams the AppConfig.first().backgroundImage bytes.
     * Returns 404 if no image has been uploaded.
     */
    def background() {
        def cfg = AppConfig.first()
        if (!cfg?.backgroundImage) {
            render status: 404
            return
        }
        response.contentType = cfg.backgroundImageContentType ?: 'application/octet-stream'
        response.setHeader('Cache-Control', 'public, max-age=3600')
        response.outputStream << cfg.backgroundImage
        response.outputStream.flush()
    }
}
