<div class="flex items-center gap-3 mb-6">
    <button hx-get="/universal/showView"
            hx-vals='{"template": "discounts/list", "data[codes]": "list:DiscountCode"}'
            hx-target="#content"
            hx-swap="innerHTML"
            class="text-stone-500 hover:text-stone-700 min-h-[44px] py-2"
            aria-label="Back">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
    </button>
    <h2 class="text-xl font-bold text-stone-800">${code ? 'Edit' : 'New'} Discount Code</h2>
</div>

<div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
    <form hx-post="${code ? '/universal/update/' + code.id : '/universal/save'}?domainName=DiscountCode"
          hx-vals='{"template": "discounts/list", "data[codes]": "list:DiscountCode"}'
          hx-target="#content"
          hx-swap="innerHTML"
          class="space-y-4">

        <g:render template="/universal/components/input"
                  model="[name: 'code', label: 'Code', value: code?.code, required: true, placeholder: 'WELCOME10', autocomplete: 'off']"/>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <g:render template="/universal/components/select"
                      model="[name: 'kind', label: 'Type', required: true, selected: code?.kind ?: 'PERCENT',
                              options: [[value: 'PERCENT', label: 'Percent off (%)'], [value: 'FIXED', label: 'Fixed credits off']]]"/>
            <g:render template="/universal/components/input"
                      model="[name: 'value', label: 'Amount', type: 'number', value: code?.value, required: true, placeholder: 'e.g. 10']"/>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <g:render template="/universal/components/input"
                      model="[name: 'startsOn', label: 'Starts on', type: 'date', value: code?.startsOn ? formatDate(date: code.startsOn, format: 'yyyy-MM-dd') : '']"/>
            <g:render template="/universal/components/input"
                      model="[name: 'expiresOn', label: 'Expires on', type: 'date', value: code?.expiresOn ? formatDate(date: code.expiresOn, format: 'yyyy-MM-dd') : '']"/>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <g:render template="/universal/components/input"
                      model="[name: 'usageLimit', label: 'Usage limit (blank = unlimited)', type: 'number', value: code?.usageLimit]"/>
            <g:render template="/universal/components/input"
                      model="[name: 'minAmount', label: 'Min. subtotal (credits)', type: 'number', value: code?.minAmount]"/>
        </div>

        <label class="flex items-center gap-2 min-h-[44px]">
            <g:checkBox name="active" value="${code ? code.active : true}"/>
            <span class="text-sm text-stone-700">Active</span>
        </label>

        <div class="pt-2">
            <g:render template="/universal/components/button" model="[text: code ? 'Save changes' : 'Create code', type: 'submit']"/>
        </div>
    </form>
</div>
