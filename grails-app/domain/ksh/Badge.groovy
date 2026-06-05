package ksh

class Badge {

    String name
    String description
    String icon            // optional emoji / fallback glyph
    String requirements

    // What kind of collectible this is (vanilla badge, equippable avatar, …).
    // Drives how it's displayed and used across the gamification system.
    BadgeKind kind = BadgeKind.BADGE

    // Global auto-grant rule. NONE = course-attached or manual only. Otherwise the
    // grant engine compares the user's stat against unlockThreshold (see UnlockType).
    UnlockType unlockType = UnlockType.NONE
    Integer unlockThreshold

    // Uploaded artwork. Bound declaratively from a multipart `image` field via
    // UniversalController.extractParams(); served as bytes by BadgeController.
    // Lazy so listing badges never drags the blob (see docs/uda.md pillar 10).
    byte[] image
    String imageContentType
    String imageFileName

    Date dateCreated
    Date lastUpdated

    static constraints = {
        name nullable: false, blank: false, maxSize: 255
        description nullable: true, maxSize: 5000
        icon nullable: true, maxSize: 500
        requirements nullable: true, maxSize: 5000
        kind nullable: false
        unlockType nullable: false
        unlockThreshold nullable: true, min: 1
        image nullable: true, maxSize: 5242880  // 5 MB
        imageContentType nullable: true, maxSize: 100
        imageFileName nullable: true, maxSize: 255
    }

    static mapping = {
        description type: 'text'
        requirements type: 'text'
        kind column: 'kind'
        unlockType column: 'unlock_type'
        unlockThreshold column: 'unlock_threshold'
        image column: 'image', sqlType: 'bytea', lazy: true
        imageContentType column: 'image_content_type'
        imageFileName column: 'image_file_name'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }

    /** True when this collectible has uploaded artwork (vs just an emoji/icon). */
    boolean hasImage() {
        imageFileName != null
    }
}
