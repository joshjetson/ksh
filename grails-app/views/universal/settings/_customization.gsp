<% def cfg = configs ? configs[0] : null %>

<h3 class="text-lg font-semibold text-stone-800 mb-4">Customization</h3>

<g:if test="${cfg}">
<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <form hx-post="/universal/update/${cfg.id}?domainName=AppConfig"
          hx-vals='{"template": "settings/customization", "data[configs]": "list:AppConfig"}'
          hx-target="#settings-content"
          hx-swap="innerHTML"
          class="space-y-6">

        <!-- Newsfeed Toggle -->
        <div>
            <label class="flex items-center justify-between min-h-[44px]">
                <div>
                    <span class="text-sm font-medium text-stone-700">Newsfeed Tab</span>
                    <p class="text-xs text-stone-400">Show or hide the Newsfeed tab for all users</p>
                </div>
                <input type="hidden" name="newsfeedEnabled" value="false"/>
                <input type="checkbox" name="newsfeedEnabled" value="true"
                       ${cfg.newsfeedEnabled ? 'checked' : ''}
                       class="rounded border-stone-300 text-rose-700 w-5 h-5"/>
            </label>
        </div>

        <!-- Courses Label -->
        <div>
            <g:render template="/universal/components/select" model="[
                name: 'coursesLabel',
                label: 'Content Label',
                selected: cfg.coursesLabel,
                options: [
                    [value: 'Courses', label: 'Courses (My Courses, Search courses...)'],
                    [value: 'Quests', label: 'Quests (My Quests, Search quests...)'],
                    [value: 'Topics', label: 'Topics (My Topics, Search topics...)'],
                    [value: 'Trainings', label: 'Trainings (My Trainings, Search trainings...)'],
                    [value: 'Lessons', label: 'Lessons (My Lessons, Search lessons...)'],
                    [value: 'Modules', label: 'Modules (My Modules, Search modules...)']
                ]
            ]"/>
            <p class="text-xs text-stone-400 mt-1">Changes the tab label and search placeholder. Requires page reload to take effect in the nav bar.</p>
        </div>

        <!-- Profile Upload Toggle -->
        <div>
            <label class="flex items-center justify-between min-h-[44px]">
                <div>
                    <span class="text-sm font-medium text-stone-700">Profile Picture Upload</span>
                    <p class="text-xs text-stone-400">When off, users choose from a fixed set of profile pictures</p>
                </div>
                <input type="hidden" name="profileUploadEnabled" value="false"/>
                <input type="checkbox" name="profileUploadEnabled" value="true"
                       ${cfg.profileUploadEnabled ? 'checked' : ''}
                       class="rounded border-stone-300 text-rose-700 w-5 h-5"/>
            </label>
        </div>

        <div class="pt-2">
            <g:render template="/universal/components/button" model="[text: 'Save Settings', type: 'submit']"/>
        </div>
    </form>
</div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'Configuration not found. Please restart the application.']"/>
</g:else>
