<div>
    <label for="${name}" class="block text-sm font-medium text-stone-700 mb-1">${label}</label>
    <select id="${name}"
            name="${name}"
            ${required ? 'required' : ''}
            class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent text-base bg-white">
        <g:if test="${prompt}"><option value="">${prompt}</option></g:if>
        <g:each in="${options}" var="opt">
            <option value="${opt.value}" ${opt.value?.toString() == selected?.toString() ? 'selected' : ''}>${opt.label}</option>
        </g:each>
    </select>
</div>
