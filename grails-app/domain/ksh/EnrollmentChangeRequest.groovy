package ksh

/**
 * A student-initiated request to change one of their enrollments — withdraw from a
 * course or ask for a deadline extension. Staff approve/deny with a response.
 * On approval of a WITHDRAW, the afterUpdate hook cancels the enrollment.
 *
 * UDA-wired: students create (ownership = requestedBy, forced PENDING); staff update.
 */
class EnrollmentChangeRequest {

    CourseEnrollment enrollment
    User requestedBy
    String kind = 'WITHDRAW'   // WITHDRAW | EXTENSION
    Date requestedDate         // desired new date (EXTENSION)
    String message
    String status = 'PENDING'  // PENDING | APPROVED | DENIED
    String responseMessage

    Date dateCreated
    Date lastUpdated

    static final List<String> KINDS = ['WITHDRAW', 'EXTENSION']
    static final List<String> STATUSES = ['PENDING', 'APPROVED', 'DENIED']

    static constraints = {
        enrollment nullable: false
        requestedBy nullable: false, validator: { val, obj ->
            // anti-spoof: the requester must own the enrollment being changed
            if (val && obj.enrollment && obj.enrollment.user?.id != val.id) {
                return 'not.owner'
            }
        }
        kind nullable: false, inList: KINDS
        requestedDate nullable: true, validator: { val, obj ->
            if (obj.kind == 'EXTENSION' && !val) return 'required.for.extension'
        }
        message nullable: false, blank: false, maxSize: 2000
        status nullable: false, inList: STATUSES
        responseMessage nullable: true, maxSize: 2000
    }

    static mapping = {
        table 'enrollment_change_request'
        enrollment column: 'enrollment_id'
        requestedBy column: 'requested_by_id'
        kind column: 'kind'
        requestedDate column: 'requested_date'
        message column: 'message', type: 'text'
        status column: 'status'
        responseMessage column: 'response_message', type: 'text'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }

    def beforeValidate() {
        if (id == null) {
            status = 'PENDING'   // a student can't pre-approve their own request
        }
    }

    /** When staff approve a WITHDRAW, cancel the underlying enrollment. */
    def afterUpdate() {
        if (status == 'APPROVED' && kind == 'WITHDRAW' && enrollment?.id) {
            CourseEnrollment.executeUpdate(
                "update CourseEnrollment e set e.status = 'CANCELLED' where e.id = :id",
                [id: enrollment.id])
        }
    }
}
