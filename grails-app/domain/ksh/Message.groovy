package ksh

import org.springframework.security.core.context.SecurityContextHolder

/**
 * A chat message in a Channel. Posted via generic save (author force-set by
 * OWNERSHIP_FIELDS → anti-spoof); read only through MessageService.channelView
 * (access-checked). The channel validator below enforces POST access: a staff-only
 * channel rejects a non-manager poster.
 *
 * It reads the ambient security context (not an injected service) because domain
 * instances created via `newInstance()` in the generic save path are NOT autowired —
 * SecurityContextHolder is a thread-local and works regardless.
 */
class Message {

    Channel channel
    User    author
    String  body

    Date dateCreated
    Date lastUpdated

    static constraints = {
        body    nullable: false, blank: false, maxSize: 4000
        author  nullable: true
        channel nullable: false, validator: { Channel ch, Message msg ->
            if (ch?.visibility == 'staff' && !Message.posterIsManager()) return 'channel.staffOnly'
            // Muted or banned in this channel → can't post.
            Long uid = Message.posterId()
            if (uid && ch) {
                def cm = ChannelMembership.findByChannelAndUser(ch, User.load(uid))
                if (cm && cm.status in ['muted', 'banned']) return 'channel.muted'
            }
        }
    }

    static mapping = {
        table 'message'
        body  type: 'text'
    }

    /** True if the current request's user is teacher/admin (for staff-channel post gating). */
    static boolean posterIsManager() {
        def roles = SecurityContextHolder.context?.authentication?.authorities*.authority ?: []
        roles.contains('ROLE_ADMIN') || roles.contains('ROLE_TEACHER')
    }

    /** Current request's user id (from the security context), or null if not authed. */
    static Long posterId() {
        def p = SecurityContextHolder.context?.authentication?.principal
        (p != null && p.hasProperty('id')) ? (p.id as Long) : null
    }

    String toString() { "Message ${id}" }
}
