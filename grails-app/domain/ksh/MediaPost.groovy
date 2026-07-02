package ksh

/**
 * A media-feed post — a photo + caption shared by teachers/admin (class moments,
 * school events). Read by any signed-in user; the image is served via
 * /universal/asset (AUTHED_BINARY_FIELDS → 'image'), not a public route.
 * `author` is force-set to the creator (AUTHOR_FIELDS).
 */
class MediaPost {

    String caption
    byte[] image
    String imageContentType
    String imageFileName
    User   author

    Date dateCreated
    Date lastUpdated

    static constraints = {
        caption          nullable: true,  maxSize: 1000
        image            nullable: false, maxSize: 52428800   // 50MB
        imageContentType nullable: true
        imageFileName    nullable: true,  maxSize: 255
        author           nullable: true
    }

    static mapping = {
        table 'media_post'
        image column: 'image', sqlType: 'bytea', lazy: true   // large byte[] MUST be lazy
    }

    String toString() { "MediaPost ${id}" }
}
