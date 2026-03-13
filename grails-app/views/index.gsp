<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Korean School House</title>
</head>
<body>

<div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
    <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-gray-900">Korean School House</h1>
        <p class="mt-2 text-gray-600">Welcome</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div class="bg-white rounded-lg shadow p-6">
            <h2 class="text-lg font-semibold text-gray-900 mb-4">System Status</h2>
            <dl class="space-y-2 text-sm">
                <div class="flex justify-between">
                    <dt class="text-gray-500">Environment</dt>
                    <dd class="text-gray-900">${grails.util.Environment.current.name}</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-500">App Version</dt>
                    <dd class="text-gray-900"><g:meta name="info.app.version"/></dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-500">Grails</dt>
                    <dd class="text-gray-900"><g:meta name="info.app.grailsVersion"/></dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-500">JVM</dt>
                    <dd class="text-gray-900">${System.getProperty('java.version')}</dd>
                </div>
            </dl>
        </div>

        <div class="bg-white rounded-lg shadow p-6">
            <h2 class="text-lg font-semibold text-gray-900 mb-4">Application Stats</h2>
            <dl class="space-y-2 text-sm">
                <div class="flex justify-between">
                    <dt class="text-gray-500">Controllers</dt>
                    <dd class="text-gray-900">${grailsApplication.controllerClasses.size()}</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-500">Domains</dt>
                    <dd class="text-gray-900">${grailsApplication.domainClasses.size()}</dd>
                </div>
                <div class="flex justify-between">
                    <dt class="text-gray-500">Services</dt>
                    <dd class="text-gray-900">${grailsApplication.serviceClasses.size()}</dd>
                </div>
            </dl>
        </div>
    </div>
</div>

</body>
</html>
