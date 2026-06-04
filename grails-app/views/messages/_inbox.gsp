<div class="flex items-center gap-3 mb-6">
    <h2 class="text-xl font-bold text-stone-800">Messages</h2>
</div>

<g:if test="${conversations}">
    <div class="bg-white rounded-xl shadow-sm border border-stone-200 divide-y divide-stone-100 overflow-hidden">
        <g:each in="${conversations}" var="c">
            <button hx-get="/messages/thread?conversationId=${c.id}"
                    hx-target="#content"
                    hx-swap="innerHTML"
                    class="w-full text-left px-4 py-3 hover:bg-stone-50 flex items-center gap-3 min-h-[44px] transition-colors">
                <div class="w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center flex-shrink-0 overflow-hidden">
                    <g:if test="${c.student?.avatar}">
                        <img src="${c.student.avatar}" alt="" class="w-10 h-10 rounded-full object-cover"/>
                    </g:if>
                    <g:else>
                        <span class="text-sm font-bold text-rose-700">${(c.student?.name ?: c.student?.username)?.charAt(0)?.toUpperCase()}</span>
                    </g:else>
                </div>
                <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2">
                        <span class="font-medium text-stone-800 truncate">${c.student?.name ?: c.student?.username}</span>
                        <g:if test="${c.awaitingReply}">
                            <span class="text-xs bg-rose-600 text-white px-2 py-0.5 rounded-full flex-shrink-0">Needs reply</span>
                        </g:if>
                    </div>
                    <p class="text-xs text-stone-400 truncate">${c.student?.email ?: ''}</p>
                </div>
                <g:if test="${c.lastMessageAt}">
                    <span class="text-xs text-stone-400 flex-shrink-0"><g:formatDate date="${c.lastMessageAt}" format="MMM d, h:mm a"/></span>
                </g:if>
            </button>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No student messages yet']"/>
</g:else>
