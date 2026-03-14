package ksh

class UrlMappings {
    static mappings = {
        // SCORM routes (need explicit mapping for wildcard and /api/ prefix)
        "/scorm/content/$courseId/**"(controller: "scorm", action: "content")
        "/api/scorm/$courseId/cmi"(controller: "scorm", action: "getCmiData", method: "GET")
        "/api/scorm/$courseId/cmi"(controller: "scorm", action: "saveCmiData", method: "POST")

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
