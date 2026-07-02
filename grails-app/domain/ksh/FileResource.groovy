package ksh

/**
 * A downloadable file (Syllabus, Handbook, Vocabulary lists, …). Managed by
 * teachers/admin; downloaded by any signed-in user via /universal/asset
 * (AUTHED_BINARY_FIELDS → 'file'), never a public route. Either an uploaded `file`
 * (byte[] in Postgres now; S3 later) OR an `externalUrl` link. The multipart binder
 * fills file/fileContentType/fileFileName from the upload.
 */
class FileResource {

    String title
    String category        // Syllabus, Handbook, Vocabulary, …
    byte[] file
    String fileContentType
    String fileFileName
    String externalUrl     // optional link instead of an upload

    Date dateCreated
    Date lastUpdated

    static constraints = {
        title           nullable: false, blank: false, maxSize: 200
        category        nullable: true,  maxSize: 80
        file            nullable: true,  maxSize: 52428800   // 50MB
        fileContentType nullable: true
        fileFileName    nullable: true,  maxSize: 255
        externalUrl     nullable: true,  url: true, maxSize: 1000
    }

    static mapping = {
        table 'file_resource'
        file  column: 'file', sqlType: 'bytea', lazy: true   // large byte[] MUST be lazy
    }

    boolean hasFile() { fileFileName != null }

    String toString() { title }
}
