<div class="flex items-center justify-between mb-6 gap-3">
    <h2 class="text-xl font-bold text-stone-800">Discount Codes</h2>
    <button hx-get="/universal/showView"
            hx-vals='{"template": "discounts/form"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="px-4 py-2 bg-rose-700 hover:bg-rose-800 text-white rounded-lg text-sm font-medium min-h-[44px]">
        + New code
    </button>
</div>

<g:if test="${codes}">
    <div class="bg-white rounded-xl shadow-sm border border-stone-200 divide-y divide-stone-100 overflow-hidden">
        <g:each in="${codes}" var="c">
            <div class="px-4 py-3 flex items-center gap-3">
                <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 flex-wrap">
                        <span class="font-mono font-semibold text-stone-800">${c.code}</span>
                        <span class="text-xs px-2 py-0.5 rounded-full ${c.active ? 'bg-emerald-100 text-emerald-800' : 'bg-stone-200 text-stone-500'}">${c.active ? 'Active' : 'Inactive'}</span>
                    </div>
                    <p class="text-xs text-stone-400 mt-0.5">
                        ${c.summary()} · used ${c.timesUsed}<g:if test="${c.usageLimit}">/${c.usageLimit}</g:if><g:if test="${c.expiresOn}"> · expires <g:formatDate date="${c.expiresOn}" format="MMM d, yyyy"/></g:if>
                    </p>
                </div>
                <button hx-get="/universal/showView"
                        hx-vals='{"template": "discounts/form", "data[code]": "get:DiscountCode:codeId", "codeId": "${c.id}"}'
                        hx-target="#content"
                        hx-swap="innerHTML"
                        class="text-sm text-stone-500 hover:text-stone-700 px-2 min-h-[44px]">Edit</button>
                <button hx-post="/universal/delete/${c.id}?domainName=DiscountCode"
                        hx-vals='{"template": "discounts/list", "data[codes]": "list:DiscountCode"}'
                        hx-target="#content"
                        hx-swap="innerHTML"
                        hx-confirm="Delete code ${c.code}?"
                        class="text-sm text-red-500 hover:text-red-700 px-2 min-h-[44px]">Delete</button>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No discount codes yet']"/>
</g:else>
