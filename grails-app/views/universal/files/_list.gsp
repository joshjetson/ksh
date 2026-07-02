<%-- Files (Syllabus, Handbook, Vocabulary lists…). Model: items (List<FileResource>),
     user. Downloads served behind auth via /universal/asset. Read-only for students. --%>
<%
    def rows = (items ?: []).sort { (it.category ?: '') + it.title }
    def byCat = rows.groupBy { it.category ?: 'General' }
%>
<div>
    <div class="flex items-center justify-between mb-5">
        <div>
            <h2 class="text-xl font-bold text-stone-800">Files</h2>
            <p class="text-sm text-stone-400">Syllabus, handbook, vocabulary lists, and more.</p>
        </div>
        <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
            <button class="btn-cta" hx-get="/universal/showView" hx-vals='{"template": "files/form"}'
                    hx-target="#content" hx-swap="innerHTML">+ New</button>
        </sec:ifAnyGranted>
    </div>

    <g:if test="${rows}">
        <g:each in="${byCat}" var="grp">
            <div class="mb-5">
                <h3 class="text-xs font-semibold text-stone-400 uppercase tracking-wide mb-2">${grp.key}</h3>
                <div class="space-y-2">
                    <g:each in="${grp.value}" var="f">
                        <div class="card p-4 flex items-center justify-between gap-3">
                            <div class="min-w-0">
                                <p class="font-semibold text-stone-800 truncate">${f.title}</p>
                                <g:if test="${f.hasFile()}"><p class="text-xs text-stone-400 truncate">${f.fileFileName}</p></g:if>
                            </div>
                            <div class="flex items-center gap-2 shrink-0">
                                <g:if test="${f.hasFile()}">
                                    <a href="/universal/asset?domainName=FileResource&amp;id=${f.id}&amp;field=file" target="_blank" rel="noopener"
                                       class="btn-secondary btn-sm">Open</a>
                                </g:if>
                                <g:elseif test="${f.externalUrl}">
                                    <a href="${f.externalUrl}" target="_blank" rel="noopener" class="btn-secondary btn-sm">Open link</a>
                                </g:elseif>
                                <sec:ifAnyGranted roles="ROLE_ADMIN,ROLE_TEACHER">
                                    <button class="btn-delete"
                                            hx-post="/universal/delete/${f.id}?domainName=FileResource"
                                            hx-vals='{"template": "files/list", "data[items]": "list:FileResource", "data[user]": "currentUser"}'
                                            hx-target="#content" hx-swap="innerHTML" hx-confirm="Delete ${f.title}?">Delete</button>
                                </sec:ifAnyGranted>
                            </div>
                        </div>
                    </g:each>
                </div>
            </div>
        </g:each>
    </g:if>
    <g:else>
        <g:render template="/universal/components/emptyState" model="[message: 'No files yet']"/>
    </g:else>
</div>
