<div class="flex items-center gap-3 mb-6">
    <button hx-get="/universal/showView"
            hx-vals='{"template": "rewards/manage", "data[badges]": "list:Badge"}'
            hx-target="#content" hx-swap="innerHTML"
            class="text-stone-500 hover:text-stone-700 min-h-[44px] py-2" aria-label="Back">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
    </button>
    <h2 class="text-xl font-bold text-stone-800">${badge ? 'Edit' : 'New'} Reward</h2>
</div>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <form hx-post="${badge ? '/universal/update/' + badge.id : '/universal/save'}?domainName=Badge"
          hx-vals='{"template": "rewards/manage", "data[badges]": "list:Badge"}'
          hx-target="#content" hx-swap="innerHTML"
          hx-encoding="multipart/form-data"
          class="space-y-4">

        <!-- Artwork -->
        <div class="flex items-center gap-4">
            <div class="w-20 h-20 rounded-full bg-stone-100 flex items-center justify-center overflow-hidden flex-shrink-0">
                <g:if test="${badge?.hasImage()}">
                    <img src="/badge/image/${badge.id}" alt="" class="w-full h-full object-cover"/>
                </g:if>
                <g:else><span class="text-3xl">&#127941;</span></g:else>
            </div>
            <div>
                <label for="image" class="block text-sm font-medium text-stone-700 mb-1">Artwork</label>
                <input type="file" id="image" name="image" accept="image/*"
                       class="text-sm text-stone-500 file:mr-2 file:py-2 file:px-3 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-stone-100 file:text-stone-700 hover:file:bg-stone-200"/>
                <g:if test="${badge?.hasImage()}"><p class="text-xs text-stone-400 mt-1">Leave empty to keep the current image.</p></g:if>
            </div>
        </div>

        <g:render template="/universal/components/input"
                  model="[name: 'name', label: 'Name', value: badge?.name, required: true, placeholder: 'e.g. Sakura Avatar']"/>

        <g:render template="/universal/components/select"
                  model="[name: 'kind', label: 'Type', required: true, selected: badge?.kind?.name() ?: 'BADGE',
                          options: [[value: 'BADGE', label: 'Badge — achievement pin'], [value: 'AVATAR', label: 'Avatar — equippable profile picture']]]"/>

        <div class="border-t border-stone-100 pt-4">
            <p class="text-sm font-medium text-stone-700 mb-2">Auto-grant rule <span class="text-stone-400 font-normal">(for global milestones — leave as None for course or manual rewards)</span></p>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <g:render template="/universal/components/select"
                          model="[name: 'unlockType', label: 'Milestone', selected: badge?.unlockType?.name() ?: 'NONE',
                                  options: [[value: 'NONE', label: 'None'], [value: 'POINTS', label: 'Reach N total points'], [value: 'COURSE_COUNT', label: 'Complete N courses'], [value: 'ANNIVERSARY', label: 'N-year anniversary']]]"/>
                <g:render template="/universal/components/input"
                          model="[name: 'unlockThreshold', label: 'Threshold (N)', type: 'number', value: badge?.unlockThreshold, placeholder: 'e.g. 500, 10, 1']"/>
            </div>
        </div>

        <g:render template="/universal/components/input"
                  model="[name: 'icon', label: 'Emoji fallback (optional)', value: badge?.icon, placeholder: '🏅']"/>

        <g:render template="/universal/components/textarea"
                  model="[name: 'description', label: 'Description', value: badge?.description, rows: 2]"/>

        <g:render template="/universal/components/textarea"
                  model="[name: 'requirements', label: 'How to earn it (shown to learners)', value: badge?.requirements, rows: 2]"/>

        <div class="pt-2">
            <g:render template="/universal/components/button" model="[text: badge ? 'Save changes' : 'Create reward', type: 'submit']"/>
        </div>
    </form>
</div>
