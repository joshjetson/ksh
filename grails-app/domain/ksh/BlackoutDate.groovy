package ksh

/**
 * A day the school is closed (holiday, break, maintenance). Surfaced on the admin
 * calendar and available for any future scheduling logic. Managed via generic UDA
 * CRUD (staff only). v1 is all-day only.
 */
class BlackoutDate {

    Date blackoutDate
    String reason
    boolean allDay = true

    Date dateCreated
    Date lastUpdated

    static constraints = {
        blackoutDate nullable: false, unique: true
        reason nullable: true, maxSize: 200
    }

    static mapping = {
        table 'blackout_date'
        blackoutDate column: 'blackout_date'
        reason column: 'reason'
        allDay column: 'all_day'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }

    def beforeValidate() {
        blackoutDate = blackoutDate?.clearTime()
    }
}
