<%-- Styled button emitting the shared .btn-* classes (src/input.css is the single
     source of truth for button styling). Model:
       text    — label
       type    — button|submit (default button)
       variant — primary (default) | cta | secondary | danger
       full    — true for w-full --%>
<%
    def variants = [
        primary  : 'btn-primary',
        cta      : 'btn-cta',
        secondary: 'btn-secondary',
        danger   : 'btn-danger',
    ]
    def cls = variants[variant ?: 'primary'] ?: variants.primary
%>
<button type="${type ?: 'button'}" class="${cls} ${full ? 'w-full' : ''}">${text}</button>
