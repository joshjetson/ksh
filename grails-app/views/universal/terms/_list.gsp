<%-- School terms (admin). Model: items (List<Term>), user. Setting one active
     deactivates the rest (domain hook). --%>
<% def rows = (items ?: []).sort { it.startsOn }.reverse() %>
<div class="max-w-xl mx-auto">
    <div class="flex items-center justify-between mb-5">
        <div>
            <h2 class="text-xl font-bold text-stone-800">Terms</h2>
            <p class="text-sm text-stone-400">School terms for enrollment &amp; grading.</p>
        </div>
        <button class="btn-cta" hx-get="/universal/showView" hx-vals='{"template": "terms/form"}' hx-target="#content" hx-swap="innerHTML">+ New</button>
    </div>
    <g:if test="${rows}">
        <div class="space-y-2">
            <g:each in="${rows}" var="t">
                <div class="card p-4 flex items-center justify-between gap-3">
                    <div class="min-w-0">
                        <p class="font-semibold text-stone-800">${t.name} <g:if test="${t.active}"><g:render template="/universal/components/pill" model="[label: 'Active', tone: 'emerald']"/></g:if></p>
                        <p class="text-xs text-stone-400"><g:formatDate date="${t.startsOn}" format="MMM d, yyyy"/> &ndash; <g:formatDate date="${t.endsOn}" format="MMM d, yyyy"/></p>
                    </div>
                    <div class="flex flex-wrap items-center gap-2 justify-end shrink-0">
                        <button class="btn-secondary btn-sm" hx-get="/universal/showView" hx-vals='{"template": "terms/form", "data[item]": "get:Term:tId", "tId": "${t.id}"}' hx-target="#content" hx-swap="innerHTML">Edit</button>
                        <button class="btn-delete" hx-post="/universal/delete/${t.id}?domainName=Term" hx-vals='{"template": "terms/list", "data[items]": "list:Term", "data[user]": "currentUser"}' hx-target="#content" hx-swap="innerHTML" hx-confirm="Delete ${t.name}?">Delete</button>
                    </div>
                </div>
            </g:each>
        </div>
    </g:if>
    <g:else><g:render template="/universal/components/emptyState" model="[message: 'No terms yet']"/></g:else>
</div>
