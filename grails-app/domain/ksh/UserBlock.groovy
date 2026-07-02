package ksh

/**
 * "Don't let us interact" — `owner` has blocked `blocked`. Owner-owned
 * (OWNERSHIP_FIELDS + READ_SCOPE_FIELDS → owner): created/removed only by the owner
 * via generic CRUD. Effect (enforced in MessageService): in shared channels neither
 * sees the other's messages, and PMs between them are disallowed — symmetric,
 * regardless of which side blocked.
 */
class UserBlock {

    User owner
    User blocked

    Date dateCreated

    static constraints = {
        owner   nullable: false
        blocked nullable: false, unique: 'owner', validator: { User b, UserBlock ub ->
            if (b?.id && ub.owner?.id && b.id == ub.owner.id) return 'self'
            // Teachers/admin are always reachable — they can't be blocked.
            if ((b?.authorities*.authority ?: []).any { it in ['ROLE_TEACHER', 'ROLE_ADMIN'] }) return 'cannotBlockStaff'
        }
    }

    static mapping = {
        table 'user_block'
    }

    String toString() { "${owner} blocks ${blocked}" }
}
