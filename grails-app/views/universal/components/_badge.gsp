<div class="flex flex-col items-center text-center">
    <div class="w-14 h-14 rounded-full bg-amber-100 flex items-center justify-center mb-2 overflow-hidden">
        <g:if test="${badge.hasImage()}">
            <img src="/badge/image/${badge.id}" alt="${badge.name}" class="w-full h-full object-cover"/>
        </g:if>
        <g:elseif test="${badge.icon && (badge.icon.startsWith('/') || badge.icon.startsWith('http'))}">
            <img src="${badge.icon}" alt="${badge.name}" class="w-8 h-8"/>
        </g:elseif>
        <g:elseif test="${badge.icon}">
            <span class="text-2xl">${badge.icon}</span>
        </g:elseif>
        <g:else>
            <span class="text-2xl">&#127942;</span>
        </g:else>
    </div>
    <span class="text-xs font-medium text-stone-700 line-clamp-2">${badge.name}</span>
</div>
