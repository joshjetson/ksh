package ksh

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests instruction string parsing logic.
 * These document the current split(':') behavior and will be used
 * to validate the colon delimiter fix (Concern #5).
 */
class InstructionParsingSpec extends Specification {

    // ====================================================================
    // CURRENT BEHAVIOR BASELINES
    // These test how instructions parse TODAY with split(':')
    // ====================================================================

    void "list instruction parses correctly"() {
        when:
        def parts = "list:Course".split(':')

        then:
        parts[0] == 'list'
        parts[1] == 'Course'
        parts.length == 2
    }

    void "get instruction parses correctly"() {
        when:
        def parts = "get:Course:courseId".split(':')

        then:
        parts[0] == 'get'
        parts[1] == 'Course'
        parts[2] == 'courseId'
        parts.length == 3
    }

    void "filter instruction parses correctly"() {
        when:
        def parts = "filter:CourseEnrollment:user.id=currentUserId,course.id=courseId".split(':')

        then:
        parts[0] == 'filter'
        parts[1] == 'CourseEnrollment'
        parts[2] == 'user.id=currentUserId,course.id=courseId'
        parts.length == 3
    }

    void "filterCount instruction parses correctly"() {
        when:
        def parts = "filterCount:CourseEnrollment:user.id=currentUserId".split(':')

        then:
        parts[0] == 'filterCount'
        parts[1] == 'CourseEnrollment'
        parts[2] == 'user.id=currentUserId'
        parts.length == 3
    }

    void "exists instruction parses correctly"() {
        when:
        def parts = "exists:CourseEnrollment:user.id=currentUserId,course.id=courseId".split(':')

        then:
        parts[0] == 'exists'
        parts[1] == 'CourseEnrollment'
        parts[2] == 'user.id=currentUserId,course.id=courseId'
        parts.length == 3
    }

    void "search instruction parses correctly"() {
        when:
        def parts = "search:Course:longTitle,shortTitle:q".split(':')

        then:
        parts[0] == 'search'
        parts[1] == 'Course'
        parts[2] == 'longTitle,shortTitle'
        parts[3] == 'q'
        parts.length == 4
    }

    void "findByOrGet instruction parses correctly"() {
        when:
        def parts = "findByOrGet:Course:shortTitle:title".split(':')

        then:
        parts[0] == 'findByOrGet'
        parts[1] == 'Course'
        parts[2] == 'shortTitle'
        parts[3] == 'title'
        parts.length == 4
    }

    void "currentUser instruction has one part"() {
        when:
        def parts = "currentUser".split(':')

        then:
        parts[0] == 'currentUser'
        parts.length == 1
    }

    void "literal instruction parses correctly"() {
        when:
        def parts = "literal:browse".split(':')

        then:
        parts[0] == 'literal'
        parts[1] == 'browse'
    }

    void "literal boolean parses correctly"() {
        when:
        def trueP = "literal:true".split(':')
        def falseP = "literal:false".split(':')

        then:
        trueP[1] == 'true'
        falseP[1] == 'false'
    }

    void "count instruction parses correctly"() {
        when:
        def parts = "count:Course".split(':')

        then:
        parts[0] == 'count'
        parts[1] == 'Course'
        parts.length == 2
    }

    void "param instruction parses correctly"() {
        when:
        def parts = "param:searchTerm".split(':')

        then:
        parts[0] == 'param'
        parts[1] == 'searchTerm'
    }

    void "date instruction parses correctly"() {
        when:
        def parts = "date:today".split(':')

        then:
        parts[0] == 'date'
        parts[1] == 'today'
    }

    void "service instruction without named service"() {
        when:
        def parts = "service:getCmiData".split(':')

        then:
        parts[0] == 'service'
        parts[1] == 'getCmiData'
        parts.length == 2
    }

    void "service instruction with named service"() {
        when:
        def parts = "service:scormService:getCmiData".split(':')

        then:
        parts[0] == 'service'
        parts[1] == 'scormService'
        parts[2] == 'getCmiData'
        parts.length == 3
    }

    // ====================================================================
    // COLON DELIMITER BUG (CONCERN #5)
    // Documents the known limitation with current split(':')
    // ====================================================================

    void "current split breaks on colon in criteria value"() {
        when: "criteria value contains a URL with colons"
        def parts = "filter:Course:field=http://example.com".split(':')

        then: "split produces too many parts — this is the known bug"
        parts.length == 4  // filter, Course, field=http, //example.com — broken
        parts[2] != 'field=http://example.com'  // criteria is mangled
    }

    void "current split breaks on time value"() {
        when:
        def parts = "literal:14:30".split(':')

        then: "split produces 3 parts instead of 2"
        parts.length == 3
        parts[1] == '14'  // wrong — should be '14:30'
    }

    // ====================================================================
    // CRITERIA VALUE RESOLUTION (resolveCriteriaValues logic)
    // These test the string manipulation that happens in the controller
    // ====================================================================

    void "criteria string splits by comma correctly"() {
        when:
        def criteria = "user.id=currentUserId,course.id=courseId"
        def parts = criteria.split(',')

        then:
        parts.length == 2
        parts[0] == 'user.id=currentUserId'
        parts[1] == 'course.id=courseId'
    }

    void "criterion splits by equals correctly"() {
        when:
        def criterion = "user.id=currentUserId"
        def parts = criterion.split('=', 2)

        then:
        parts[0] == 'user.id'
        parts[1] == 'currentUserId'
    }

    void "criterion with equals in value splits correctly"() {
        when: "value itself contains an equals sign"
        def criterion = "field=value=with=equals".split('=', 2)

        then: "only splits on first equals"
        criterion[0] == 'field'
        criterion[1] == 'value=with=equals'
    }

    // ====================================================================
    // NEW-GENERATION INSTRUCTIONS (fts / latest / latestCount / distinct / config)
    // ====================================================================

    void "fts instruction parses correctly"() {
        when:
        def parts = "fts:Course:search_fts:q".split(':')

        then:
        parts[0] == 'fts'
        parts[1] == 'Course'
        parts[2] == 'search_fts'
        parts[3] == 'q'
        parts.length == 4
    }

    void "latest instruction parses correctly"() {
        when:
        def parts = "latest:CourseEnrollment:enrolledAt:user.id=currentUserId".split(':')

        then:
        parts[0] == 'latest'
        parts[1] == 'CourseEnrollment'
        parts[2] == 'enrolledAt'
        parts[3] == 'user.id=currentUserId'
        parts.length == 4
    }

    void "latest instruction without criteria parses correctly"() {
        when:
        def parts = "latest:CourseEnrollment:enrolledAt".split(':')

        then:
        parts.length == 3
        parts[2] == 'enrolledAt'
    }

    void "latestCount instruction parses correctly"() {
        when:
        def parts = "latestCount:CourseEnrollment:enrolledAt:course.id=courseId".split(':')

        then:
        parts[0] == 'latestCount'
        parts.length == 4
    }

    void "distinct instruction parses correctly"() {
        when:
        def parts = "distinct:Course:tags".split(':')

        then:
        parts[0] == 'distinct'
        parts[1] == 'Course'
        parts[2] == 'tags'
        parts.length == 3
    }

    void "config instruction with dotted key parses correctly"() {
        when: "config keys contain dots but never colons"
        def parts = "config:ksh.public.url".split(':')

        then:
        parts[0] == 'config'
        parts[1] == 'ksh.public.url'
        parts.length == 2
    }

    // ====================================================================
    // NEW CRITERIA VALUE FORMS (in: / yyyy-MM-dd / null)
    // ====================================================================

    void "in-list criteria value survives comma and equals splitting"() {
        when: "membership values are pipe-delimited so the comma split is safe"
        def criteria = "status=in:ACTIVE|COMPLETED,user.id=3"
        def criteriaParts = criteria.split(',')
        def firstValue = criteriaParts[0].split('=', 2)[1]

        then:
        criteriaParts.length == 2
        firstValue == 'in:ACTIVE|COMPLETED'
        firstValue.substring(3).split('\\|') == ['ACTIVE', 'COMPLETED']
    }

    void "empty in-list produces no items (sentinel path)"() {
        expect: "an empty membership set must match nothing, not everything"
        'in:'.substring(3).split('\\|').findAll { it } == []
    }

    void "date-day criteria value matches the yyyy-MM-dd pattern"() {
        expect:
        ('2026-07-01' ==~ /\d{4}-\d{2}-\d{2}/)
        !('today' ==~ /\d{4}-\d{2}-\d{2}/)
        !('2026-7-1' ==~ /\d{4}-\d{2}-\d{2}/)
    }

    void "colon-delimiter sharp edge: a value containing a colon mis-splits"() {
        when: "documented limitation — criteria values must stay colon-free"
        def parts = "filter:Course:thumbnailSmall=https://example.com/x.png".split(':')

        then: "the URL is broken across segments; this is why colons are banned in values"
        parts.length > 3
        parts[2] != 'thumbnailSmall=https://example.com/x.png'
    }
}
