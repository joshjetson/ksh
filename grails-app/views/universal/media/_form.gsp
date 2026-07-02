<%-- New media post. Model: (none needed). Teachers/admin only (gated server-side). --%>
<% def listView = '{"template": "media/list", "data[items]": "list:MediaPost", "data[user]": "currentUser"}' %>
<div class="max-w-lg">
    <button class="text-stone-400 hover:text-rose-700 text-sm mb-4" hx-get="/universal/showView" hx-vals='${listView}'
            hx-target="#content" hx-swap="innerHTML">&larr; Media</button>
    <div class="card p-6">
        <h2 class="text-xl font-bold text-stone-800 mb-5">Post a photo</h2>
        <form hx-post="/universal/save?domainName=MediaPost" hx-vals='${listView}' hx-encoding="multipart/form-data"
              hx-target="#content" hx-swap="innerHTML" class="space-y-4">
            <div>
                <label class="block text-sm font-medium text-stone-700 mb-1">Photo *</label>
                <input type="file" name="image" accept="image/*" required
                       class="block w-full text-sm text-stone-600 file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:bg-rose-100 file:text-rose-700 file:font-medium hover:file:bg-rose-200 cursor-pointer"/>
            </div>
            <g:render template="/universal/components/textarea"
                      model="[name: 'caption', label: 'Caption', rows: 2]"/>
            <g:render template="/universal/components/button"
                      model="[text: 'Post', type: 'submit', variant: 'primary', full: true]"/>
        </form>
    </div>
</div>
