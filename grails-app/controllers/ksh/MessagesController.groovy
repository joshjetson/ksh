package ksh

import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.springsecurity.annotation.Secured

/**
 * Dedicated controller for the 1:1 student↔staff messaging threads.
 *
 * This is a sanctioned non-UDA controller (docs/uda.md pillar 13): messaging needs
 * find-or-create of a student's thread and per-thread access checks that the generic
 * /universal CRUD endpoint can't express. It still leans on UniversalDataService for
 * the actual persistence (so writes flush under the service transaction) and on
 * UniversalController.broadcastEvent for live SSE updates.
 *
 * Staff = ROLE_ADMIN or ROLE_TEACHER (can see every thread). Student = the thread owner.
 */
@Secured(['ROLE_USER', 'ROLE_ADMIN', 'ROLE_TEACHER'])
class MessagesController {

    SpringSecurityService springSecurityService
    UniversalDataService universalDataService

    static allowedMethods = [send: 'POST']

    def index() {
        redirect uri: '/'
    }

    /**
     * GET /messages/inbox — staff only. Every thread, newest activity first.
     */
    def inbox() {
        User me = currentUser()
        if (!isStaff(me)) {
            render status: 403, text: 'Staff only'
            return
        }
        def conversations = Conversation.createCriteria().list {
            order('lastMessageAt', 'desc')
            order('dateCreated', 'desc')
        }
        render template: '/messages/inbox', model: [conversations: conversations]
    }

    /**
     * GET /messages/myThread — a student's own thread (created on first visit).
     */
    def myThread() {
        User me = currentUser()
        Conversation c = findOrCreateForStudent(me)
        renderThread(c, me)
    }

    /**
     * GET /messages/thread?conversationId=X — open a specific thread (staff or owner).
     */
    def thread() {
        User me = currentUser()
        Conversation c = Conversation.get(params.long('conversationId'))
        if (!c || !canAccess(c, me)) {
            render status: 403, text: 'Not your conversation'
            return
        }
        // Opening a thread as staff clears the "awaiting reply" flag is NOT done here —
        // the flag flips when staff actually replies (see send). Opening is read-only.
        renderThread(c, me)
    }

    /**
     * GET /messages/bubbles?conversationId=X — just the message list (SSE reload target).
     */
    def bubbles() {
        User me = currentUser()
        Conversation c = Conversation.get(params.long('conversationId'))
        if (!c || !canAccess(c, me)) {
            render status: 403, text: 'Not your conversation'
            return
        }
        render template: '/messages/bubbles', model: [messages: c.messages as List, me: me]
    }

    /**
     * POST /messages/send — append a message (staff or owner), then broadcast.
     */
    def send() {
        User me = currentUser()
        Conversation c = Conversation.get(params.long('conversationId'))
        if (!c || !canAccess(c, me)) {
            render status: 403, text: 'Not your conversation'
            return
        }
        String body = params.body?.toString()?.trim()
        if (!body) {
            render status: 422, text: 'Message cannot be empty'
            return
        }

        // sender is force-set server-side — never bound from the request.
        universalDataService.save(Message, [conversation: [id: c.id], sender: [id: me.id], body: body])

        // Maintain denormalized fields at this single write site:
        //  - lastMessageAt powers inbox sort
        //  - awaitingReply is true iff the student sent the last message (staff owes a reply)
        universalDataService.update(Conversation, c.id, [lastMessageAt: new Date(), awaitingReply: !isStaff(me)])

        // Live-update any other open thread/inbox via SSE.
        UniversalController.broadcastEvent('Message-create', "conv:${c.id}")

        // Immediate feedback for the sender: return the refreshed bubble list.
        c.refresh()
        render template: '/messages/bubbles', model: [messages: c.messages as List, me: me]
    }

    // ==================== Private helpers ====================

    private User currentUser() {
        springSecurityService.currentUser as User
    }

    private boolean isStaff(User u) {
        def roles = u?.authorities*.authority ?: []
        return ('ROLE_ADMIN' in roles) || ('ROLE_TEACHER' in roles)
    }

    private boolean canAccess(Conversation c, User u) {
        return isStaff(u) || c.student?.id == u?.id
    }

    private Conversation findOrCreateForStudent(User student) {
        Conversation c = Conversation.findByStudent(student)
        if (!c) {
            c = universalDataService.save(Conversation, [student: [id: student.id]]) as Conversation
        }
        return c
    }

    private void renderThread(Conversation c, User me) {
        render template: '/messages/thread',
            model: [conversation: c, messages: c.messages as List, me: me, staff: isStaff(me)]
    }
}
