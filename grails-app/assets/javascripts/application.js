// Minimal application.js for the HTMX-based Grails PWA (UDA).
//
//= require htmx.min
//= require sse
//= require_self

document.addEventListener('DOMContentLoaded', function () {
    // Inject CSRF token into every HTMX request.
    document.body.addEventListener('htmx:configRequest', function (evt) {
        var csrfToken = document.querySelector('meta[name="_csrf"]');
        var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
        if (csrfToken && csrfHeader && csrfToken.content && csrfHeader.content) {
            evt.detail.headers[csrfHeader.content] = csrfToken.content;
        }
    });

    // Session-expiry guard: if an HTMX request was transparently redirected to login
    // (session lost), don't swap the login HTML into #content — do a full navigation.
    document.body.addEventListener('htmx:beforeSwap', function (evt) {
        var url = (evt.detail.xhr && evt.detail.xhr.responseURL) || '';
        if (url.indexOf('/login/auth') !== -1) {
            evt.detail.shouldSwap = false;
            window.location.href = '/login/auth';
        }
    });

    // Top-bar loading indicator.
    document.body.addEventListener('htmx:beforeRequest', function () {
        var i = document.getElementById('htmx-indicator'); if (i) i.style.opacity = '1';
    });
    document.body.addEventListener('htmx:afterRequest', function () {
        var i = document.getElementById('htmx-indicator'); if (i) i.style.opacity = '0';
    });

    // Success toast (HX-Trigger: showSuccessToast).
    document.body.addEventListener('showSuccessToast', function (evt) {
        showSuccessToast((evt.detail && evt.detail.message) || 'Done');
    });

    // Chat composer: after a message posts, clear + refocus the box and refetch the
    // bubbles immediately for the sender (the SSE 'Message-create' broadcast updates
    // everyone else). Uses htmx.trigger directly — reliable, no body-event wiring.
    document.body.addEventListener('htmx:afterRequest', function (evt) {
        var f = evt.target.closest && evt.target.closest('form[data-chat-composer]');
        if (f && evt.detail.successful) {
            f.reset();
            var box = f.querySelector('input[name="body"], textarea[name="body"]');
            if (box) box.focus();
            var bubbles = document.getElementById('msg-bubbles');
            if (bubbles && window.htmx) htmx.trigger(bubbles, 'refreshmsgs');
        }
    });
});

// Channel member/person strip: open/close the shared action bottom-sheet (presentation
// only — content is server-rendered into #action-sheet-body, actions are declarative).
function openActionSheet() {
    var o = document.getElementById('action-sheet-overlay'), s = document.getElementById('action-sheet');
    if (!o || !s) return;
    o.classList.remove('hidden'); s.classList.remove('hidden');
    requestAnimationFrame(function () { s.classList.remove('translate-y-full'); });
}
function closeActionSheet() {
    var o = document.getElementById('action-sheet-overlay'), s = document.getElementById('action-sheet');
    if (!s || s.classList.contains('hidden')) return;
    s.classList.add('translate-y-full');
    setTimeout(function () { o.classList.add('hidden'); s.classList.add('hidden'); }, 250);
}

// Batch create/upsert → POST /universal/saveBatch, then swap #content with the
// re-rendered view. `records` is an array of plain objects; `rerender` is the showView
// payload (template + data[...] instructions) for what to render back.
function kshSaveBatch(domainName, naturalKey, records, rerender) {
    htmx.ajax('POST', '/universal/saveBatch', {
        target: '#content', swap: 'innerHTML',
        values: Object.assign({
            domainName: domainName, naturalKey: naturalKey, records: JSON.stringify(records)
        }, rerender || {})
    });
}

function showSuccessToast(message) {
    var container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'fixed top-4 right-4 z-50 space-y-2';
        document.body.appendChild(container);
    }
    var toast = document.createElement('div');
    toast.className = 'bg-rose-700 text-white px-4 py-3 rounded-xl shadow-lg flex items-center gap-2 transition-all duration-300 translate-x-full opacity-0';
    toast.innerHTML = '<svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">' +
        '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/></svg>' +
        '<span>' + message + '</span>';
    container.appendChild(toast);
    setTimeout(function () { toast.classList.remove('translate-x-full', 'opacity-0'); }, 10);
    setTimeout(function () {
        toast.classList.add('translate-x-full', 'opacity-0');
        setTimeout(function () { toast.remove(); }, 320);
    }, 3200);
}
