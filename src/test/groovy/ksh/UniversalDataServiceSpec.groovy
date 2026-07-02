package ksh

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import spock.lang.Specification

class UniversalDataServiceSpec extends Specification
    implements ServiceUnitTest<UniversalDataService>, DataTest {

    // The service @Autowires the passwordEncoder bean (registered in resources.groovy
    // in the real app) — the unit context must provide it too.
    Closure doWithSpring() {{ ->
        passwordEncoder(BCryptPasswordEncoder)
    }}

    Class[] getDomainClassesToMock() {
        [User, Role, UserRole, Course, CourseEnrollment, Review, Badge, UserBadge, ScormCmiData,
         Announcement, Event, FileResource, MediaPost, UserSetting, UserBlock]
    }

    User teacher
    User learner
    Course course

    def setup() {
        // Wire the DataBinder — ServiceUnitTest doesn't auto-inject it
        service.grailsWebDataBinder = applicationContext.getBean('grailsWebDataBinder')

        teacher = new User(username: 'teacher1', password: 'pass123', roleType: 'teacher', kCredits: 0, points: 0).save(failOnError: true, flush: true)
        learner = new User(username: 'learner1', password: 'pass123', roleType: 'learner', kCredits: 100, points: 0).save(failOnError: true, flush: true)
        course = new Course(shortTitle: 'Korean 101', longTitle: 'Intro to Korean', costKCredits: 0, pointReward: 10, creator: teacher).save(failOnError: true, flush: true)
    }

    // NOTE: In unit tests, association binding requires nested maps (creator: [id: X])
    // because there is no GrailsParameterMap. In the real app, form submissions use
    // dotted keys ('creator.id') which GrailsParameterMap converts automatically.
    // Both paths go through the same DataBinder — the difference is only in how
    // the map structure reaches SimpleMapDataBindingSource.

    // ====================================================================
    // CRUD BASELINES
    // ====================================================================

    void "getById returns instance"() {
        expect:
        service.getById(Course, course.id)?.shortTitle == 'Korean 101'
    }

    void "getById returns null for missing ID"() {
        expect:
        service.getById(Course, 99999L) == null
    }

    void "count returns total instances"() {
        expect:
        service.count(Course) == 1
    }

    void "deleteById removes instance"() {
        when:
        def result = service.deleteById(Course, course.id)

        then:
        result == true
        Course.get(course.id) == null
    }

    void "deleteById returns false for missing ID"() {
        expect:
        service.deleteById(Course, 99999L) == false
    }

    // ====================================================================
    // SAVE BASELINES
    // ====================================================================

    void "save creates Course with all fields bound"() {
        when:
        def result = service.save(Course, [
            shortTitle: 'Korean 201',
            longTitle: 'Intermediate Korean',
            costKCredits: '5',
            pointReward: '20',
            tags: 'grammar, intermediate',
            creator: [id: teacher.id]
        ])

        then:
        result != null
        result.shortTitle == 'Korean 201'
        result.longTitle == 'Intermediate Korean'
        result.costKCredits == 5
        result.pointReward == 20
        result.tags == 'grammar, intermediate'
        result.creator.id == teacher.id
    }

    void "save creates free Course with zero costs"() {
        when:
        def result = service.save(Course, [
            shortTitle: 'Free Course',
            longTitle: 'A Free Course',
            costKCredits: '0',
            pointReward: '0',
            creator: [id: teacher.id]
        ])

        then:
        result != null
        result.costKCredits == 0
        result.pointReward == 0
    }

    void "save creates CourseEnrollment with user and course"() {
        when:
        def result = service.save(CourseEnrollment, [
            user: [id: learner.id],
            course: [id: course.id]
        ])

        then:
        result != null
        result.user.id == learner.id
        result.course.id == course.id
        result.progress == 0
        result.completedAt == null
    }

    void "save creates Review with all fields"() {
        when:
        def result = service.save(Review, [
            user: [id: learner.id],
            course: [id: course.id],
            starRating: '5',
            title: 'Excellent',
            text: 'Loved it'
        ])

        then:
        result != null
        result.starRating == 5
        result.title == 'Excellent'
        result.user.id == learner.id
        result.course.id == course.id
    }

    void "save returns null for invalid data (missing required fields)"() {
        when:
        def result = service.save(Course, [
            shortTitle: 'No Long Title',
            creator: [id: teacher.id]
            // longTitle missing — required by constraints
        ])

        then:
        result == null
    }

    // ====================================================================
    // UPDATE BASELINES
    // ====================================================================

    void "update modifies existing Course fields"() {
        when:
        def updated = service.update(Course, course.id, [
            shortTitle: 'Korean 101 Updated',
            longTitle: 'Intro to Korean Updated',
            costKCredits: '10',
            pointReward: '50'
        ])

        then:
        updated.shortTitle == 'Korean 101 Updated'
        updated.costKCredits == 10
        updated.pointReward == 50
    }

    void "update does not overwrite creator when not in params"() {
        when:
        def updated = service.update(Course, course.id, [
            shortTitle: 'New Title'
        ])

        then:
        updated.shortTitle == 'New Title'
        updated.creator.id == teacher.id
    }

    void "update returns null for missing ID"() {
        expect:
        service.update(Course, 99999L, [shortTitle: 'X']) == null
    }

    // ====================================================================
    // LIST / PAGINATION BASELINES
    // ====================================================================

    void "list returns all instances"() {
        when:
        def results = service.list(Course)

        then:
        results.size() == 1
        results[0].shortTitle == 'Korean 101'
    }

    void "list respects max param"() {
        given:
        5.times { i ->
            new Course(shortTitle: "Course ${i}", longTitle: "Course ${i}", costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)
        }

        when:
        def results = service.list(Course, [max: 3])

        then:
        results.size() == 3
    }

    void "list with offset skips records"() {
        given:
        3.times { i ->
            new Course(shortTitle: "Course ${i}", longTitle: "Course ${i}", costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)
        }
        // 4 total (1 from setup + 3 new)

        when:
        def results = service.list(Course, [max: 10, offset: 2])

        then:
        results.size() == 2
    }

    // ====================================================================
    // FILTER BASELINES
    // ====================================================================

    void "filter by numeric field"() {
        given:
        new Course(shortTitle: 'Paid', longTitle: 'Paid Course', costKCredits: 10, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)

        when:
        def free = service.filter(Course, "costKCredits=0")
        def paid = service.filter(Course, "costKCredits=10")

        then:
        free.size() == 1
        free[0].shortTitle == 'Korean 101'
        paid.size() == 1
        paid[0].shortTitle == 'Paid'
    }

    // NOTE: filter/exists by association ID (e.g. "user.id=X") requires a real DB.
    // GORM DataTest mocks don't support nested property paths in createCriteria.
    // Those tests live in the integration spec (UniversalControllerIntegrationSpec).

    void "filter with multiple top-level criteria"() {
        given:
        new Course(shortTitle: 'Match', longTitle: 'Match', costKCredits: 77, pointReward: 88, creator: teacher).save(failOnError: true, flush: true)
        new Course(shortTitle: 'Partial', longTitle: 'Partial', costKCredits: 77, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)

        when:
        def results = service.filter(Course, "costKCredits=77,pointReward=88")

        then:
        results.size() == 1
        results[0].shortTitle == 'Match'
    }

    void "filter with empty criteria returns all"() {
        when:
        def results = service.filter(Course, "")

        then:
        results.size() == 1
    }

    void "filter by boolean field"() {
        given:
        // User has 'enabled' boolean field
        def disabledUser = new User(username: 'disabled1', password: 'pass', roleType: 'learner', enabled: false).save(failOnError: true, flush: true)

        when:
        def enabled = service.filter(User, "enabled=true")
        def disabled = service.filter(User, "enabled=false")

        then:
        enabled.size() == 2  // teacher + learner from setup
        disabled.size() == 1
        disabled[0].username == 'disabled1'
    }

    void "filter respects pagination"() {
        given:
        5.times { i ->
            new Course(shortTitle: "C${i}", longTitle: "C${i}", costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)
        }

        when:
        def results = service.filter(Course, "costKCredits=0", [max: 3])

        then:
        results.size() == 3
    }

    // ====================================================================
    // FILTER COUNT BASELINES
    // ====================================================================

    void "filterCount returns correct count"() {
        given:
        new Course(shortTitle: 'Paid', longTitle: 'Paid', costKCredits: 10, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)

        expect:
        service.filterCount(Course, "costKCredits=0") == 1
        service.filterCount(Course, "costKCredits=10") == 1
    }

    void "filterCount with empty criteria returns total"() {
        expect:
        service.filterCount(Course, "") == 1
    }

    // ====================================================================
    // EXISTS BASELINES
    // ====================================================================

    void "exists returns true when record matches by top-level field"() {
        given:
        new Course(shortTitle: 'Exists Test', longTitle: 'Exists Test', costKCredits: 99, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)

        expect:
        service.exists(Course, "costKCredits=99") == true
    }

    void "exists returns false when no match"() {
        expect:
        service.exists(Course, "costKCredits=999") == false
    }

    void "exists returns false for null domain"() {
        expect:
        service.exists(null, "user.id=1") == false
    }

    void "exists returns false for empty criteria"() {
        expect:
        service.exists(CourseEnrollment, "") == false
    }

    // ====================================================================
    // SEARCH BASELINES
    // ====================================================================

    void "search finds matching records"() {
        when:
        def results = service.search(Course, 'longTitle', 'Korean')

        then:
        results.size() == 1
    }

    void "search is case insensitive"() {
        when:
        def results = service.search(Course, 'longTitle', 'korean')

        then:
        results.size() == 1
    }

    void "search across multiple fields"() {
        when:
        def results = service.search(Course, 'longTitle,shortTitle', 'Korean')

        then:
        results.size() == 1
    }

    void "search with empty term returns all"() {
        when:
        def results = service.search(Course, 'longTitle', '')

        then:
        results.size() == 1
    }

    void "search with no matches returns empty"() {
        when:
        def results = service.search(Course, 'longTitle', 'Nonexistent')

        then:
        results.size() == 0
    }

    void "search respects pagination"() {
        given:
        5.times { i ->
            new Course(shortTitle: "Korean ${i}", longTitle: "Korean ${i}", costKCredits: 0, pointReward: 0, creator: teacher).save(failOnError: true, flush: true)
        }

        when:
        def results = service.search(Course, 'longTitle', 'Korean', [max: 3])

        then:
        results.size() == 3
    }

    // ====================================================================
    // CONCERN 2: SEARCH WILDCARD ESCAPING
    // GORM DataTest mock doesn't respect escape sequences in ilike,
    // so full search tests are in the integration spec.
    // Here we verify the escape method via reflection.
    // ====================================================================

    void "escapeLikeWildcards escapes percent"() {
        when:
        def result = service.invokeMethod('escapeLikeWildcards', '%%')

        then:
        result == '\\%\\%'
    }

    void "escapeLikeWildcards escapes underscore"() {
        when:
        def result = service.invokeMethod('escapeLikeWildcards', '___')

        then:
        result == '\\_\\_\\_'
    }

    void "escapeLikeWildcards leaves normal text alone"() {
        when:
        def result = service.invokeMethod('escapeLikeWildcards', 'Korean')

        then:
        result == 'Korean'
    }

    void "escapeLikeWildcards handles null"() {
        when:
        def result = service.invokeMethod('escapeLikeWildcards', [null] as Object[])

        then:
        result == null
    }

    // ====================================================================
    // FIND BY OR GET BASELINES
    // ====================================================================

    void "findByOrGet finds by field"() {
        when:
        def result = service.findByOrGet(Course, 'shortTitle', 'Korean 101')

        then:
        result != null
        result.id == course.id
    }

    void "findByOrGet falls back to get by ID"() {
        when:
        def result = service.findByOrGet(Course, 'shortTitle', course.id.toString())

        then:
        result != null
        result.id == course.id
    }

    void "findByOrGet returns null when not found"() {
        expect:
        service.findByOrGet(Course, 'shortTitle', 'Nonexistent') == null
    }

    // ====================================================================
    // ANNOUNCEMENT / EVENT BASELINES
    // ====================================================================

    void "save creates Announcement with author"() {
        when:
        def result = service.save(Announcement, [title: 'Fall term opens', body: 'Enroll now', pinned: 'true'],
                                  [author: teacher.id])

        then:
        result != null
        result.pinned
        result.author.id == teacher.id
    }

    void "save creates Event and rejects an unknown color"() {
        when:
        def ok = service.save(Event, [title: 'Live class', startsAt: '2026-07-15T18:00', color: 'sky'])
        def bad = service.save(Event, [title: 'Bad', startsAt: '2026-07-15T18:00', color: 'chartreuse'])

        then:
        ok != null
        !ok.allDay
        bad == null   // inList constraint fails loudly at the service boundary
    }

    // ====================================================================
    // FILE / MEDIA BASELINES (declarative multipart binding: byte[] +
    // ContentType + FileName params, as extractParams() injects them)
    // ====================================================================

    void "save creates FileResource from multipart-shaped params"() {
        when:
        def result = service.save(FileResource, [
            title: 'Syllabus', category: 'Handbook',
            file: 'PDFBYTES'.bytes, fileContentType: 'application/pdf', fileFileName: 'syllabus.pdf'])

        then:
        result != null
        result.hasFile()
        result.fileContentType == 'application/pdf'
    }

    void "save creates link-only FileResource without an upload"() {
        expect:
        service.save(FileResource, [title: 'Naver dictionary', externalUrl: 'https://dict.naver.com']) != null
    }

    void "save creates MediaPost with author override"() {
        when:
        def result = service.save(MediaPost, [
            caption: 'Class trip', image: 'PNGBYTES'.bytes,
            imageContentType: 'image/png', imageFileName: 'trip.png'], [author: teacher.id])

        then:
        result != null
        result.author.id == teacher.id
    }

    void "save rejects MediaPost without an image"() {
        expect: "image is the point of a media post — constraint fails loudly"
        service.save(MediaPost, [caption: 'No photo'], [author: teacher.id]) == null
    }

    // ====================================================================
    // PRIVACY BASELINES (UserSetting / UserBlock validators)
    // ====================================================================

    void "save creates UserSetting with forced owner"() {
        when:
        def result = service.save(UserSetting, [discoverable: 'true'], [user: learner.id])

        then:
        result != null
        result.discoverable
        result.user.id == learner.id
    }

    void "UserBlock rejects blocking yourself"() {
        expect:
        service.save(UserBlock, [blocked: [id: learner.id]], [owner: learner.id]) == null
    }

    void "UserBlock allows blocking another student once"() {
        given:
        def other = new User(username: 'other1', password: 'pass123', roleType: 'learner').save(failOnError: true, flush: true)

        expect: "first block works; a duplicate violates unique(owner,blocked)"
        service.save(UserBlock, [blocked: [id: other.id]], [owner: learner.id]) != null
        service.save(UserBlock, [blocked: [id: other.id]], [owner: learner.id]) == null
    }

    // ====================================================================
    // DISTINCT VALUES BASELINES
    // ====================================================================

    void "distinctValues returns unique non-null values ascending"() {
        given:
        new Course(shortTitle: 'B Course', longTitle: 'B', costKCredits: 5, pointReward: 0, creator: teacher, tags: 'beta').save(failOnError: true, flush: true)
        new Course(shortTitle: 'A Course', longTitle: 'A', costKCredits: 5, pointReward: 0, creator: teacher, tags: 'alpha').save(failOnError: true, flush: true)
        new Course(shortTitle: 'C Course', longTitle: 'C', costKCredits: 5, pointReward: 0, creator: teacher, tags: 'alpha').save(failOnError: true, flush: true)

        when:
        def values = service.distinctValues(Course, 'tags')

        then:
        values == ['alpha', 'beta']
    }

    void "distinctValues returns empty for null inputs"() {
        expect:
        service.distinctValues(null, 'tags') == []
        service.distinctValues(Course, null) == []
    }

    // ====================================================================
    // UPSERT BATCH BASELINES
    // ====================================================================

    void "upsertBatch inserts new records and updates existing by natural key"() {
        when: "two new courses arrive"
        def result = service.upsertBatch(Course, [
            [shortTitle: 'Batch A', longTitle: 'Batch A Long', costKCredits: '1', pointReward: '0', creator: [id: teacher.id]],
            [shortTitle: 'Batch B', longTitle: 'Batch B Long', costKCredits: '2', pointReward: '0', creator: [id: teacher.id]],
        ], ['shortTitle'])

        then:
        result.inserted == 2
        result.updated == 0

        when: "one of them is re-submitted with a change"
        def result2 = service.upsertBatch(Course, [
            [shortTitle: 'Batch A', longTitle: 'Batch A Revised', costKCredits: '9', pointReward: '0', creator: [id: teacher.id]],
        ], ['shortTitle'])

        then: "it updates instead of duplicating"
        result2.inserted == 0
        result2.updated == 1
        Course.findByShortTitle('Batch A').longTitle == 'Batch A Revised'
        Course.countByShortTitle('Batch A') == 1
    }

    void "upsertBatch skips records with a null or empty natural key"() {
        when:
        def result = service.upsertBatch(Course, [
            [shortTitle: '', longTitle: 'No Key', costKCredits: '0', pointReward: '0', creator: [id: teacher.id]],
        ], ['shortTitle'])

        then:
        result.inserted == 0
        result.updated == 0
    }

    // ====================================================================
    // PASSWORD BASELINES (encode-at-the-chokepoint)
    // ====================================================================

    void "updatePassword encodes a raw password"() {
        when:
        def ok = service.updatePassword(learner, 'newSecret123')

        then:
        ok
        learner.password.startsWith('$2')
        learner.password != 'newSecret123'
    }

    void "updatePassword is a no-op for blank password"() {
        given:
        def before = learner.password

        expect: "an edit form leaving the field empty keeps the current password"
        !service.updatePassword(learner, '')
        !service.updatePassword(learner, '   ')
        !service.updatePassword(null, 'whatever')
        learner.password == before
    }

    void "updatePassword passes through an already-hashed value unchanged"() {
        given: "a value that is already a bcrypt hash"
        service.updatePassword(learner, 'firstSecret')
        def hashed = learner.password

        when: "the same hash is submitted again (idempotence)"
        service.updatePassword(learner, hashed)

        then:
        learner.password == hashed
    }

    // ====================================================================
    // FTS GUARD PATHS (the Postgres query itself is exercised in dev —
    // these cover the sanitization/fallback behavior)
    // ====================================================================

    void "ftsSearch rejects an invalid column name"() {
        expect: "column names failing the \\w+ validation are refused outright"
        service.ftsSearch(Course, 'search_fts; DROP TABLE', 'korean') == []
        service.ftsSearch(Course, null, 'korean') == []
    }

    void "ftsSearch falls back to list for a blank term"() {
        expect:
        service.ftsSearch(Course, 'search_fts', '  ').size() == Course.count()
    }

    void "ftsSearch returns empty when the term sanitizes to nothing"() {
        expect: "punctuation-only input produces no tsquery tokens"
        service.ftsSearch(Course, 'search_fts', '!!! ???') == []
    }
}
