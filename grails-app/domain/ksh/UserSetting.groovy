package ksh

/**
 * Per-user, self-managed preferences. Owner-owned (OWNERSHIP_FIELDS + READ_SCOPE_FIELDS
 * → user): a user reads/creates/updates only their own row via generic CRUD.
 * `discoverable` (default off) controls whether other students can find and PM them.
 */
class UserSetting {

    User    user
    boolean discoverable = false

    Date dateCreated
    Date lastUpdated

    static constraints = {
        user nullable: false, unique: true
    }

    static mapping = {
        table 'user_setting'
    }

    String toString() { "settings(${user})" }
}
