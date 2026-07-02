<%-- "People you can message" strip (non-moderators). Model: pe (channelPeople). Tap an
     avatar → person actions (Message / Block) in the shared action sheet. Null for
     moderators (they get the moderation strip instead). --%>
<g:if test="${pe}">
<div class="flex items-center gap-2 mb-1.5">
    <span class="text-[11px] font-semibold text-stone-500 uppercase tracking-wide">Members</span>
    <span class="text-[11px] font-bold text-white bg-rose-600 rounded-full px-2 py-0.5">${pe.count}</span>
    <span class="text-[11px] text-stone-400">tap to message</span>
</div>
<g:if test="${pe.people}">
    <div class="member-strip">
        <g:each in="${pe.people}" var="person">
            <div class="member-cell">
                <button type="button" class="rounded-full"
                        hx-get="/universal/showView"
                        hx-vals='{"template":"messaging/personActions","data[pa]":"service:messageService:personActions","userId":"${person.userId}"}'
                        hx-target="#action-sheet-body" hx-swap="innerHTML" hx-on::after-request="openActionSheet()">
                    <g:render template="/universal/components/avatar" model="[name: person.name, size: 'h-11 w-11', textSize: 'text-sm']"/>
                </button>
                <span class="block text-[10px] text-center mt-0.5 w-12 truncate text-stone-400">${person.name.split(' ')[0]}</span>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <p class="text-xs text-stone-400">No one to message here yet — turn on “discoverable” in Settings so other students can reach you.</p>
</g:else>
</g:if>
