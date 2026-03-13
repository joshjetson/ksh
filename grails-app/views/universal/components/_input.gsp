<div>
    <label for="${name}" class="block text-sm font-medium text-stone-700 mb-1">${label}</label>
    <input type="${type ?: 'text'}"
           id="${name}"
           name="${name}"
           value="${value ?: ''}"
           placeholder="${placeholder ?: ''}"
           ${required ? 'required' : ''}
           class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent text-base"/>
</div>
