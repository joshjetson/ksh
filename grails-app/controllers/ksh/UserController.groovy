package ksh

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@Secured(['ROLE_ADMIN'])
class UserController {

    SpringSecurityService springSecurityService
    UniversalDataService universalDataService
    def passwordEncoder
    def rememberMeServices
    def userDetailsService

    def index() {
        redirect uri: '/'
    }

    /**
     * POST /user/save
     * Create a new user account with encoded password and role assignment.
     */
    def save() {
        try {
            String rawPassword = params.password
            if (!rawPassword?.trim()) {
                render status: 400, text: 'Password is required'
                return
            }

            Map userParams = new LinkedHashMap(params)
            userParams.password = springSecurityService.encodePassword(rawPassword)

            if (userParams.firstName || userParams.lastName) {
                userParams.name = [userParams.firstName, userParams.lastName].findAll().join(' ')
            }

            def user = universalDataService.createUserWithRoles(userParams, params.list('roles'))
            if (!user) {
                render status: 400, text: 'Failed to create user'
                return
            }

            renderUserList()
        } catch (Exception e) {
            println "ERROR: Error creating user: ${e.message}"
            render status: 500, text: 'Error creating user'
        }
    }

    /**
     * POST /user/update/{id}
     * Update an existing user. Only re-encodes password if a new one is provided.
     */
    def update() {
        try {
            Long id = params.long('id')
            if (!id) {
                render status: 400, text: 'Invalid user ID'
                return
            }

            def user = User.get(id)
            if (!user) {
                render status: 404, text: 'User not found'
                return
            }

            Map userParams = new LinkedHashMap(params)

            if (userParams.password?.trim()) {
                userParams.password = springSecurityService.encodePassword(userParams.password as String)
            } else {
                userParams.remove('password')
            }

            if (userParams.firstName || userParams.lastName) {
                userParams.name = [userParams.firstName, userParams.lastName].findAll().join(' ')
            }

            def updated = universalDataService.update(User, id, userParams)
            if (!updated) {
                render status: 400, text: 'Failed to update user'
                return
            }

            universalDataService.replaceUserRoles(user, params.list('roles'))
            renderUserList()
        } catch (Exception e) {
            println "ERROR: Error updating user: ${e.message}"
            render status: 500, text: 'Error updating user'
        }
    }

    /**
     * POST /user/delete/{id}
     * Delete a user and all associated roles. Cannot delete yourself.
     */
    def delete() {
        try {
            Long id = params.long('id')
            if (!id) {
                render status: 400, text: 'Invalid user ID'
                return
            }

            def currentUser = springSecurityService.currentUser as User
            if (currentUser.id == id) {
                render status: 400, text: 'Cannot delete your own account'
                return
            }

            def user = User.get(id)
            if (!user) {
                render status: 404, text: 'User not found'
                return
            }

            universalDataService.deleteUser(user)
            renderUserList()
        } catch (Exception e) {
            println "ERROR: Error deleting user: ${e.message}"
            render status: 500, text: 'Error deleting user'
        }
    }

    /**
     * POST /user/updateProfile
     * Users update their own profile (firstName, lastName, title, avatar).
     * Only modifies the currently logged-in user.
     */
    @Secured(['ROLE_USER', 'ROLE_ADMIN'])
    def updateProfile() {
        try {
            def currentUser = springSecurityService.currentUser as User
            if (!currentUser) {
                render status: 401, text: 'Not authenticated'
                return
            }

            Map userParams = new LinkedHashMap(params)
            // Only allow profile fields — not password, username, roles, etc.
            Map safeParams = [:]
            ['firstName', 'lastName', 'title'].each { field ->
                if (userParams.containsKey(field)) safeParams[field] = userParams[field]
            }

            if (safeParams.firstName || safeParams.lastName) {
                safeParams.name = [safeParams.firstName ?: currentUser.firstName, safeParams.lastName ?: currentUser.lastName].findAll().join(' ')
            }

            // Handle avatar upload if present
            if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest) {
                def avatarFile = request.getFile('avatarFile')
                if (avatarFile && !avatarFile.empty) {
                    def ext = avatarFile.originalFilename?.contains('.') ?
                        avatarFile.originalFilename.substring(avatarFile.originalFilename.lastIndexOf('.')) : '.png'
                    def dir = new File("uploads/avatars")
                    dir.mkdirs()
                    def dest = new File(dir, "${currentUser.id}${ext}")
                    avatarFile.transferTo(dest)
                    safeParams.avatar = "/user/avatar/${currentUser.id}"
                }
            }

            // Handle avatar removal
            if (params.removeAvatar == 'true') {
                safeParams.avatar = null
            }

            // Handle preset avatar selection (when upload is disabled)
            if (params.presetAvatar) {
                String slug = params.presetAvatar.toString().trim()
                if (slug && AvatarController.presetSlugs.contains(slug)) {
                    safeParams.avatar = "/avatar/preset/${slug}"
                }
            }

            def updated = universalDataService.update(User, currentUser.id, safeParams)
            if (!updated) {
                render status: 400, text: 'Failed to update profile'
                return
            }

            if (isHtmxRequest()) {
                def config = AppConfig.first() ?: new AppConfig()
                def badges = UserBadge.findAllByUser(currentUser)
                def enrollmentCount = CourseEnrollment.countByUser(currentUser)
                response.setHeader('HX-Trigger', 'showSuccessToast')
                render template: "/universal/profile/view", model: [user: User.get(currentUser.id), badges: badges, enrollmentCount: enrollmentCount, config: config]
            } else {
                render status: 200, text: 'Profile updated'
            }
        } catch (Exception e) {
            println "ERROR: Error updating profile: ${e.message}"
            render status: 500, text: 'Error updating profile'
        }
    }

    /**
     * GET /user/avatar/{id}
     * Serve uploaded avatar image from disk.
     */
    @Secured(['ROLE_USER', 'ROLE_ADMIN'])
    def avatar() {
        Long id = params.long('id')
        if (!id) {
            render status: 404
            return
        }

        def dir = new File("uploads/avatars")
        def match = dir.listFiles()?.find { it.name.startsWith("${id}.") }
        if (!match) {
            render status: 404
            return
        }

        def ext = match.name.substring(match.name.lastIndexOf('.') + 1).toLowerCase()
        def mimeTypes = [png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg', gif: 'image/gif', webp: 'image/webp']
        response.contentType = mimeTypes[ext] ?: 'application/octet-stream'
        response.setHeader('Cache-Control', 'public, max-age=3600')
        match.withInputStream { response.outputStream << it }
    }

    /**
     * POST /user/changePassword
     * A user changes their OWN password. Verifies the current password, enforces a
     * minimum length, then re-issues the remember-me cookie so the user stays logged
     * in — the hash-based remember-me token is derived from the password, so without
     * this refresh the old cookie would silently stop working on the next browser
     * restart. Renders an inline result message (targets #password-msg via HTMX).
     */
    @Secured(['ROLE_USER', 'ROLE_ADMIN'])
    def changePassword() {
        try {
            def currentUser = springSecurityService.currentUser as User
            if (!currentUser) {
                render status: 401, text: 'Not authenticated'
                return
            }

            String currentPassword = params.currentPassword
            String newPassword = params.newPassword
            String confirmPassword = params.confirmPassword

            def fail = { String msg ->
                render(template: '/universal/profile/passwordResult', model: [ok: false, message: msg])
            }

            if (!currentPassword?.trim() || !newPassword?.trim() || !confirmPassword?.trim()) {
                fail('All fields are required.'); return
            }
            if (!passwordEncoder.matches(currentPassword, currentUser.password)) {
                fail('Your current password is incorrect.'); return
            }
            if (newPassword.length() < 8) {
                fail('New password must be at least 8 characters.'); return
            }
            if (newPassword != confirmPassword) {
                fail('New password and confirmation do not match.'); return
            }
            if (passwordEncoder.matches(newPassword, currentUser.password)) {
                fail('New password must be different from your current password.'); return
            }

            currentUser.password = springSecurityService.encodePassword(newPassword)
            if (!currentUser.save(flush: true)) {
                fail('Could not update password. Please try again.'); return
            }

            // Rebuild the authentication with the NEW password hash, then re-issue the
            // remember-me cookie. The existing Authentication's principal still holds
            // the old hash, so signing from it would produce a cookie that fails on the
            // next auto-login. Reloading fresh UserDetails fixes the signature and also
            // refreshes the live SecurityContext.
            try {
                def freshDetails = userDetailsService.loadUserByUsername(currentUser.username)
                def newAuth = new UsernamePasswordAuthenticationToken(freshDetails, freshDetails.password, freshDetails.authorities)
                SecurityContextHolder.context.authentication = newAuth
                rememberMeServices.loginSuccess(request, response, newAuth)
            } catch (Exception e) {
                println "WARN: Could not refresh remember-me cookie after password change: ${e.message}"
            }

            response.setHeader('HX-Trigger', 'showSuccessToast')
            render(template: '/universal/profile/passwordResult', model: [ok: true, message: 'Password updated successfully.'])
        } catch (Exception e) {
            println "ERROR: Error changing password: ${e.message}"
            render status: 500, text: 'Error changing password'
        }
    }

    // ==================== Private Helpers ====================

    private void renderUserList() {
        if (isHtmxRequest()) {
            Map model = [users: User.list(max: 100)]
            response.setHeader('HX-Trigger', 'showSuccessToast')
            render template: "/universal/admin/userList", model: model
        } else {
            render status: 200, text: 'OK'
        }
    }

    private boolean isHtmxRequest() {
        return request.getHeader('HX-Request') == 'true'
    }
}
