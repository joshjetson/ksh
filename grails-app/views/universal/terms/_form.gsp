<%-- New / edit term (admin). Model: item (Term, optional). Marking one active
     deactivates the others (domain hook). --%>
<%
    def editing = item?.id != null
    def action  = editing ? "/universal/update/${item.id}?domainName=Term" : "/universal/save?domainName=Term"
    def back    = '{"template": "terms/list", "data[items]": "list:Term", "data[user]": "currentUser"}'
    String startsStr = item?.startsOn ? formatDate(date: item.startsOn, format: "yyyy-MM-dd") : ''
    String endsStr   = item?.endsOn   ? formatDate(date: item.endsOn,   format: "yyyy-MM-dd") : ''
%>
<div class="max-w-lg mx-auto">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${back}' hx-target="#content" hx-swap="innerHTML">&larr; Terms</button>
    <div class="card p-6">
        <h2 class="text-xl font-bold text-stone-800 mb-5">${editing ? 'Edit' : 'New'} term</h2>
        <form hx-post="${action}" hx-vals='${back}' hx-target="#content" hx-swap="innerHTML" class="space-y-4">
            <g:render template="/universal/components/input" model="[name: 'name', label: 'Name', required: true, value: item?.name, placeholder: 'e.g. Fall 2026']"/>
            <div class="grid grid-cols-2 gap-3">
                <g:render template="/universal/components/input" model="[name: 'startsOn', label: 'Starts', type: 'date', required: true, value: startsStr]"/>
                <g:render template="/universal/components/input" model="[name: 'endsOn', label: 'Ends', type: 'date', required: true, value: endsStr]"/>
            </div>
            <label class="flex items-center gap-2 text-sm text-stone-700">
                <input type="hidden" name="_active"/>
                <input type="checkbox" name="active" value="true" ${item?.active ? 'checked' : ''} class="h-4 w-4 rounded border-cream-300 text-rose-600 focus:ring-rose-500"/>
                Active term (the current one)
            </label>
            <g:render template="/universal/components/button" model="[text: editing ? 'Save' : 'Create', type: 'submit', variant: 'primary', full: true]"/>
        </form>
    </div>
</div>
