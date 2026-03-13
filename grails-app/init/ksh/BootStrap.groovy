package ksh

class BootStrap {

    BootstrapService bootstrapService

    def init = { servletContext ->
        bootstrapService.createRoles()
        bootstrapService.createDevelopmentUsers()
        bootstrapService.createSampleCourses()
        bootstrapService.createSampleBadges()
    }

    def destroy = {
    }
}
