package ksh

import grails.plugin.springsecurity.SpringSecurityService

class LoginController {

    SpringSecurityService springSecurityService

    def index() {
        if (springSecurityService.isLoggedIn()) {
            redirect uri: "/"
        } else {
            render view: "auth", model: brandingModel()
        }
    }

    def auth() {
        if (springSecurityService.isLoggedIn()) {
            redirect uri: "/"
        } else {
            render view: "auth", model: brandingModel()
        }
    }

    private Map brandingModel() {
        [config: AppConfig.first() ?: new AppConfig()]
    }
}
