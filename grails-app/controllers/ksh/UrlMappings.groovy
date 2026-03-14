package ksh

class UrlMappings {
    static mappings = {
        // REST API routes
        "/api/universal/$domainName"(controller: "universal", action: "list", method: "GET")
        "/api/universal/$domainName/count"(controller: "universal", action: "count", method: "GET")
        "/api/universal/$domainName"(controller: "universal", action: "save", method: "POST")
        "/api/universal/$domainName/$id"(controller: "universal", action: "show", method: "GET")
        "/api/universal/$domainName/$id"(controller: "universal", action: "update", method: "PUT")
        "/api/universal/$domainName/$id"(controller: "universal", action: "delete", method: "DELETE")

        // SCORM player routes
        "/scorm/player/$id"(controller: "scorm", action: "player")
        "/scorm/content/$courseId/**"(controller: "scorm", action: "content")
        "/api/scorm/$courseId/cmi"(controller: "scorm", action: "getCmiData", method: "GET")
        "/api/scorm/$courseId/cmi"(controller: "scorm", action: "saveCmiData", method: "POST")

        // Login routes
        "/login"(controller: "login", action: "index")
        "/login/auth"(controller: "login", action: "auth")

        // Generic controller/action routing
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller: "universal", action: "index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
