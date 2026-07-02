package ksh

/**
 * A community wall entry — a post or a kudos (celebrating someone, e.g. a classmate's
 * progress). Any signed-in user may post; only managers moderate (hide / edit / delete).
 * Reads NEVER go through the generic whitelist (it can't enforce visibility) — they go
 * through CommunityService, which is why CommunityPost is MANAGER_READ-only on the
 * generic path. `visibility`:
 *   'public' — the community wall (everyone), 'staff' — private to teachers/admin (+ author),
 *   'self'   — private to the author only (managers retain moderation oversight).
 * `hidden` = a moderator pulled it from the wall (kept for search, not deleted). `author`
 * is force-set to the creator (AUTHOR_FIELDS); `anonymous` hides the name in the UI only.
 */
class CommunityPost {

    String  kind = 'post'           // 'post' | 'kudos'
    String  title
    String  body
    String  visibility = 'public'   // 'public' | 'staff' | 'self'
    boolean anonymous = false
    boolean hidden = false
    User    author

    Date dateCreated
    Date lastUpdated

    static constraints = {
        kind       inList: ['post', 'kudos']
        title      nullable: false, blank: false, maxSize: 200
        body       nullable: false, blank: false, maxSize: 3000
        visibility inList: ['public', 'staff', 'self']
        author     nullable: true
    }

    static mapping = {
        table 'community_post'
        body  type: 'text'
    }

    /** Name to show on the wall (respects the anonymous flag). */
    String authorName() { anonymous ? 'Anonymous' : (author?.name ?: 'A member of our community') }

    String toString() { title }
}
