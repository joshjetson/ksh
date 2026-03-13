<%
    def isEdit = course?.id != null
%>

<!-- Back button -->
<button hx-get="/universal/showView"
        hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser"}'
        hx-target="#content"
        hx-swap="innerHTML"
        class="flex items-center gap-2 text-sm text-stone-500 hover:text-stone-700 mb-4 min-h-[44px] py-2">
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
    </svg>
    Back
</button>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <h2 class="text-xl font-bold text-stone-800 mb-6">${isEdit ? 'Edit Course' : 'Create Course'}</h2>

    <form hx-${isEdit ? 'put' : 'post'}="${isEdit ? '/api/universal/Course/' + course.id + '?domainName=Course&refreshId=' + course.id : '/api/universal/Course?domainName=Course'}"
          hx-vals='{"template": "courses/browse", "data[courses]": "list:Course", "data[user]": "currentUser"}'
          hx-target="#content"
          hx-swap="innerHTML"
          class="space-y-4">

        <g:if test="${isEdit}">
            <input type="hidden" name="id" value="${course.id}"/>
        </g:if>
        <input type="hidden" name="creator.id" value="${user.id}"/>

        <g:render template="/universal/components/input" model="[name: 'shortTitle', label: 'Short Title', value: course?.shortTitle, required: true, placeholder: 'e.g. Korean Basics']"/>
        <g:render template="/universal/components/input" model="[name: 'longTitle', label: 'Full Title', value: course?.longTitle, required: true, placeholder: 'e.g. Introduction to Korean Language - Beginner Level']"/>
        <g:render template="/universal/components/textarea" model="[name: 'shortDescription', label: 'Short Description', value: course?.shortDescription, rows: 2, placeholder: 'Brief summary shown on course cards']"/>
        <g:render template="/universal/components/textarea" model="[name: 'longDescription', label: 'Full Description', value: course?.longDescription, rows: 5, placeholder: 'Detailed course description shown on preview page']"/>
        <g:render template="/universal/components/input" model="[name: 'tags', label: 'Tags (comma-separated)', value: course?.tags, placeholder: 'grammar, beginner, hangul']"/>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <g:render template="/universal/components/input" model="[name: 'costKCredits', label: 'Cost (K-Credits)', type: 'number', value: course?.costKCredits ?: 0]"/>
            <g:render template="/universal/components/input" model="[name: 'pointReward', label: 'Point Reward', type: 'number', value: course?.pointReward ?: 0]"/>
        </div>

        <g:render template="/universal/components/input" model="[name: 'badgeReward', label: 'Badge Reward (optional)', value: course?.badgeReward, placeholder: 'Badge name awarded on completion']"/>

        <div class="pt-2">
            <g:render template="/universal/components/button" model="[text: isEdit ? 'Update Course' : 'Create Course', type: 'submit', full: true]"/>
        </div>
    </form>
</div>
