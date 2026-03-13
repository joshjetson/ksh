package ksh

class Review {

    User user
    Course course
    Integer starRating
    String title
    String text

    Date dateCreated
    Date lastUpdated

    static constraints = {
        starRating min: 1, max: 5
        title nullable: false, blank: false
        text nullable: true
        user unique: 'course'
    }

    static mapping = {
        user column: 'user_id'
        course column: 'course_id'
        starRating column: 'star_rating'
        text type: 'text'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
