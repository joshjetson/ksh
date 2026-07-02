<%-- New / edit classroom. Model: item (Classroom, optional). --%>
<%
    def editing  = item?.id != null
    def action   = editing ? "/universal/update/${item.id}?domainName=Classroom" : "/universal/save?domainName=Classroom"
    def listView = '{"template": "classrooms/list", "data[items]": "list:Classroom", "data[user]": "currentUser"}'
%>
<div class="max-w-lg">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${listView}'
            hx-target="#content" hx-swap="innerHTML">&larr; Classrooms</button>
    <div class="card p-6">
        <h2 class="text-xl font-bold text-stone-800 mb-5">${editing ? "Edit ${item.name}" : 'New classroom'}</h2>
        <form hx-post="${action}" hx-vals='${listView}' hx-target="#content" hx-swap="innerHTML" class="space-y-4">
            <g:render template="/universal/components/input"
                      model="[name: 'name', label: 'Name', required: true, value: item?.name, placeholder: 'e.g. Wednesday Conversation Group']"/>
            <g:render template="/universal/components/textarea"
                      model="[name: 'description', label: 'Description', rows: 2, value: item?.description]"/>
            <g:render template="/universal/components/input"
                      model="[name: 'sortOrder', label: 'Sort order', type: 'number', value: item?.sortOrder]"/>
            <g:render template="/universal/components/button"
                      model="[text: editing ? 'Save' : 'Create classroom', type: 'submit', variant: 'primary', full: true]"/>
        </form>
    </div>
</div>
