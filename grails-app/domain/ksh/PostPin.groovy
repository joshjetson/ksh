package ksh

/**
 * A user's pin of a community post to their OWN wall — "keep this on my wall." Persistent
 * until the user removes it. Owner-scoped (OWNERSHIP_FIELDS → user): created via generic
 * save with `user` force-set, removed via generic delete (owner-checked). Reads go through
 * CommunityService.myWall, which re-checks each pinned post's visibility (so a pin never
 * grants read access to a post the user couldn't otherwise see).
 */
class PostPin {

    User          user
    CommunityPost post

    Date dateCreated
    Date lastUpdated

    static constraints = {
        user nullable: false
        post nullable: false, unique: 'user'   // one pin per (user, post)
    }

    static mapping = {
        table 'post_pin'
    }

    String toString() { "PostPin ${user?.id}->${post?.id}" }
}
