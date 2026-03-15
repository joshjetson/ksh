package ksh

class InvalidRouteInterceptor {

    InvalidRouteInterceptor() {
        matchAll()
    }

    boolean before() {
        def controllerClass = grailsApplication.getArtefactByLogicalPropertyName('Controller', controllerName)
        if (!controllerClass) {
            redirect uri: '/'
            return false
        }

        if (actionName && !controllerClass.clazz.declaredMethods*.name.contains(actionName)) {
            redirect uri: '/'
            return false
        }

        return true
    }

    boolean after() { true }

    void afterView() {}
}
