<%-- New channel (admin). Posts Channel → back to the channel list. --%>
<% def homeVals = '{"template": "messaging/home", "data[list]": "service:messageService:channelList", "data[user]": "currentUser"}' %>
<div class="max-w-lg">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${homeVals}'
            hx-target="#content" hx-swap="innerHTML">&larr; Channels</button>
    <div class="card p-6">
        <h2 class="text-xl font-bold text-stone-800 mb-5">New channel</h2>
        <form hx-post="/universal/save?domainName=Channel" hx-vals='${homeVals}' hx-target="#content" hx-swap="innerHTML" class="space-y-4">
            <g:render template="/universal/components/input"
                      model="[name: 'name', label: 'Name', required: true, placeholder: 'e.g. General']"/>
            <g:render template="/universal/components/input"
                      model="[name: 'description', label: 'Description', placeholder: 'What is this channel for?']"/>
            <g:render template="/universal/components/select"
                      model="[name: 'visibility', label: 'Who can see it',
                              options: [[value: 'school', label: 'Whole school (students + staff)'],
                                        [value: 'staff', label: 'Staff only']]]"/>
            <g:render template="/universal/components/button"
                      model="[text: 'Create channel', type: 'submit', variant: 'primary', full: true]"/>
        </form>
    </div>
</div>
