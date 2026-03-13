<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>Korean School House - Login</title>
    <style>
        .korean-bg {
            position: fixed;
            inset: 0;
            overflow: hidden;
            pointer-events: none;
            z-index: 0;
        }
        .korean-bg svg {
            position: absolute;
            opacity: 0.08;
        }
    </style>
</head>
<body>

<div class="min-h-screen flex items-center justify-center p-4 relative" style="background-color: #ffe4c4;">

    <!-- Scattered Korean cultural icons -->
    <div class="korean-bg">

        <!-- Taegeuk (Korean flag symbol) -->
        <svg viewBox="0 0 100 100" style="width:70px; top:5%; left:8%; transform:rotate(-15deg);">
            <circle cx="50" cy="50" r="45" fill="none" stroke="#8B4513" stroke-width="3"/>
            <path d="M50 5 A45 45 0 0 1 50 95 A22.5 22.5 0 0 1 50 50 A22.5 22.5 0 0 0 50 5" fill="#C53030"/>
            <path d="M50 95 A45 45 0 0 1 50 5 A22.5 22.5 0 0 1 50 50 A22.5 22.5 0 0 0 50 95" fill="#2B6CB0"/>
        </svg>

        <!-- Hanbok (traditional dress) -->
        <svg viewBox="0 0 80 100" style="width:55px; top:12%; right:10%; transform:rotate(10deg);">
            <path d="M40 10 C35 10 30 15 30 20 L28 35 L15 38 L20 42 L30 40 L28 70 L20 95 L35 90 L40 70 L45 90 L60 95 L52 70 L50 40 L60 42 L65 38 L52 35 L50 20 C50 15 45 10 40 10Z" fill="#8B4513" stroke="#8B4513" stroke-width="1.5"/>
            <path d="M30 20 Q35 28 40 25 Q45 28 50 20" fill="none" stroke="#8B4513" stroke-width="2"/>
            <circle cx="40" cy="30" r="2" fill="#8B4513"/>
        </svg>

        <!-- Korean fan (부채) -->
        <svg viewBox="0 0 100 80" style="width:65px; top:30%; left:3%; transform:rotate(20deg);">
            <path d="M50 75 L10 15 Q50 -10 90 15 Z" fill="none" stroke="#8B4513" stroke-width="2.5"/>
            <line x1="50" y1="75" x2="30" y2="25" stroke="#8B4513" stroke-width="1.5"/>
            <line x1="50" y1="75" x2="50" y2="15" stroke="#8B4513" stroke-width="1.5"/>
            <line x1="50" y1="75" x2="70" y2="25" stroke="#8B4513" stroke-width="1.5"/>
            <line x1="50" y1="75" x2="20" y2="20" stroke="#8B4513" stroke-width="1"/>
            <line x1="50" y1="75" x2="80" y2="20" stroke="#8B4513" stroke-width="1"/>
        </svg>

        <!-- Mugunghwa (hibiscus / national flower) -->
        <svg viewBox="0 0 100 100" style="width:60px; top:8%; left:45%; transform:rotate(-5deg);">
            <g transform="translate(50,50)">
                <ellipse cx="0" cy="-20" rx="12" ry="22" fill="#8B4513" transform="rotate(0)"/>
                <ellipse cx="0" cy="-20" rx="12" ry="22" fill="#8B4513" transform="rotate(72)"/>
                <ellipse cx="0" cy="-20" rx="12" ry="22" fill="#8B4513" transform="rotate(144)"/>
                <ellipse cx="0" cy="-20" rx="12" ry="22" fill="#8B4513" transform="rotate(216)"/>
                <ellipse cx="0" cy="-20" rx="12" ry="22" fill="#8B4513" transform="rotate(288)"/>
                <circle cx="0" cy="0" r="8" fill="#8B4513"/>
            </g>
        </svg>

        <!-- Korean temple roof (기와) -->
        <svg viewBox="0 0 120 80" style="width:80px; bottom:15%; right:5%; transform:rotate(-8deg);">
            <path d="M10 50 Q30 25 60 20 Q90 25 110 50" fill="none" stroke="#8B4513" stroke-width="3" stroke-linecap="round"/>
            <path d="M5 52 Q30 30 60 25 Q90 30 115 52" fill="none" stroke="#8B4513" stroke-width="2"/>
            <line x1="60" y1="50" x2="60" y2="75" stroke="#8B4513" stroke-width="2.5"/>
            <line x1="35" y1="55" x2="35" y2="75" stroke="#8B4513" stroke-width="2"/>
            <line x1="85" y1="55" x2="85" y2="75" stroke="#8B4513" stroke-width="2"/>
            <line x1="25" y1="75" x2="95" y2="75" stroke="#8B4513" stroke-width="2"/>
        </svg>

        <!-- Janggu (hourglass drum) -->
        <svg viewBox="0 0 100 60" style="width:60px; bottom:25%; left:7%; transform:rotate(15deg);">
            <ellipse cx="20" cy="30" rx="18" ry="25" fill="none" stroke="#8B4513" stroke-width="2.5"/>
            <ellipse cx="80" cy="30" rx="18" ry="25" fill="none" stroke="#8B4513" stroke-width="2.5"/>
            <path d="M20 5 Q50 25 80 5" fill="none" stroke="#8B4513" stroke-width="2"/>
            <path d="M20 55 Q50 35 80 55" fill="none" stroke="#8B4513" stroke-width="2"/>
            <line x1="38" y1="18" x2="62" y2="18" stroke="#8B4513" stroke-width="1.5"/>
            <line x1="38" y1="42" x2="62" y2="42" stroke="#8B4513" stroke-width="1.5"/>
        </svg>

        <!-- Korean lantern -->
        <svg viewBox="0 0 60 100" style="width:40px; top:50%; right:8%; transform:rotate(5deg);">
            <line x1="30" y1="0" x2="30" y2="15" stroke="#8B4513" stroke-width="2"/>
            <rect x="22" y="12" width="16" height="5" rx="2" fill="#8B4513"/>
            <path d="M15 17 Q15 50 20 65 L40 65 Q45 50 45 17 Z" fill="none" stroke="#8B4513" stroke-width="2.5"/>
            <line x1="20" y1="30" x2="40" y2="30" stroke="#8B4513" stroke-width="1"/>
            <line x1="18" y1="45" x2="42" y2="45" stroke="#8B4513" stroke-width="1"/>
            <path d="M22 65 L20 75 M38 65 L40 75" stroke="#8B4513" stroke-width="1.5"/>
            <circle cx="25" cy="80" r="3" fill="#8B4513"/>
            <circle cx="35" cy="80" r="3" fill="#8B4513"/>
        </svg>

        <!-- Rice bowl with chopsticks -->
        <svg viewBox="0 0 100 80" style="width:55px; bottom:8%; left:35%; transform:rotate(-10deg);">
            <path d="M15 45 Q15 70 50 70 Q85 70 85 45" fill="none" stroke="#8B4513" stroke-width="2.5"/>
            <path d="M10 45 L90 45" stroke="#8B4513" stroke-width="2.5" stroke-linecap="round"/>
            <path d="M20 40 Q35 25 50 35 Q65 25 80 40" fill="none" stroke="#8B4513" stroke-width="1.5"/>
            <line x1="65" y1="42" x2="90" y2="10" stroke="#8B4513" stroke-width="2" stroke-linecap="round"/>
            <line x1="70" y1="42" x2="95" y2="12" stroke="#8B4513" stroke-width="2" stroke-linecap="round"/>
        </svg>

        <!-- Hangul ㅎ -->
        <svg viewBox="0 0 60 60" style="width:45px; top:70%; left:15%; transform:rotate(12deg);">
            <circle cx="30" cy="15" r="8" fill="none" stroke="#8B4513" stroke-width="3"/>
            <line x1="12" y1="32" x2="48" y2="32" stroke="#8B4513" stroke-width="3" stroke-linecap="round"/>
            <circle cx="30" cy="47" r="10" fill="none" stroke="#8B4513" stroke-width="3"/>
        </svg>

        <!-- Hangul ㄱ -->
        <svg viewBox="0 0 50 50" style="width:35px; top:25%; right:25%; transform:rotate(-20deg);">
            <path d="M10 10 L40 10 L40 45" fill="none" stroke="#8B4513" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>

        <!-- Korean knot pattern (매듭) -->
        <svg viewBox="0 0 80 80" style="width:50px; bottom:35%; right:22%; transform:rotate(8deg);">
            <circle cx="40" cy="40" r="15" fill="none" stroke="#8B4513" stroke-width="2.5"/>
            <circle cx="40" cy="40" r="25" fill="none" stroke="#8B4513" stroke-width="1.5"/>
            <line x1="40" y1="10" x2="40" y2="0" stroke="#8B4513" stroke-width="2"/>
            <line x1="40" y1="70" x2="40" y2="80" stroke="#8B4513" stroke-width="2"/>
            <line x1="10" y1="40" x2="0" y2="40" stroke="#8B4513" stroke-width="2"/>
            <line x1="70" y1="40" x2="80" y2="40" stroke="#8B4513" stroke-width="2"/>
        </svg>

        <!-- Onggi (Korean pot) -->
        <svg viewBox="0 0 70 80" style="width:45px; top:45%; left:25%; transform:rotate(-7deg);">
            <path d="M20 20 L15 65 Q15 75 35 75 Q55 75 55 65 L50 20" fill="none" stroke="#8B4513" stroke-width="2.5"/>
            <ellipse cx="35" cy="20" rx="17" ry="6" fill="none" stroke="#8B4513" stroke-width="2.5"/>
            <path d="M18 35 Q35 40 52 35" fill="none" stroke="#8B4513" stroke-width="1.5"/>
            <path d="M16 50 Q35 55 54 50" fill="none" stroke="#8B4513" stroke-width="1.5"/>
        </svg>

        <!-- Hangul ㅅ -->
        <svg viewBox="0 0 50 50" style="width:35px; bottom:5%; right:35%; transform:rotate(15deg);">
            <path d="M10 45 L25 10 L40 45" fill="none" stroke="#8B4513" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>

        <!-- Small taegeuk -->
        <svg viewBox="0 0 100 100" style="width:40px; bottom:45%; left:45%; transform:rotate(30deg);">
            <circle cx="50" cy="50" r="45" fill="none" stroke="#8B4513" stroke-width="3"/>
            <path d="M50 5 A45 45 0 0 1 50 95 A22.5 22.5 0 0 1 50 50 A22.5 22.5 0 0 0 50 5" fill="#C53030"/>
            <path d="M50 95 A45 45 0 0 1 50 5 A22.5 22.5 0 0 1 50 50 A22.5 22.5 0 0 0 50 95" fill="#2B6CB0"/>
        </svg>

        <!-- Hangul ㅁ -->
        <svg viewBox="0 0 50 50" style="width:30px; top:80%; right:12%; transform:rotate(-12deg);">
            <rect x="8" y="8" width="34" height="34" rx="2" fill="none" stroke="#8B4513" stroke-width="3.5"/>
        </svg>

    </div>

    <!-- Login card -->
    <div class="w-full max-w-md relative z-10">

        <div class="text-center mb-6">
            <div class="inline-block mb-4">
                <span class="text-6xl font-bold text-rose-700 tracking-tight">한</span>
            </div>
            <h1 class="text-2xl font-bold text-stone-800">Korean School House</h1>
            <p class="text-stone-500 text-sm mt-1">한국어 학교</p>
        </div>

        <div class="bg-white rounded-xl shadow-lg border border-stone-200 p-8">

            <g:if test="${params.login_error}">
                <div class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6 text-sm">
                    Invalid username or password
                </div>
            </g:if>

            <form action="/login/authenticate" method="post" class="space-y-5">
                <div>
                    <label for="username" class="block text-sm font-medium text-stone-700 mb-1">Username</label>
                    <input type="text" id="username" name="username" required
                           placeholder="Enter your username"
                           class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent text-base">
                </div>

                <div>
                    <label for="password" class="block text-sm font-medium text-stone-700 mb-1">Password</label>
                    <input type="password" id="password" name="password" required
                           placeholder="Enter your password"
                           class="block w-full px-4 py-3 border border-stone-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-rose-500 focus:border-transparent text-base">
                </div>

                <button type="submit"
                        class="w-full py-3 px-4 rounded-lg text-white font-medium bg-rose-700 hover:bg-rose-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-rose-500 transition-colors text-base">
                    Sign In
                </button>
            </form>
        </div>

        <div class="mt-6 text-center">
            <p class="text-xs text-stone-400">&copy; ${new Date().year + 1900} Korean School House</p>
        </div>

    </div>
</div>

</body>
</html>
