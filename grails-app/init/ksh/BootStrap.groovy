package ksh

class BootStrap {

    BootstrapService bootstrapService
    ScormService scormService

    def init = { servletContext ->
        scormService.cleanExtractedFiles()
        bootstrapService.createRoles()
        bootstrapService.createDevelopmentUsers()
        bootstrapService.createSampleCourses()
        bootstrapService.createSampleBadges()
    }

    def destroy = {
    }
}
