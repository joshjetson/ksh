<%-- Announcements. Model: items (List<Announcement>), user. Read-only for students;
     staff/admin get create/edit/delete. Pinned announcements sort first. --%>
<%
    def rows = (items ?: []).sort { a, b -> (b.pinned <=> a.pinned) ?: (b.dateCreated <=> a.dateCreated) }
%>
<div>
    <div class="flex items-center justify-between mb-5">
        <div>
            <h2 class="text-xl font-bold text-stone-800">Announcements</h2>
            <p class="text-sm text-stone-400">News and updates for our students.</p>
        </div>
        <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
            <button class="btn-cta" hx-get="/universal/showView" hx-vals='{"template": "announcements/form"}'
                    hx-target="#content" hx-swap="innerHTML">+ New</button>
        </sec:ifAnyGranted>
    </div>

    <g:if test="${rows}">
        <div class="space-y-3">
            <g:each in="${rows}" var="a">
                <div class="card p-4">
                    <div class="flex items-start justify-between gap-3">
                        <div class="min-w-0">
                            <div class="flex items-center gap-2">
                                <g:if test="${a.pinned}"><g:render template="/universal/components/pill" model="[label: 'Pinned', tone: 'gold']"/></g:if>
                                <h3 class="font-semibold text-stone-800">${a.title}</h3>
                            </div>
                            <p class="mt-1 text-sm text-stone-600 whitespace-pre-line">${a.body}</p>
                            <p class="mt-2 text-xs text-stone-400"><g:formatDate date="${a.dateCreated}" format="MMM d, yyyy"/><g:if test="${a.author}"> · ${a.author.name ?: a.author.username}</g:if></p>
                        </div>
                        <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                            <div class="flex items-center gap-2 shrink-0">
                                <button class="btn-secondary btn-sm" hx-get="/universal/showView"
                                        hx-vals='{"template": "announcements/form", "data[item]": "get:Announcement:aId", "aId": "${a.id}"}'
                                        hx-target="#content" hx-swap="innerHTML">Edit</button>
                                <button class="btn-delete"
                                        hx-post="/universal/delete/${a.id}?domainName=Announcement"
                                        hx-vals='{"template": "announcements/list", "data[items]": "list:Announcement", "data[user]": "currentUser"}'
                                        hx-target="#content" hx-swap="innerHTML" hx-confirm="Delete this announcement?">Delete</button>
                            </div>
                        </sec:ifAnyGranted>
                    </div>
                </div>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <g:render template="/universal/components/emptyState" model="[message: 'No announcements yet']"/>
    </g:else>
</div>
