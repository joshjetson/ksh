<%-- Media feed. Model: items (List<MediaPost>), user. Photos served behind auth via
     /universal/asset (never public). Teachers/admin post; everyone views. --%>
<% def rows = (items ?: []).sort { it.dateCreated }.reverse() %>
<div>
    <div class="flex items-center justify-between mb-5">
        <div>
            <h2 class="text-xl font-bold text-stone-800">Media</h2>
            <p class="text-sm text-stone-400">Photos and moments from around the school.</p>
        </div>
        <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
            <button class="btn-cta" hx-get="/universal/showView" hx-vals='{"template": "media/form"}'
                    hx-target="#content" hx-swap="innerHTML">+ Post</button>
        </sec:ifAnyGranted>
    </div>

    <g:if test="${rows}">
        <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
            <g:each in="${rows}" var="m">
                <div class="card overflow-hidden">
                    <img src="/universal/asset?domainName=MediaPost&amp;id=${m.id}&amp;field=image&amp;v=${m.lastUpdated?.time ?: 0}"
                         alt="${m.caption ?: ''}" loading="lazy" class="w-full h-40 object-cover"/>
                    <div class="p-3">
                        <g:if test="${m.caption}"><p class="text-sm text-stone-600">${m.caption}</p></g:if>
                        <div class="mt-1 flex items-center justify-between">
                            <p class="text-xs text-stone-400"><g:formatDate date="${m.dateCreated}" format="MMM d"/><g:if test="${m.author}"> · ${m.author.name ?: m.author.username}</g:if></p>
                            <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                                <button class="btn-delete-link"
                                        hx-post="/universal/delete/${m.id}?domainName=MediaPost"
                                        hx-vals='{"template": "media/list", "data[items]": "list:MediaPost", "data[user]": "currentUser"}'
                                        hx-target="#content" hx-swap="innerHTML" hx-confirm="Delete this photo?">Delete</button>
                            </sec:ifAnyGranted>
                        </div>
                    </div>
                </div>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <g:render template="/universal/components/emptyState" model="[message: 'No photos yet']"/>
    </g:else>
</div>
