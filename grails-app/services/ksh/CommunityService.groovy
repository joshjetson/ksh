package ksh

import grails.gorm.transactions.ReadOnly
import grails.plugin.springsecurity.SpringSecurityService
import java.text.SimpleDateFormat

/**
 * The read side of the community wall — exposed via `service:` data instructions. This is
 * where post VISIBILITY is enforced (public vs staff vs self), which is why CommunityPost is
 * MANAGER_READ-only on the generic path (the generic filter can't enforce visibility).
 * Builds all-scalar card maps (per-card cheer count + the current user's cheer/pin state)
 * so the GSP renders with plain g:each — the MessageService pattern. Writes (create /
 * pin / cheer / hide / delete) go through the generic CRUD engine, not here.
 */
class CommunityService {

    SpringSecurityService springSecurityService
    UniversalDataService universalDataService   // reused for FTS in searchPosts

    private User me() { springSecurityService.currentUser as User }
    private boolean isManager() {
        def r = me()?.authorities*.authority ?: []; r.contains('ROLE_ADMIN') || r.contains('ROLE_TEACHER')
    }
    private boolean isModerator() {
        def r = me()?.authorities*.authority ?: []
        r.contains('ROLE_ADMIN') || r.contains('ROLE_TEACHER') || r.contains('ROLE_MODERATOR')
    }

    /** Can the current user SEE this post? Manager (oversight), a public non-hidden one, or
     *  their own. Pinning does NOT grant visibility — a pinned-then-privated post drops off. */
    private boolean canSee(CommunityPost p, User u) {
        if (!p) return false
        if (isManager()) return true
        if (p.visibility == 'public' && !p.hidden) return true
        return p.author?.id == u?.id
    }

    /** The scalar card map the community/card partial renders. */
    private Map cardOf(CommunityPost p, User u) {
        def myCheer = u ? PostCheer.findByPostAndUser(p, u) : null
        def myPin   = u ? PostPin.findByPostAndUser(p, u) : null
        [id         : p.id, kind: p.kind, title: p.title, body: p.body,
         authorName : p.authorName(), isAnonymous: p.anonymous, createdAt: p.dateCreated,
         cheerCount : PostCheer.countByPost(p) as int,
         iCheered   : (myCheer != null), myCheerId: myCheer?.id,
         iPinned    : (myPin != null),   myPinId: myPin?.id,
         isMine     : (u != null && p.author?.id == u.id),
         visibility : p.visibility, hidden: p.hidden, canModerate: isModerator()]
    }

    /** The general Community Wall — public (and, for staff, staff-only) posts, not hidden,
     *  this week, newest first. Private 'self' posts never appear here. */
    @ReadOnly
    Map generalWall(Map params) {
        User u = me()
        Date weekStart = new Date().clearTime() - 6   // last 7 days, inclusive
        List<String> vis = isManager() ? ['public', 'staff'] : ['public']
        def posts = CommunityPost.createCriteria().list {
            inList('visibility', vis)
            eq('hidden', false)
            ge('dateCreated', weekStart)
            order('dateCreated', 'desc')
        }
        SimpleDateFormat f = new SimpleDateFormat('MMM d')
        [weekLabel : "${f.format(weekStart)} – ${f.format(new Date().clearTime())}",
         postCount : posts.count { it.kind == 'post' },
         kudosCount: posts.count { it.kind == 'kudos' },
         cards     : posts.collect { cardOf(it, u) }]
    }

    /** The current user's own wall — their posts (any visibility) + posts they've pinned
     *  ("cheering for this"). Pinned re-checks visibility so it never leaks. */
    @ReadOnly
    Map myWall(Map params) {
        User u = me()
        if (!u) return [mine: [], pinned: []]
        def mine = CommunityPost.findAllByAuthor(u, [sort: 'dateCreated', order: 'desc'])
        def pinned = PostPin.findAllByUser(u, [sort: 'dateCreated', order: 'desc'])
                            .collect { it.post }
                            .findAll { it != null && it.author?.id != u.id && canSee(it, u) }
        [mine: mine.collect { cardOf(it, u) }, pinned: pinned.collect { cardOf(it, u) }]
    }

    /** Moderator search across ALL posts ever (any visibility, including hidden). Param `q`
     *  → FTS; blank → most recent. Managers only. */
    @ReadOnly
    Map searchPosts(Map params) {
        if (!isManager()) return [q: null, cards: []]
        User u = me()
        String q = params?.q?.toString()
        def results = q?.trim() ?
            universalDataService.ftsSearch(CommunityPost, 'search_fts', q, [max: 50]) :
            CommunityPost.list(max: 50, sort: 'dateCreated', order: 'desc')
        [q: q, cards: results.collect { cardOf(it, u) }]
    }

    /** One card, re-fetched after an action (cheer / pin / hide) so the wall can swap just
     *  that card. Access-checked — returns null (→ the card disappears) if not visible/deleted. */
    @ReadOnly
    Map postCard(Map params) {
        User u = me()
        Long id = params?.cardId ? (params.cardId as Long) : null
        CommunityPost p = id ? CommunityPost.get(id) : null
        (p && canSee(p, u)) ? cardOf(p, u) : null
    }
}
