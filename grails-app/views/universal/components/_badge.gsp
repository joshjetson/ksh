<div class="flex flex-col items-center text-center">
    <div class="w-14 h-14 rounded-full bg-amber-100 flex items-center justify-center mb-2">
        <g:if test="${badge.icon}">
            <img src="${badge.icon}" alt="${badge.name}" class="w-8 h-8"/>
        </g:if>
        <g:else>
            <span class="text-2xl">&#127942;</span>
        </g:else>
    </div>
    <span class="text-xs font-medium text-stone-700 line-clamp-2">${badge.name}</span>
</div>
