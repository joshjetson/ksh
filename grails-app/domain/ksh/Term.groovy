package ksh

/**
 * A school term (e.g. "Fall 2026") with start/end dates — the time container for
 * classroom membership and grading. Admin-managed; readable by all signed-in users
 * (term dates aren't sensitive). Exactly one term is `active` at a time: setting one
 * active deactivates the others, so the `activeTerm` criteria token resolves cleanly.
 */
class Term {

    String  name
    Date    startsOn
    Date    endsOn
    boolean active = false

    Date dateCreated
    Date lastUpdated

    static constraints = {
        name     nullable: false, blank: false, maxSize: 80, unique: true
        startsOn nullable: false
        endsOn   nullable: false
    }

    static mapping = {
        table 'term'
    }

    def beforeInsert() {
        if (active) Term.where { active == true }.updateAll(active: false)
    }
    def beforeUpdate() {
        if (active && isDirty('active')) Term.where { active == true && id != this.id }.updateAll(active: false)
    }

    String toString() { name }
}
