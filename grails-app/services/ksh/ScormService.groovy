package ksh

import grails.gorm.transactions.Transactional
import java.util.zip.ZipInputStream

@Transactional
class ScormService {

    private static final String UPLOAD_DIR = 'uploads/scorm'

    /**
     * Extract SCORM zip bytes to disk and parse imsmanifest.xml for the launch URL
     */
    Course extractAndParseScorm(Course course) {
        if (!course.scorm) return course

        def courseDir = new File(UPLOAD_DIR, course.id.toString())

        // Clean up previous extraction
        if (courseDir.exists()) {
            courseDir.deleteDir()
        }
        courseDir.mkdirs()

        // Unzip
        def zis = new ZipInputStream(new ByteArrayInputStream(course.scorm))
        def entry
        while ((entry = zis.nextEntry) != null) {
            // Guard against path traversal
            if (entry.name.contains('..')) continue

            def outFile = new File(courseDir, entry.name)
            if (entry.isDirectory()) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                outFile.withOutputStream { os ->
                    os << zis
                }
            }
            zis.closeEntry()
        }
        zis.close()

        // Parse imsmanifest.xml
        def manifest = new File(courseDir, 'imsmanifest.xml')
        if (manifest.exists()) {
            def xml = new XmlSlurper().parse(manifest)
            def launchHref = xml.'**'.find { it.name() == 'resource' && it.@type?.toString()?.contains('sco') }?.@href?.toString()

            if (!launchHref) {
                // Fallback: first resource with an href
                launchHref = xml.'**'.find { it.name() == 'resource' && it.@href?.toString() }?.@href?.toString()
            }

            if (launchHref) {
                course.scormLaunchUrl = launchHref
                course.save(failOnError: true)
            }
        }

        return course
    }

    /**
     * Get all CMI data for a user/course as a Map
     */
    Map<String, String> getCmiData(User user, Course course) {
        Map<String, String> data = [:]

        // Load stored data
        ScormCmiData.findAllByUserAndCourse(user, course).each { cmi ->
            data[cmi.cmiKey] = cmi.cmiValue
        }

        // Set defaults if first time
        if (!data.containsKey('cmi.core.lesson_status')) {
            data['cmi.core.lesson_status'] = 'not attempted'
        }
        if (!data.containsKey('cmi.core.student_id')) {
            data['cmi.core.student_id'] = user.id.toString()
        }
        if (!data.containsKey('cmi.core.student_name')) {
            data['cmi.core.student_name'] = user.name ?: user.username
        }
        if (!data.containsKey('cmi.core.credit')) {
            data['cmi.core.credit'] = 'credit'
        }
        if (!data.containsKey('cmi.core.entry')) {
            data['cmi.core.entry'] = 'ab-initio'
        }

        return data
    }

    /**
     * Save CMI data from the SCORM player
     */
    void saveCmiData(User user, Course course, Map<String, String> cmiData) {
        cmiData.each { key, value ->
            def existing = ScormCmiData.findByUserAndCourseAndCmiKey(user, course, key)
            if (existing) {
                existing.cmiValue = value?.toString()
                existing.save(failOnError: true)
            } else {
                new ScormCmiData(
                    user: user,
                    course: course,
                    cmiKey: key,
                    cmiValue: value?.toString()
                ).save(failOnError: true)
            }
        }

        // Update enrollment progress on completion
        def status = cmiData['cmi.core.lesson_status']
        if (status in ['completed', 'passed']) {
            def enrollment = CourseEnrollment.findByUserAndCourse(user, course)
            if (enrollment && !enrollment.completedAt) {
                enrollment.progress = 100
                enrollment.completedAt = new Date()
                enrollment.save(failOnError: true)
            }
        }

        // Set entry to "resume" for next session
        def entryRecord = ScormCmiData.findByUserAndCourseAndCmiKey(user, course, 'cmi.core.entry')
        if (entryRecord) {
            entryRecord.cmiValue = 'resume'
            entryRecord.save(failOnError: true)
        } else {
            new ScormCmiData(user: user, course: course, cmiKey: 'cmi.core.entry', cmiValue: 'resume').save(failOnError: true)
        }
    }

    /**
     * Clean up extracted files on dev restart
     */
    void cleanExtractedFiles() {
        def dir = new File(UPLOAD_DIR)
        if (dir.exists()) {
            dir.deleteDir()
        }
    }
}
