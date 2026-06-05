// File upload limits
def maxFileSize = 500 * 1024 * 1024 // 500MB
def maxRequestSize = 500 * 1024 * 1024 // 500MB
grails.controllers.upload.maxFileSize = maxFileSize
grails.controllers.upload.maxRequestSize = maxRequestSize

grails.plugin.databasemigration.updateOnStart = true
grails.plugin.databasemigration.updateOnStartFileNames = ['changelog.groovy']

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.userLookup.userDomainClassName = 'ksh.User'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'ksh.UserRole'
grails.plugin.springsecurity.authority.className = 'ksh.Role'
grails.plugin.springsecurity.successHandler.defaultTargetUrl = '/'
grails.plugin.springsecurity.successHandler.alwaysUseDefault = true
grails.plugin.springsecurity.auth.loginFormUrl = '/login/auth'
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = '/login/auth?login_error=1'
grails.plugin.springsecurity.logout.afterLogoutUrl = '/'
grails.plugin.springsecurity.logout.postOnly = false

// Remember-me: a persistent login cookie so users stay authenticated even after the
// session cookie (JSESSIONID) is dropped — e.g. closing the browser. This is the
// session-based equivalent of a long-lived token expiry. alwaysRemember = true issues
// the cookie on every successful login, so no checkbox is needed in the login form.
// The key signs the token; keep it stable and secret (changing it invalidates all
// outstanding remember-me cookies). The default hash-based service ties the token to
// the user's password, so a password change also invalidates it.
grails.plugin.springsecurity.rememberMe.alwaysRemember = true
grails.plugin.springsecurity.rememberMe.tokenValiditySeconds = 31536000 // 1 year
grails.plugin.springsecurity.rememberMe.key = 'ksh-r3m3mb3r-m3-9f3c1a-do-not-change'

grails.plugin.springsecurity.controllerAnnotations.staticRules = [
        [pattern: '/error',          access: ['permitAll']],
        [pattern: '/shutdown',       access: ['permitAll']],
        [pattern: '/assets/**',      access: ['permitAll']],
        [pattern: '/**/js/**',       access: ['permitAll']],
        [pattern: '/**/css/**',      access: ['permitAll']],
        [pattern: '/**/images/**',   access: ['permitAll']],
        [pattern: '/**/favicon.ico', access: ['permitAll']],
        [pattern: '/login/**',       access: ['permitAll']],
        [pattern: '/logout/**',      access: ['permitAll']],
        [pattern: '/branding/**',    access: ['permitAll']],
        [pattern: '/public/**',      access: ['permitAll']],
        [pattern: '/course/image/**', access: ['permitAll']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/assets/**',      filters: 'none'],
        [pattern: '/**/js/**',       filters: 'none'],
        [pattern: '/**/css/**',      filters: 'none'],
        [pattern: '/**/images/**',   filters: 'none'],
        [pattern: '/**/favicon.ico', filters: 'none'],
        [pattern: '/**',             filters: 'JOINED_FILTERS']
]

grails.plugin.springsecurity.securityConfigType = 'InterceptUrlMap'
grails.plugin.springsecurity.interceptUrlMap = [
        [pattern: '/login/**',       access: ['permitAll']],
        [pattern: '/logout/**',      access: ['permitAll']],
        [pattern: '/assets/**',      access: ['permitAll']],
        [pattern: '/**/js/**',       access: ['permitAll']],
        [pattern: '/**/css/**',      access: ['permitAll']],
        [pattern: '/**/images/**',   access: ['permitAll']],
        [pattern: '/**/favicon.ico', access: ['permitAll']],
        [pattern: '/branding/**',    access: ['permitAll']],
        [pattern: '/public/**',      access: ['permitAll']],
        [pattern: '/course/image/**', access: ['permitAll']],
        [pattern: '/**',             access: ['ROLE_USER', 'ROLE_ADMIN']]
]
