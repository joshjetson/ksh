<%-- New file. Model: item (FileResource, optional). Upload a file OR link out. --%>
<%
    def editing  = item?.id != null
    def action   = editing ? "/universal/update/${item.id}?domainName=FileResource" : "/universal/save?domainName=FileResource"
    def listView = '{"template": "files/list", "data[items]": "list:FileResource", "data[user]": "currentUser"}'
%>
<div class="max-w-lg">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${listView}'
            hx-target="#content" hx-swap="innerHTML">&larr; Files</button>
    <div class="card p-6">
        <h2 class="text-xl font-bold text-stone-800 mb-5">${editing ? 'Edit file' : 'Add a file'}</h2>
        <form hx-post="${action}" hx-vals='${listView}' hx-encoding="multipart/form-data"
              hx-target="#content" hx-swap="innerHTML" class="space-y-4">
            <g:render template="/universal/components/input"
                      model="[name: 'title', label: 'Title', required: true, value: item?.title]"/>
            <g:render template="/universal/components/input"
                      model="[name: 'category', label: 'Category', value: item?.category, placeholder: 'e.g. Syllabus']"/>
            <div>
                <label class="block text-sm font-medium text-stone-700 mb-1">Upload a file</label>
                <input type="file" name="file"
                       class="block w-full text-sm text-stone-600 file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:bg-rose-100 file:text-rose-700 file:font-medium hover:file:bg-rose-200 cursor-pointer"/>
                <g:if test="${item?.hasFile()}"><p class="text-xs text-stone-400 mt-1">Current: ${item.fileFileName} — choose a new file to replace it.</p></g:if>
            </div>
            <p class="text-center text-xs text-stone-400">— or —</p>
            <g:render template="/universal/components/input"
                      model="[name: 'externalUrl', label: 'Link to an external file', type: 'url', value: item?.externalUrl, placeholder: 'https://…']"/>
            <g:render template="/universal/components/button"
                      model="[text: editing ? 'Save' : 'Add file', type: 'submit', variant: 'primary', full: true]"/>
        </form>
    </div>
</div>
