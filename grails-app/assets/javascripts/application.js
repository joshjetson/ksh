// Minimal application.js for HTMX-based Grails application
//
// HTMX loaded locally from assets
//
//= require htmx.min
//= require sse
//= require_self

// HTMX Configuration
document.addEventListener('DOMContentLoaded', function() {
    // Configure HTMX loading indicator
    document.body.addEventListener('htmx:beforeRequest', function() {
        const indicator = document.getElementById('htmx-indicator');
        if (indicator) {
            indicator.style.opacity = '1';
        }
    });

    document.body.addEventListener('htmx:afterRequest', function() {
        const indicator = document.getElementById('htmx-indicator');
        if (indicator) {
            indicator.style.opacity = '0';
        }
    });

    // Success toast notification handler
    document.body.addEventListener('showSuccessToast', function(evt) {
        const data = evt.detail;
        showSuccessToast(data.message || 'Operation completed successfully');
    });
});

// Success toast notification function
function showSuccessToast(message) {
    let toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.id = 'toast-container';
        toastContainer.className = 'fixed top-4 right-4 z-50 space-y-2';
        document.body.appendChild(toastContainer);
    }

    const toast = document.createElement('div');
    toast.className = 'bg-green-500 text-white px-4 py-3 rounded-lg shadow-lg flex items-center space-x-2 transform transition-all duration-300 translate-x-full opacity-0';
    toast.innerHTML = `
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
        </svg>
        <span>${message}</span>
    `;

    toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.classList.remove('translate-x-full', 'opacity-0');
    }, 10);

    setTimeout(() => {
        toast.classList.add('translate-x-full', 'opacity-0');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}
