package ksh

/**
 * A single message within a Conversation. The sender is always force-set
 * server-side in MessagesController.send() (never bound from the request),
 * so a client can't spoof who a message is from. The parent conversation's
 * lastMessageAt / awaitingReply are updated by the controller right after save
 * — kept off an afterInsert hook to avoid mutating during the flush.
 */
class Message {

    Conversation conversation
    User sender
    String body
    Date readAt   // reserved for future read-receipt tracking

    Date dateCreated

    static belongsTo = [conversation: Conversation]

    static constraints = {
        conversation nullable: false
        sender nullable: false
        body nullable: false, blank: false, maxSize: 4000
        readAt nullable: true
    }

    static mapping = {
        table 'message'
        conversation column: 'conversation_id'
        sender column: 'sender_id'
        body type: 'text'
        readAt column: 'read_at'
        dateCreated column: 'date_created'
    }
}
