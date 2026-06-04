<h2 class="text-xl font-bold text-stone-800 mb-4">Change Requests</h2>

<g:if test="${requests}">
    <div class="space-y-3">
        <g:each in="${requests}" var="r">
            <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4">
                <div class="flex items-center justify-between gap-3 mb-2">
                    <div class="min-w-0">
                        <span class="font-medium text-stone-800">${r.requestedBy?.name ?: r.requestedBy?.username}</span>
                        <span class="text-xs text-stone-400">· ${r.enrollment?.course?.shortTitle}</span>
                    </div>
                    <span class="inline-block text-xs font-medium px-2 py-0.5 rounded-full ${r.kind == 'WITHDRAW' ? 'bg-rose-100 text-rose-700' : 'bg-sky-100 text-sky-800'}">
                        ${r.kind == 'WITHDRAW' ? 'Withdrawal' : 'Extension'}
                    </span>
                </div>
                <g:if test="${r.requestedDate}">
                    <p class="text-xs text-stone-500 mb-1">Preferred date: <g:formatDate date="${r.requestedDate}" format="MMM d, yyyy"/></p>
                </g:if>
                <p class="text-sm text-stone-600 mb-3">${r.message}</p>

                <form hx-post="/universal/update/${r.id}?domainName=EnrollmentChangeRequest"
                      hx-vals='{"template": "requests/manage", "data[requests]": "filter:EnrollmentChangeRequest:status=PENDING"}'
                      hx-target="#content" hx-swap="innerHTML"
                      class="space-y-2">
                    <textarea name="responseMessage" rows="2" placeholder="Response to the student (optional)…"
                              class="block w-full px-3 py-2 border border-stone-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500 resize-none"></textarea>
                    <div class="flex gap-2">
                        <button type="submit" name="status" value="APPROVED"
                                class="flex-1 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium min-h-[44px]">Approve</button>
                        <button type="submit" name="status" value="DENIED"
                                class="flex-1 px-4 py-2 bg-stone-200 hover:bg-stone-300 text-stone-800 rounded-lg text-sm font-medium min-h-[44px]">Deny</button>
                    </div>
                </form>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No pending requests 🎉']"/>
</g:else>
