package ksh

class Badge {

    String name
    String description
    String icon
    String requirements

    Date dateCreated
    Date lastUpdated

    static constraints = {
        name nullable: false, blank: false
        description nullable: true
        icon nullable: true
        requirements nullable: true
    }

    static mapping = {
        description type: 'text'
        requirements type: 'text'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
