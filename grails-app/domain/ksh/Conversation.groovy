package ksh

/**
 * A 1:1 message thread between a student and the school's staff (teachers/admins).
 * One conversation per student (found-or-created on first visit). Denormalized
 * lastMessageAt + awaitingReply keep the inbox sort and "needs reply" count O(1)
 * — both are maintained at the single write site, MessagesController.send().
 *
 * Intentionally NOT in the UDA whitelists: messaging needs find-or-create and
 * per-thread access checks that generic CRUD can't express, so it flows through
 * the dedicated MessagesController (see docs/uda.md pillar 13).
 */
class Conversation {

    User student          // the learner who owns this thread
    String subject
    Date lastMessageAt    // newest message time — powers inbox sort
    boolean awaitingReply = false  // true when the last message was from the student

    Date dateCreated
    Date lastUpdated

    static hasMany = [messages: Message]

    static constraints = {
        student nullable: false, unique: true
        subject nullable: true, maxSize: 200
        lastMessageAt nullable: true
    }

    static mapping = {
        table 'conversation'
        student column: 'student_id'
        subject column: 'subject'
        lastMessageAt column: 'last_message_at'
        awaitingReply column: 'awaiting_reply'
        messages sort: 'dateCreated', order: 'asc'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
