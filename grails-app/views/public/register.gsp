<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Sign up · ${config?.siteTitle ?: 'Korean School House'}</title>
</head>
<body>

<div class="max-w-md mx-auto px-4 py-12">
    <div class="text-center mb-6">
        <div class="text-4xl mb-1">${config?.logoText ?: '한'}</div>
        <h1 class="text-2xl font-bold text-stone-800">Create your account</h1>
        <p class="text-sm text-stone-500 mt-1">Start learning Korean today.</p>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-stone-200 p-6">
        <g:if test="${error}">
            <p class="text-sm text-rose-700 bg-rose-50 border border-rose-200 rounded-lg px-3 py-2 mb-4">${error}</p>
        </g:if>

        <form action="/public/doRegister" method="post" class="space-y-4">
            <div class="grid grid-cols-2 gap-3">
                <div>
                    <label for="firstName" class="block text-sm font-medium text-stone-700 mb-1">First name *</label>
                    <input id="firstName" name="firstName" value="${firstName ?: ''}" required
                           class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 text-base"/>
                </div>
                <div>
                    <label for="lastName" class="block text-sm font-medium text-stone-700 mb-1">Last name</label>
                    <input id="lastName" name="lastName" value="${lastName ?: ''}"
                           class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 text-base"/>
                </div>
            </div>
            <div>
                <label for="username" class="block text-sm font-medium text-stone-700 mb-1">Email *</label>
                <input id="username" type="email" name="username" value="${username ?: ''}" required autocomplete="email"
                       class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 text-base"/>
            </div>
            <div>
                <label for="password" class="block text-sm font-medium text-stone-700 mb-1">Password *</label>
                <input id="password" type="password" name="password" required minlength="8" autocomplete="new-password"
                       class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 text-base"/>
                <p class="text-xs text-stone-400 mt-1">At least 8 characters.</p>
            </div>
            <button type="submit"
                    class="w-full py-3 px-4 rounded-lg text-white font-medium bg-rose-700 hover:bg-rose-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-rose-500 transition-colors text-base">
                Sign up
            </button>
        </form>

        <p class="text-center text-sm text-stone-500 mt-4">
            Already have an account? <a href="/login/auth" class="text-rose-700 font-medium hover:underline">Log in</a>
        </p>
    </div>
</div>

</body>
</html>
