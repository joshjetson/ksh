package ksh

class Badge {

    String name
    String description
    String icon
    String requirements

    Date dateCreated
    Date lastUpdated

    static constraints = {
        name nullable: false, blank: false, maxSize: 255
        description nullable: true, maxSize: 5000
        icon nullable: true, maxSize: 500
        requirements nullable: true, maxSize: 5000
    }

    static mapping = {
        description type: 'text'
        requirements type: 'text'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
