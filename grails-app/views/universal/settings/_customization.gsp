<% def cfg = configs ? configs[0] : null %>

<h3 class="text-lg font-semibold text-stone-800 mb-4">Customization</h3>

<g:if test="${cfg}">
<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <form hx-post="/universal/update/${cfg.id}?domainName=AppConfig"
          hx-vals='{"template": "settings/customization", "data[configs]": "list:AppConfig"}'
          hx-target="#settings-content"
          hx-swap="innerHTML"
          hx-encoding="multipart/form-data"
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

        <!-- Branding -->
        <div class="border-t border-stone-200 pt-6 space-y-4">
            <h4 class="text-sm font-semibold text-stone-800">Branding</h4>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <g:render template="/universal/components/input" model="[name: 'logoText', label: 'Logo', value: cfg.logoText, required: true, placeholder: '한']"/>
                <div class="md:col-span-2">
                    <g:render template="/universal/components/input" model="[name: 'siteTitle', label: 'Site Title', value: cfg.siteTitle, required: true, placeholder: 'Korean School House']"/>
                </div>
            </div>

            <g:render template="/universal/components/input" model="[name: 'siteSubtitle', label: 'Subtitle', value: cfg.siteSubtitle, placeholder: '한국어 학교']"/>

            <div>
                <label class="block text-sm font-medium text-stone-700 mb-1">Login Background Image</label>
                <g:if test="${cfg.backgroundImageFileName}">
                    <div class="flex items-center gap-3 p-3 rounded-lg bg-green-50 border border-green-200 mb-2">
                        <svg class="w-5 h-5 text-green-600 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                        <span class="text-sm text-green-800 truncate flex-1">${cfg.backgroundImageFileName}</span>
                    </div>
                </g:if>
                <input type="file"
                       name="backgroundImage"
                       accept="image/*"
                       class="block w-full text-sm text-stone-500 file:mr-4 file:py-3 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-rose-50 file:text-rose-700 hover:file:bg-rose-100 file:min-h-[44px] file:cursor-pointer"/>
                <p class="text-xs text-stone-400 mt-1">${cfg.backgroundImageFileName ? 'Upload a new image to replace the current background' : 'Replaces the bisque solid background on the login page. Leave blank to keep the default.'}</p>
            </div>
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
