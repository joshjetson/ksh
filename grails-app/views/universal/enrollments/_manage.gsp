<h2 class="text-xl font-bold text-stone-800 mb-4">Enrollments</h2>

<g:if test="${enrollments}">
    <div class="space-y-3">
        <g:each in="${enrollments}" var="e">
            <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-4">
                <div class="flex items-center justify-between gap-3 mb-3">
                    <div class="min-w-0">
                        <div class="font-medium text-stone-800 truncate">${e.user?.name ?: e.user?.username}</div>
                        <div class="text-xs text-stone-400 truncate">${e.course?.shortTitle} · <ksh:credits amount="${e.totalAmount}"/></div>
                    </div>
                    <div class="flex items-center gap-1 flex-shrink-0">
                        <ksh:statusBadge status="${e.status}"/>
                        <ksh:statusBadge status="${e.paymentStatus}"/>
                    </div>
                </div>
                <form hx-post="/universal/update/${e.id}?domainName=CourseEnrollment"
                      hx-vals='{"template": "enrollments/manage", "data[enrollments]": "list:CourseEnrollment"}'
                      hx-target="#content"
                      hx-swap="innerHTML"
                      class="flex flex-wrap items-end gap-2">
                    <div class="flex-1 min-w-[120px]">
                        <label class="block text-xs text-stone-500 mb-1">Status</label>
                        <select name="status" class="block w-full px-3 py-2 border border-stone-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-rose-500">
                            <g:each in="${['REQUESTED', 'ACTIVE', 'COMPLETED', 'CANCELLED']}" var="s">
                                <option value="${s}" ${e.status == s ? 'selected' : ''}>${s.toLowerCase().capitalize()}</option>
                            </g:each>
                        </select>
                    </div>
                    <div class="flex-1 min-w-[120px]">
                        <label class="block text-xs text-stone-500 mb-1">Payment</label>
                        <select name="paymentStatus" class="block w-full px-3 py-2 border border-stone-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-rose-500">
                            <g:each in="${['UNPAID', 'DEPOSIT', 'PAID']}" var="p">
                                <option value="${p}" ${e.paymentStatus == p ? 'selected' : ''}>${p.toLowerCase().capitalize()}</option>
                            </g:each>
                        </select>
                    </div>
                    <button type="submit" class="px-3 py-2 bg-stone-200 hover:bg-stone-300 text-stone-800 rounded-lg text-sm font-medium min-h-[44px]">Update</button>
                </form>
            </div>
        </g:each>
    </div>
</g:if>
<g:else>
    <g:render template="/universal/components/emptyState" model="[message: 'No enrollments yet']"/>
</g:else>
