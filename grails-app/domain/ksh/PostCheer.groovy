package ksh

/**
 * A cheer (응원) — a user letting the author know they're rooting for them. One per
 * (user, post); the card shows the count and the author feels the support. Owner-scoped
 * (OWNERSHIP_FIELDS → user): toggled on via generic save (user force-set), off via
 * generic delete (owner-checked).
 */
class PostCheer {

    User          user
    CommunityPost post

    Date dateCreated
    Date lastUpdated

    static constraints = {
        user nullable: false
        post nullable: false, unique: 'user'   // one cheer per (user, post)
    }

    static mapping = {
        table 'post_cheer'
    }

    String toString() { "PostCheer ${user?.id}->${post?.id}" }
}
