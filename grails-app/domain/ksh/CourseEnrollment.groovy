package ksh

class CourseEnrollment {

    User user
    Course course
    Date enrolledAt = new Date()
    Date completedAt
    Integer progress = 0

    // --- Commerce / lifecycle (managed by staff after create; tamper-safe on create) ---
    String status = 'ACTIVE'         // REQUESTED | ACTIVE | COMPLETED | CANCELLED
    String paymentStatus = 'UNPAID'  // UNPAID | DEPOSIT | PAID
    BigDecimal baseAmount            // K-credit price snapshot at enrollment time
    BigDecimal totalAmount           // baseAmount after any discount
    DiscountCode discountCode        // applied code, if any
    String codeEntered               // transient — raw code typed on the enroll form
    String notes

    Date dateCreated
    Date lastUpdated

    static final List<String> STATUSES = ['REQUESTED', 'ACTIVE', 'COMPLETED', 'CANCELLED']
    static final List<String> PAYMENT_STATUSES = ['UNPAID', 'DEPOSIT', 'PAID']

    static transients = ['codeEntered']

    static constraints = {
        completedAt nullable: true
        progress min: 0, max: 100
        user unique: 'course'
        status nullable: false, inList: STATUSES
        paymentStatus nullable: false, inList: PAYMENT_STATUSES
        baseAmount nullable: true, min: 0.0g, scale: 2
        totalAmount nullable: true, min: 0.0g, scale: 2
        discountCode nullable: true
        notes nullable: true, maxSize: 2000
    }

    static mapping = {
        table 'course_enrollment'
        user column: 'user_id'
        course column: 'course_id'
        enrolledAt column: 'enrolled_at'
        completedAt column: 'completed_at'
        status column: 'status'
        paymentStatus column: 'payment_status'
        baseAmount column: 'base_amount'
        totalAmount column: 'total_amount'
        discountCode column: 'discount_code_id'
        notes column: 'notes', type: 'text'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }

    /**
     * On create only, snapshot the price and apply a discount code if one was entered.
     * Force-sets status/paymentStatus so a student can't POST a forged "PAID" enrollment.
     * Staff updates (id != null) leave these alone.
     */
    def beforeValidate() {
        if (id == null) {
            BigDecimal base = (course?.costKCredits ?: 0) as BigDecimal
            baseAmount = base

            BigDecimal total = base
            if (codeEntered?.trim()) {
                DiscountCode dc = DiscountCode.findByCode(codeEntered.trim().toUpperCase())
                if (dc && dc.isRedeemable(base)) {
                    discountCode = dc
                    total = dc.applyTo(base)
                }
            }
            totalAmount = total

            if (!status) status = 'ACTIVE'
            // Free (or fully-discounted) enrollments are settled immediately; paid ones start UNPAID.
            paymentStatus = (total <= 0) ? 'PAID' : 'UNPAID'
        }
    }

    /** Atomically count a redemption once the enrollment is persisted. */
    def afterInsert() {
        if (discountCode?.id) {
            DiscountCode.executeUpdate(
                'update DiscountCode d set d.timesUsed = d.timesUsed + 1 where d.id = :id',
                [id: discountCode.id])
        }
    }
}
