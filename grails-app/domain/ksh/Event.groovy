package ksh

/**
 * A calendar event (school-wide: live sessions, deadlines, holidays). Managed by
 * teachers/admin; read by everyone. `startsAt`/`endsAt` bind from
 * <input type="datetime-local"> (see grails.databinding.dateFormats).
 */
class Event {

    String  title
    String  description
    String  location
    Date    startsAt
    Date    endsAt
    boolean allDay = false
    String  color = 'rose'   // calendar chip color — see KshTagLib.EVENT_COLORS

    Date dateCreated
    Date lastUpdated

    static constraints = {
        title       nullable: false, blank: false, maxSize: 200
        description nullable: true,  maxSize: 5000
        location    nullable: true,  maxSize: 200
        startsAt    nullable: false
        endsAt      nullable: true
        color       inList: ['rose', 'gold', 'sky', 'amber', 'emerald', 'violet']
    }

    static mapping = {
        table 'event'
        description type: 'text'
    }

    String toString() { title }
}
