package ksh

import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityService

/**
 * Read side of messaging (+ the one find-or-create-DM write) — exposed via `service:`
 * data instructions. This is where channel/message/PM ACCESS is enforced, which is why
 * Channel/Message are deliberately NOT in the generic read whitelist. Returns all-scalar
 * maps so the GSP renders with plain g:each.
 *
 * PM rules: teachers/admin ↔ anyone always. Student ↔ student only if BOTH are
 * `discoverable` and neither has blocked the other. A personal block also hides
 * messages both ways in shared channels.
 */
class MessageService {

    SpringSecurityService springSecurityService
    UniversalDataService universalDataService   // reused for the generic FTS primitive

    private User me() { springSecurityService.currentUser as User }
    private boolean isManager() {
        def r = me()?.authorities*.authority ?: []; r.contains('ROLE_ADMIN') || r.contains('ROLE_TEACHER')
    }
    private boolean isModerator() {
        def r = me()?.authorities*.authority ?: []; r.contains('ROLE_ADMIN') || r.contains('ROLE_MODERATOR')
    }
    private List usersWithAnyRole(List<String> auths) {
        def roles = Role.findAllByAuthorityInList(auths)
        roles ? (UserRole.findAllByRoleInList(roles)*.user).unique { it.id } : []
    }
    private boolean isDiscoverable(User u) { u && UserSetting.findByUser(u)?.discoverable }

    /** User ids the viewer shouldn't see, in EITHER direction (block is symmetric). */
    private Set blockedIds(User u) {
        ((UserBlock.findAllByOwner(u)*.blocked*.id ?: []) + (UserBlock.findAllByBlocked(u)*.owner*.id ?: [])) as Set
    }

    /** May `a` and `b` exchange PMs? Teacher/admin involved → always; else both
     *  discoverable and neither blocks the other. */
    private boolean canPm(User a, User b) {
        if (!a || !b || a.id == b.id) return false
        def ra = a.authorities*.authority, rb = b.authorities*.authority
        if (ra.contains('ROLE_TEACHER') || ra.contains('ROLE_ADMIN') || rb.contains('ROLE_TEACHER') || rb.contains('ROLE_ADMIN')) return true
        if (!isDiscoverable(a) || !isDiscoverable(b)) return false
        !(UserBlock.findByOwnerAndBlocked(a, b) || UserBlock.findByOwnerAndBlocked(b, a))
    }

    // ── Channel list / view ───────────────────────────────────────────────────

    /** School channels the user may see (+ staff channels for managers). */
    @ReadOnly
    Map channelList(Map params) {
        boolean mgr = isManager()
        def channels = Channel.createCriteria().list {
            ne('visibility', 'dm'); if (!mgr) eq('visibility', 'school')
            order('sortOrder', 'asc'); order('name', 'asc')
        }
        [canManage: mgr,
         channels : channels.collect { [id: it.id, name: it.name, description: it.description, visibility: it.visibility] }]
    }

    /** One channel (or DM) + its messages, or null if the user may not access it. */
    @ReadOnly
    Map channelView(Map params) {
        Long id = params?.channelId ? (params.channelId as Long) : null
        return viewFor(id ? Channel.get(id) : null, me())
    }

    private Map viewFor(Channel ch, User me) {
        if (!ch || !me) return null
        def myCm = ChannelMembership.findByChannelAndUser(ch, me)
        if (ch.visibility == 'staff' && !isManager()) return null
        if (ch.visibility == 'dm' && !myCm) return null         // not a participant
        if (myCm?.status == 'banned') return null

        String name = ch.name
        User other = null
        if (ch.visibility == 'dm') {
            other = ChannelMembership.findAllByChannel(ch).find { it.user.id != me.id }?.user
            name = other ? (other.name ?: other.username) : 'Direct message'
        }

        Set blocked = (ch.visibility == 'dm') ? ([] as Set) : blockedIds(me)
        def msgs = Message.createCriteria().list { eq('channel', ch); order('dateCreated', 'asc') }
                          .findAll { !(it.author?.id in blocked) }

        boolean canPost = (ch.visibility == 'dm')
            ? (other != null && canPm(me, other))
            : ((ch.visibility != 'staff' || isManager()) && !(myCm?.status in ['muted', 'banned']))

        [channel : [id: ch.id, name: name, description: ch.description, visibility: ch.visibility, isDm: (ch.visibility == 'dm')],
         meId    : me.id,
         canModerate: (ch.visibility != 'dm') && isModerator(),
         canPost : canPost,
         muted   : (myCm?.status == 'muted'),
         messages: msgs.collect { [id: it.id, body: it.body, time: it.dateCreated, authorId: it.author?.id,
                                   authorName: (it.author?.name ?: it.author?.username ?: 'Someone')] }]
    }

    // ── Direct messages ─────────────────────────────────────────────────────────

    /** The current user's DM threads (the "pm's" list), newest first. */
    @ReadOnly
    Map dmList(Map params) {
        def me = me()
        if (!me) return [threads: []]
        def myDms = ChannelMembership.createCriteria().list { eq('user', me); channel { eq('visibility', 'dm') } }*.channel
        def threads = myDms.collect { ch ->
            def other = ChannelMembership.findAllByChannel(ch).find { it.user.id != me.id }?.user
            def last = Message.createCriteria().get { eq('channel', ch); order('dateCreated', 'desc'); maxResults(1) }
            [id: ch.id, name: (other ? (other.name ?: other.username) : 'Direct message'),
             otherId: other?.id, last: last?.body, time: last?.dateCreated]
        }.findAll { it.otherId != null }.sort { a, b -> (b.time ?: new Date(0)) <=> (a.time ?: new Date(0)) }
        [threads: threads]
    }

    /** Find-or-create the DM between the current user and target, then return its view.
     *  Null if the PM isn't allowed. (The one write here — access-checked.) */
    @Transactional
    Map startDm(Map params) {
        def me = me()
        Long tid = params?.targetUserId ? (params.targetUserId as Long) : null
        User t = tid ? User.get(tid) : null
        if (!me || !t || me.id == t.id || !canPm(me, t)) return null
        def myDms = ChannelMembership.createCriteria().list { eq('user', me); channel { eq('visibility', 'dm') } }*.channel
        Channel dm = myDms.find { ChannelMembership.findByChannelAndUser(it, t) }
        if (!dm) {
            // flush:true — OSIV runs the session in MANUAL flush, so without it the
            // membership query in viewFor() below wouldn't see these new rows.
            dm = new Channel(name: 'Direct message', visibility: 'dm', sortOrder: 0).save(flush: true, failOnError: true)
            new ChannelMembership(channel: dm, user: me, status: 'active').save(failOnError: true)
            new ChannelMembership(channel: dm, user: t,  status: 'active').save(flush: true, failOnError: true)
        }
        viewFor(dm, me)
    }

    // ── Search (access-controlled FTS) ───────────────────────────────────────────
    // Message and User are deliberately OUTSIDE the generic read whitelist for search, so
    // they can't be searched via the `fts:` data instruction without an access gate. These
    // methods reuse the generic FTS primitive (universalDataService.ftsSearch → the
    // GIN-indexed search_fts column) and then apply the SAME visibility rules as
    // channelView/canPm, so search can never surface a message or person the caller
    // couldn't otherwise see.

    /** Full-text search the current user's visible messages. Param `q`. Returns scalar maps
     *  (channel context included so a hit can deep-link into the conversation). */
    @ReadOnly
    Map searchMessages(Map params) {
        def me = me()
        String q = params?.q?.toString()
        if (!me || !q?.trim()) return [q: q, results: []]

        boolean mgr = isManager()
        Set blocked = blockedIds(me)
        def hits = universalDataService.ftsSearch(Message, 'search_fts', q, [max: 50])

        def results = hits.findAll { Message m ->
            Channel ch = m.channel
            if (!ch || m.author?.id in blocked) return false
            if (ch.visibility == 'staff') return mgr
            if (ch.visibility == 'dm')    return ChannelMembership.findByChannelAndUser(ch, me) != null
            // school channel — visible unless the viewer is banned from it
            ChannelMembership.findByChannelAndUser(ch, me)?.status != 'banned'
        }.collect { Message m ->
            Channel ch = m.channel
            String chName = ch.name
            if (ch.visibility == 'dm') {
                def other = ChannelMembership.findAllByChannel(ch).find { it.user.id != me.id }?.user
                chName = other ? (other.name ?: other.username) : 'Direct message'
            }
            [id: m.id, body: m.body, time: m.dateCreated, channelId: ch.id, channelName: chName,
             isDm: (ch.visibility == 'dm'),
             authorName: (m.author?.name ?: m.author?.username ?: 'Someone')]
        }
        [q: q, results: results]
    }

    /** Full-text search people the current user is allowed to message. Param `q`. Honours
     *  the same canPm rules as the people strip (discoverable + not blocked, or staff). */
    @ReadOnly
    Map searchPeople(Map params) {
        def me = me()
        String q = params?.q?.toString()
        if (!me || !q?.trim()) return [q: q, people: []]

        Set blocked = blockedIds(me)
        def hits = universalDataService.ftsSearch(User, 'search_fts', q, [max: 50])
        def people = hits.findAll { User u -> u.id != me.id && !(u.id in blocked) && u.enabled && canPm(me, u) }
                         .collect { User u -> [userId: u.id, name: (u.name ?: u.username)] }
        [q: q, people: people]
    }

    // ── Member strips ───────────────────────────────────────────────────────────

    /** Moderator strip — all channel members + their moderation status. Mods/admins only. */
    @ReadOnly
    Map channelMembers(Map params) {
        Long id = params?.channelId ? (params.channelId as Long) : null
        Channel ch = id ? Channel.get(id) : null
        if (!ch || ch.visibility == 'dm' || !isModerator()) return null
        def eligible = (ch.visibility == 'staff') ? usersWithAnyRole(['ROLE_TEACHER', 'ROLE_ADMIN', 'ROLE_MODERATOR']) : User.findAllByEnabled(true)
        def cmByUser = [:]; ChannelMembership.findAllByChannel(ch).each { cmByUser[it.user.id] = it }
        def members = eligible.collect { u ->
            def cm = cmByUser[u.id]
            [userId: u.id, name: (u.name ?: u.username), status: (cm?.status ?: 'active'),
             membershipId: cm?.id, isAdmin: (u.authorities*.authority).contains('ROLE_ADMIN')]
        }.sort { it.name }
        [channel: [id: ch.id, name: ch.name], count: members.size(), members: members]
    }

    /** One member's moderation actions (for the action sheet). Moderators/admins only. */
    @ReadOnly
    Map memberActions(Map params) {
        Long cid = params?.channelId ? (params.channelId as Long) : null
        Long uid = params?.userId ? (params.userId as Long) : null
        Channel ch = cid ? Channel.get(cid) : null
        User u = uid ? User.get(uid) : null
        if (!ch || !u || !isModerator()) return null
        def cm = ChannelMembership.findByChannelAndUser(ch, u)
        [channelId: ch.id, userId: u.id, name: (u.name ?: u.username), status: (cm?.status ?: 'active'),
         membershipId: cm?.id, isAdmin: (u.authorities*.authority).contains('ROLE_ADMIN'),
         canMessage: (me() && u.id != me().id && canPm(me(), u))]
    }

    /** Non-moderator "people you can message" strip — discoverable members minus blocked.
     *  Count is the channel's total size; the scrollable list is the PM-able subset. */
    @ReadOnly
    Map channelPeople(Map params) {
        Long id = params?.channelId ? (params.channelId as Long) : null
        Channel ch = id ? Channel.get(id) : null
        def me = me()
        if (!ch || !me || ch.visibility == 'dm') return null
        def eligible = (ch.visibility == 'staff') ? usersWithAnyRole(['ROLE_TEACHER', 'ROLE_ADMIN', 'ROLE_MODERATOR']) : User.findAllByEnabled(true)
        def blocked = blockedIds(me)
        def people = eligible.findAll { u -> u.id != me.id && !(u.id in blocked) && canPm(me, u) }
                             .collect { [userId: it.id, name: (it.name ?: it.username)] }.sort { it.name }
        [channelId: ch.id, count: eligible.size(), people: people]
    }

    /** One person's PM/block actions (for the people sheet). */
    @ReadOnly
    Map personActions(Map params) {
        Long uid = params?.userId ? (params.userId as Long) : null
        User u = uid ? User.get(uid) : null
        def me = me()
        if (!u || !me || u.id == me.id) return null
        def block = UserBlock.findByOwnerAndBlocked(me, u)
        boolean targetIsStaff = (u.authorities*.authority).any { it in ['ROLE_TEACHER', 'ROLE_ADMIN'] }
        [userId: u.id, name: (u.name ?: u.username), blockId: block?.id,
         blocked: (block != null), canMessage: canPm(me, u), canBlock: !targetIsStaff]
    }
}
