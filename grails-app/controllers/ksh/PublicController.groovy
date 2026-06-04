package ksh

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured

/**
 * Anonymous, pre-auth surface: a marketing landing page with the public course
 * catalog, plus student self-registration. Sanctioned non-UDA controller
 * (docs/uda.md pillar 13) — it's an auth/onboarding flow with a different security
 * model, and reuses UniversalDataService for the actual user creation.
 */
@Secured(['permitAll'])
class PublicController {

    SpringSecurityService springSecurityService
    UniversalDataService universalDataService

    /** GET /public — marketing landing + public catalog. */
    def index() {
        if (springSecurityService.isLoggedIn()) {
            redirect uri: '/'
            return
        }
        [courses: Course.list(max: 12, sort: 'dateCreated', order: 'desc'), config: AppConfig.first()]
    }

    /** GET /public/register — the sign-up form. */
    def register() {
        if (springSecurityService.isLoggedIn()) {
            redirect uri: '/'
            return
        }
        [config: AppConfig.first()]
    }

    /** POST /public/register/doRegister — create a ROLE_USER account and log in. */
    def doRegister() {
        String username = params.username?.toString()?.trim()
        String password = params.password?.toString()
        String firstName = params.firstName?.toString()?.trim()
        String lastName = params.lastName?.toString()?.trim()
        def config = AppConfig.first()

        def fail = { String msg ->
            render view: 'register', model: [config: config, error: msg,
                                             username: username, firstName: firstName, lastName: lastName]
        }

        if (!username || !password || !firstName) {
            fail('Please fill in your name, email, and a password.'); return
        }
        if (password.length() < 8) {
            fail('Password must be at least 8 characters.'); return
        }
        if (User.findByUsername(username)) {
            fail('An account with that email already exists. Try logging in.'); return
        }

        Map userParams = [
            username : username,
            password : springSecurityService.encodePassword(password),
            email    : username,
            firstName: firstName,
            lastName : lastName,
            name     : [firstName, lastName].findAll().join(' '),
            roleType : 'learner'
        ]
        def user = universalDataService.createUserWithRoles(userParams, [])  // [] → ROLE_USER only
        if (!user) {
            fail('Sorry, we could not create your account. Please try again.'); return
        }

        springSecurityService.reauthenticate(user.username)
        redirect uri: '/'
    }
}
