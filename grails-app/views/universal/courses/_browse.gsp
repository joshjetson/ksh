<% def searchLabel = config?.coursesLabel?.toLowerCase() ?: 'courses' %>

<!-- Search -->
<div class="mb-6 flex gap-2">
    <input hx-get="/universal/showView"
           hx-vals='{"template": "courses/browse", "data[courses]": "search:Course:longTitle,shortTitle,shortDescription:q", "data[user]": "currentUser", "data[config]": "get:AppConfig:configId", "configId": "${config?.id}"}'
           hx-trigger="input changed delay:300ms"
           hx-target="#content"
           hx-swap="innerHTML"
           hx-include="this"
           name="q"
           value="${params?.q ?: ''}"
           placeholder="Search ${searchLabel}..."
           class="flex-1 px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent text-base"/>
    <button hx-get="/universal/showView"
            hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser", "data[config]": "get:AppConfig:configId", "configId": "${config?.id}"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="px-4 py-3 bg-stone-200 hover:bg-stone-300 text-stone-700 rounded-lg text-sm font-medium transition-colors whitespace-nowrap">
        Clear
    </button>
</div>

<!-- Course Grid -->
<g:if test="${courses}">
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <g:each in="${courses}" var="course">
            <g:render template="/universal/components/courseCard" model="[course: course]"/>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No ${searchLabel} available yet']"/>
</g:else>
