<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>${config?.siteTitle ?: 'Korean School House'}</title>
</head>
<body>

<!-- Hero -->
<div class="bg-gradient-to-br from-rose-600 to-rose-800 text-white">
    <div class="max-w-5xl mx-auto px-4 py-16 text-center">
        <div class="text-5xl mb-3">${config?.logoText ?: '한'}</div>
        <h1 class="text-3xl md:text-4xl font-bold">${config?.siteTitle ?: 'Korean School House'}</h1>
        <p class="text-rose-100 mt-2 text-lg">${config?.siteSubtitle ?: '한국어 학교'}</p>
        <p class="mt-4 text-rose-50 max-w-xl mx-auto">Learn Korean at your own pace with interactive, self-paced courses.</p>
        <div class="mt-7 flex gap-3 justify-center flex-wrap">
            <a href="/public/register" class="px-6 py-3 bg-white text-rose-700 rounded-lg font-medium hover:bg-rose-50 transition-colors min-h-[44px] inline-flex items-center">Get started — it's free</a>
            <a href="/login/auth" class="px-6 py-3 bg-rose-900/40 text-white rounded-lg font-medium hover:bg-rose-900/60 transition-colors min-h-[44px] inline-flex items-center">Log in</a>
        </div>
    </div>
</div>

<!-- Catalog -->
<div class="max-w-5xl mx-auto px-4 py-12">
    <h2 class="text-xl font-bold text-stone-800 mb-6 text-center">Our courses</h2>
    <g:if test="${courses}">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <g:each in="${courses}" var="c">
                <div class="bg-white rounded-xl shadow-sm border border-stone-200 overflow-hidden">
                    <div class="aspect-video bg-stone-100 flex items-center justify-center">
                        <g:if test="${c.thumbnailLarge}">
                            <img src="${c.thumbnailLarge}" alt="${c.shortTitle}" class="w-full h-full object-cover"/>
                        </g:if>
                        <g:else><span class="text-4xl">&#128218;</span></g:else>
                    </div>
                    <div class="p-4">
                        <h3 class="font-semibold text-stone-800">${c.shortTitle}</h3>
                        <p class="text-sm text-stone-500 line-clamp-2 mt-1">${c.shortDescription ?: ''}</p>
                        <div class="mt-2 text-sm font-bold ${c.costKCredits > 0 ? 'text-rose-700' : 'text-green-600'}">
                            ${c.costKCredits > 0 ? c.costKCredits + ' K-Credits' : 'Free'}
                        </div>
                    </div>
                </div>
            </g:each>
        </div>
    </g:if>
    <g:else>
        <p class="text-center text-stone-400">Courses coming soon.</p>
    </g:else>

    <div class="text-center mt-10">
        <a href="/public/register" class="px-6 py-3 bg-rose-700 hover:bg-rose-800 text-white rounded-lg font-medium inline-flex items-center min-h-[44px] transition-colors">Create your free account</a>
    </div>
</div>

<footer class="text-center text-xs text-stone-400 py-6">
    &copy; ${new Date().format('yyyy')} ${config?.siteTitle ?: 'Korean School House'}
</footer>

</body>
</html>
