package ksh

class ScormCmiData {

    User user
    Course course
    String cmiKey
    String cmiValue

    Date dateCreated
    Date lastUpdated

    static constraints = {
        cmiKey nullable: false, blank: false
        cmiValue nullable: true
        user nullable: false
        course nullable: false
        cmiKey unique: ['user', 'course']
    }

    static mapping = {
        table 'scorm_cmi_data'
        user column: 'user_id'
        course column: 'course_id'
        cmiKey column: 'cmi_key'
        cmiValue column: 'cmi_value', type: 'text'
        dateCreated column: 'date_created'
        lastUpdated column: 'last_updated'
    }
}
