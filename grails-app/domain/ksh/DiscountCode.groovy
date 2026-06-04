package ksh

/**
 * A scholarship / promo code applied to a CourseEnrollment at enrollment time.
 * Amounts are in K-credits (matching Course.costKCredits). All redemption rules
 * live here (isRedeemable / applyTo) so the enrollment's beforeValidate can apply
 * a code without any service logic. Managed via generic UDA CRUD (staff only).
 */
class DiscountCode {

    String code            // unique, stored uppercase, e.g. "WELCOME10"
    String kind = 'PERCENT'  // PERCENT (% off) | FIXED (flat credits off)
    BigDecimal value = 0     // percentage (0–100) or flat credit amount
    boolean active = true
    Date startsOn
    Date expiresOn
    Integer usageLimit       // max redemptions (null = unlimited)
    Integer timesUsed = 0
    BigDecimal minAmount     // minimum enrollment subtotal to qualify (null = none)

    Date dateCreated
    Date lastUpdated

    static final List<String> KINDS = ['PERCENT', 'FIXED']

    static constraints = {
        code nullable: false, blank: false, unique: true, maxSize: 40
        kind nullable: false, inList: KINDS
        value nullable: false, min: 0.0g, scale: 2
        startsOn nullable: true
        expiresOn nullable: true
        usageLimit nullable: true, min: 1
        timesUsed nullable: false, min: 0
        minAmount nullable: true, min: 0.0g, scale: 2
    }

    static mapping = {
        table 'discount_code'
        code column: 'code'
        kind column: 'kind'
        value column: 'value'
        active column: 'active'
        startsOn column: 'starts_on'
        expiresOn column: 'expires_on'
        usageLimit column: 'usage_limit'
        timesUsed column: 'times_used'
        minAmount column: 'min_amount'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }

    def beforeValidate() {
        code = code?.trim()?.toUpperCase()
    }

    /** Is this code usable for an enrollment subtotal of `amount` at time `when`? */
    boolean isRedeemable(BigDecimal amount, Date when = new Date()) {
        if (!active) return false
        if (startsOn && when.before(startsOn)) return false
        if (expiresOn && when.after(expiresOn)) return false
        if (usageLimit != null && (timesUsed ?: 0) >= usageLimit) return false
        if (minAmount != null && (amount ?: 0) < minAmount) return false
        return true
    }

    /** Apply this code to a base amount (K-credits). Never returns below zero. */
    BigDecimal applyTo(BigDecimal base) {
        BigDecimal b = base ?: 0
        BigDecimal result
        if (kind == 'FIXED') {
            result = b - (value ?: 0)
        } else { // PERCENT
            result = b * (100 - (value ?: 0)) / 100
        }
        return (result < 0 ? 0 : result).setScale(2, BigDecimal.ROUND_HALF_UP)
    }

    /** Human-readable summary, e.g. "10% off" or "5 credits off". */
    String summary() {
        kind == 'FIXED' ? "${value} credits off" : "${value}% off"
    }
}
