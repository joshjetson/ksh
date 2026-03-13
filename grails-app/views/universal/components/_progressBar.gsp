<%
    def p = (progress ?: 0) as Integer
    def color = p == 100 ? 'bg-green-500' : 'bg-rose-600'
%>
<div class="w-full">
    <div class="flex justify-between items-center mb-1">
        <span class="text-xs text-stone-500">${label ?: 'Progress'}</span>
        <span class="text-xs font-medium text-stone-700">${p}%</span>
    </div>
    <div class="w-full bg-stone-200 rounded-full h-2">
        <div class="${color} h-2 rounded-full transition-all duration-300" style="width: ${p}%"></div>
    </div>
</div>
