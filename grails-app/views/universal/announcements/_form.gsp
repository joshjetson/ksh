<%-- New / edit announcement. Model: item (Announcement, optional). --%>
<%
    def editing  = item?.id != null
    def action   = editing ? "/universal/update/${item.id}?domainName=Announcement" : "/universal/save?domainName=Announcement"
    def listView = '{"template": "announcements/list", "data[items]": "list:Announcement", "data[user]": "currentUser"}'
%>
<div class="max-w-lg">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${listView}'
            hx-target="#content" hx-swap="innerHTML">&larr; Announcements</button>
    <div class="card p-6">
        <h2 class="text-xl font-bold text-stone-800 mb-5">${editing ? 'Edit announcement' : 'New announcement'}</h2>
        <form hx-post="${action}" hx-vals='${listView}' hx-target="#content" hx-swap="innerHTML" class="space-y-4">
            <g:render template="/universal/components/input"
                      model="[name: 'title', label: 'Title', required: true, value: item?.title]"/>
            <g:render template="/universal/components/textarea"
                      model="[name: 'body', label: 'Message', rows: 6, required: true, value: item?.body]"/>
            <label class="flex items-center gap-2 text-sm text-stone-700">
                <input type="hidden" name="_pinned"/>
                <input type="checkbox" name="pinned" value="true" ${item?.pinned ? 'checked' : ''}
                       class="h-4 w-4 rounded border-cream-300 text-rose-600 focus:ring-rose-500"/>
                Pin to top
            </label>
            <g:render template="/universal/components/button"
                      model="[text: editing ? 'Save' : 'Post announcement', type: 'submit', variant: 'primary', full: true]"/>
        </form>
    </div>
</div>
