package ksh

/**
 * Per-(channel, user) moderation state. For role-based channels (school/staff) a row
 * exists ONLY to override the default: 'muted' (can read, can't post) or 'banned'
 * (can't access). No row = normal access. Set by moderators (ROLE_MODERATOR — the
 * community_leader authority — or admin) via generic CRUD; enforced in Message (post)
 * and MessageService.channelView (read).
 */
class ChannelMembership {

    Channel channel
    User    user
    String  status = 'active'   // 'active' | 'muted' | 'banned'

    Date dateCreated
    Date lastUpdated

    static constraints = {
        channel nullable: false, unique: 'user'   // one row per (channel, user)
        status  inList: ['active', 'muted', 'banned']
        user    nullable: false, validator: { User u, ChannelMembership m ->
            // A moderator may not mute/ban an admin.
            if (m.status in ['muted', 'banned'] && (u?.authorities*.authority ?: []).contains('ROLE_ADMIN')) {
                return 'cannotModerateAdmin'
            }
        }
    }

    static mapping = {
        table 'channel_membership'
    }

    String toString() { "${user} in ${channel}: ${status}" }
}
