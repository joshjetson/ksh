<%-- Person actions sheet (Message / Block). Model: pa (personActions map). The Message
     button find-or-creates a DM (service:startDm) and opens it; Block/Unblock is generic
     owner-owned CRUD on UserBlock. template:"" keeps the block post quiet (the strip
     refetches via 'peoplechanged'). --%>
<g:if test="${pa}">
<% def after = "if(event.detail.successful){document.body.dispatchEvent(new Event('peoplechanged'));closeActionSheet()}" %>
<div class="flex items-center gap-3 mb-3">
    <g:render template="/universal/components/avatar" model="[name: pa.name, size: 'h-11 w-11', textSize: 'text-base']"/>
    <p class="font-semibold text-stone-800 truncate">${pa.name}</p>
</div>
<div class="space-y-1">
    <g:if test="${pa.canMessage}">
        <button class="member-act text-rose-700" hx-get="/universal/showView"
                hx-vals='{"template":"messaging/channel","data[view]":"service:messageService:startDm","data[user]":"currentUser","targetUserId":"${pa.userId}"}'
                hx-target="#content" hx-swap="innerHTML" hx-on::after-request="closeActionSheet()">💬 Message</button>
    </g:if>
    <g:if test="${pa.blocked}">
        <button class="member-act" hx-post="/universal/delete/${pa.blockId}?domainName=UserBlock"
                hx-vals='{"template":""}' hx-swap="none" hx-on::after-request="${after}">Unblock</button>
    </g:if>
    <g:elseif test="${pa.canBlock}">
        <button class="member-act text-rose-600" hx-post="/universal/save?domainName=UserBlock"
                hx-vals='{"blocked.id":"${pa.userId}","template":""}' hx-swap="none"
                hx-confirm="Block ${pa.name}? You won't see each other's messages." hx-on::after-request="${after}">Block</button>
    </g:elseif>
</div>
</g:if>
