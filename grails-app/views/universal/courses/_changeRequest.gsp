<div class="flex items-center gap-3 mb-6">
    <button hx-get="/universal/showView"
            hx-vals='{"template": "courses/myCourses", "data[user]": "currentUser", "data[enrollments]": "filter:CourseEnrollment:user.id=currentUserId"}'
            hx-target="#content" hx-swap="innerHTML"
            class="text-stone-500 hover:text-stone-700 min-h-[44px] py-2" aria-label="Back">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
    </button>
    <h2 class="text-xl font-bold text-stone-800">Request a change</h2>
</div>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <p class="text-sm text-stone-500 mb-4">Course: <span class="font-medium text-stone-800">${enrollment.course?.shortTitle}</span></p>

    <form hx-post="/universal/save?domainName=EnrollmentChangeRequest"
          hx-vals='{"template": "courses/myCourses", "data[user]": "currentUser", "data[enrollments]": "filter:CourseEnrollment:user.id=currentUserId"}'
          hx-target="#content" hx-swap="innerHTML" class="space-y-4">

        <input type="hidden" name="enrollment.id" value="${enrollment.id}"/>

        <g:render template="/universal/components/select"
                  model="[name: 'kind', label: 'Request type', required: true, selected: 'WITHDRAW',
                          options: [[value: 'WITHDRAW', label: 'Withdraw from this course'], [value: 'EXTENSION', label: 'Request a deadline extension']]]"/>

        <g:render template="/universal/components/input"
                  model="[name: 'requestedDate', label: 'Preferred new date (extensions only)', type: 'date']"/>

        <g:render template="/universal/components/textarea"
                  model="[name: 'message', label: 'Tell us why', required: true, rows: 4, placeholder: 'A short note for the school…']"/>

        <div class="pt-2">
            <g:render template="/universal/components/button" model="[text: 'Submit request', type: 'submit']"/>
        </div>
    </form>
</div>
