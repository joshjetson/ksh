package ksh

import grails.gorm.transactions.Transactional
import java.util.zip.ZipInputStream

@Transactional
class ScormService {

    RewardService rewardService

    private static final String UPLOAD_DIR = 'uploads/scorm'

    // Matches Articulate Storyline's per-slide JS files (res/data/slide<N>.js).
    // Each slide loads its file when the user navigates to it, which makes this
    // the only reliable progress signal for packages that don't talk to the
    // SCORM API at all.
    private static final java.util.regex.Pattern SLIDE_FILE_PATTERN = ~/(?i).*[\/\\]slide(\d+)\.js$/

    // Lightweight stdout logger — opt into a tee'd file via -Dscorm.debug=true.
    // Default is stdout-only because the file-append churn was contributing to
    // I/O pressure during heavy SCORM sessions.
    private static final boolean DEBUG_TO_FILE = Boolean.getBoolean('scorm.debug')
    private static final File DEBUG_LOG = new File('/tmp/scorm-debug.log')

    private static void log(String msg) {
        String line = "${new Date().format('HH:mm:ss.SSS')} ${msg}"
        System.out.println(line)
        if (DEBUG_TO_FILE) {
            try { DEBUG_LOG.append(line + '\n') } catch (Exception ignored) { }
        }
    }

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
            }
        }

        // Count slide files so the progress bar has a denominator.
        course.scormSlideCount = countSlideFiles(courseDir)
        course.save(failOnError: true)

        log "SCORM extract: course=${course.id} slides=${course.scormSlideCount} launch=${course.scormLaunchUrl}"

        return course
    }

    /**
     * Ensure the package is extracted on disk. Called lazily by the content
     * controller when serving a file from a course that hasn't been extracted
     * yet (e.g. uploaded before the extract hook existed, or files cleaned up).
     */
    Course ensureExtracted(Course course) {
        if (!course?.scorm) return course
        def dir = new File(UPLOAD_DIR, course.id.toString())
        if (!dir.exists() || dir.list()?.length == 0) {
            log "SCORM ensureExtracted: re-extracting course=${course.id}"
            extractAndParseScorm(course)
        }
        return course
    }

    /**
     * Count distinct slide<N>.js files in the extracted package. Returns null
     * if no slides match (we leave scormSlideCount null rather than 0 so views
     * can detect 'unknown total' vs 'definitely zero').
     */
    private Integer countSlideFiles(File courseDir) {
        if (!courseDir?.exists()) return null
        Set<Integer> slides = [] as Set<Integer>
        courseDir.eachFileRecurse { f ->
            def m = SLIDE_FILE_PATTERN.matcher(f.absolutePath)
            if (m.matches()) slides << (m.group(1) as Integer)
        }
        return slides ? slides.size() : null
    }

    /**
     * Bump the learner's progress to reflect the highest slide they've reached,
     * given the total slide count. Idempotent and monotonic — never goes down.
     */
    void recordSlideVisit(User user, Course course, int slideNumber) {
        Integer total = course.scormSlideCount
        if (!total || total <= 0) return
        def enrollment = CourseEnrollment.findByUserAndCourse(user, course)
        if (!enrollment) return

        // Slide N in a 1-indexed package means N/total slides have been touched.
        int pct = Math.max(0, Math.min(100, ((slideNumber as BigDecimal) / total * 100).intValue()))
        Integer current = enrollment.progress ?: 0
        if (pct > current) {
            enrollment.progress = pct
            // 100% via slide visits is "viewed every slide" — treat as complete.
            if (pct >= 100 && !enrollment.completedAt) {
                enrollment.completedAt = new Date()
            }
            enrollment.save(failOnError: true)
            log "SCORM slide visit: user=${user.username} course=${course.id} slide=${slideNumber}/${total} progress ${current} -> ${pct}"
        }
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

        // SCORM 1.2 defaults
        if (!data.containsKey('cmi.core.lesson_status')) data['cmi.core.lesson_status'] = 'not attempted'
        if (!data.containsKey('cmi.core.student_id'))    data['cmi.core.student_id']    = user.id.toString()
        if (!data.containsKey('cmi.core.student_name'))  data['cmi.core.student_name']  = user.name ?: user.username
        if (!data.containsKey('cmi.core.credit'))        data['cmi.core.credit']        = 'credit'
        if (!data.containsKey('cmi.core.entry'))         data['cmi.core.entry']         = 'ab-initio'

        // SCORM 2004 defaults — same data, different key shape.
        if (!data.containsKey('cmi.completion_status')) data['cmi.completion_status'] = 'unknown'
        if (!data.containsKey('cmi.success_status'))    data['cmi.success_status']    = 'unknown'
        if (!data.containsKey('cmi.learner_id'))        data['cmi.learner_id']        = user.id.toString()
        if (!data.containsKey('cmi.learner_name'))      data['cmi.learner_name']      = user.name ?: user.username
        if (!data.containsKey('cmi.credit'))            data['cmi.credit']            = 'credit'
        if (!data.containsKey('cmi.entry'))             data['cmi.entry']             = 'ab-initio'
        if (!data.containsKey('cmi.mode'))              data['cmi.mode']              = 'normal'

        return data
    }

    // Location-bump increment — bigger jumps mean fewer Commit calls before the bar visibly moves.
    private static final int LOCATION_BUMP = 10
    // Progress cap from heuristics — only an explicit completion can push to 100.
    private static final int HEURISTIC_CEILING = 90

    /**
     * Save CMI data from the SCORM player
     */
    void saveCmiData(User user, Course course, Map<String, String> cmiData) {
        if (!cmiData) return

        // ONE query for all existing CMI rows for this (user, course). Avoids the
        // N findBy queries the old per-key loop did, which was the main DB pressure.
        Map<String, ScormCmiData> existing = ScormCmiData
            .findAllByUserAndCourse(user, course)
            .collectEntries { [(it.cmiKey): it] }

        String previousLocation = existing['cmi.core.lesson_location']?.cmiValue ?: existing['cmi.location']?.cmiValue
        String previousSuspend  = existing['cmi.suspend_data']?.cmiValue

        // Force entry='resume' so the next session resumes. LMS-controlled value;
        // SCO is read-only on it. Both 1.2 and 2004 key names.
        Map<String, String> dataToSave = new LinkedHashMap<>(cmiData)
        dataToSave['cmi.core.entry'] = 'resume'
        dataToSave['cmi.entry']      = 'resume'

        dataToSave.each { key, value ->
            String newVal = value?.toString()
            def rec = existing[key]
            if (rec) {
                // Skip the UPDATE entirely if the value hasn't changed — eliminates
                // the constant suspend_data churn that hammered Postgres' WAL.
                if (rec.cmiValue != newVal) {
                    rec.cmiValue = newVal
                    rec.save(failOnError: true)
                }
            } else {
                try {
                    new ScormCmiData(user: user, course: course, cmiKey: key, cmiValue: newVal).save(failOnError: true)
                } catch (Exception e) {
                    // Race: another concurrent persist inserted the same key. Update instead.
                    def retry = ScormCmiData.findByUserAndCourseAndCmiKey(user, course, key)
                    if (retry) {
                        retry.cmiValue = newVal
                        retry.save(failOnError: true)
                    } else {
                        throw e
                    }
                }
            }
        }

        // Update enrollment progress — partial during the session, 100 on completion.
        def enrollment = CourseEnrollment.findByUserAndCourse(user, course)
        if (enrollment) {
            String status1_2  = cmiData['cmi.core.lesson_status']
            String completion = cmiData['cmi.completion_status']
            String success    = cmiData['cmi.success_status']
            boolean justCompleted = (status1_2 in ['completed', 'passed'] || completion == 'completed' || success in ['passed', 'failed']) && !enrollment.completedAt
            Integer currentProgress = enrollment.progress ?: 0
            Integer newProgress = computeProgress(cmiData, currentProgress, previousLocation, previousSuspend)

            if (justCompleted) {
                enrollment.progress = 100
                enrollment.completedAt = new Date()
                enrollment.save(failOnError: true)
                // First-time completion → award points, course badges, milestones.
                rewardService.grantForCompletion(user, course)
            } else if (newProgress > currentProgress) {
                enrollment.progress = newProgress
                enrollment.save(failOnError: true)
            }
        }
    }

    // First non-blank value among the supplied CMI keys. Lets us read both
    // SCORM 1.2 (cmi.core.*) and SCORM 2004 (cmi.* without 'core') keys.
    private static String firstOf(Map<String, String> cmiData, String... keys) {
        for (k in keys) {
            String v = cmiData[k]?.toString()?.trim()
            if (v) return v
        }
        return null
    }

    /**
     * Derive 0..100 progress from CMI data. Five signals, in priority order:
     *   1. cmi.progress_measure (SCORM 2004) — 0..1 fraction reported by the SCO.
     *   2. score.raw / score.max — explicit score, normalized.
     *   3. completion_status / success_status / lesson_status — coarse status-to-percent map.
     *   4. location change — bump per navigation, capped at HEURISTIC_CEILING.
     *   5. First suspend_data write — last-resort signal of "started".
     * Never returns less than the existing progress.
     */
    private Integer computeProgress(Map<String, String> cmiData, Integer currentProgress,
                                    String previousLocation, String previousSuspend) {
        Integer base = currentProgress ?: 0

        // 1. SCORM 2004 progress_measure (0..1 fraction) — best signal when reported.
        String pm = cmiData['cmi.progress_measure']?.toString()
        if (pm?.isNumber()) {
            try {
                int pct = Math.max(0, Math.min(100, (new BigDecimal(pm) * 100).intValue()))
                return Math.max(base, pct)
            } catch (Exception ignored) { }
        }

        // 2. Score-based.
        String rawScore = firstOf(cmiData, 'cmi.core.score.raw', 'cmi.score.raw')
        if (rawScore?.isNumber()) {
            try {
                BigDecimal score = new BigDecimal(rawScore)
                String maxStr = firstOf(cmiData, 'cmi.core.score.max', 'cmi.score.max')
                BigDecimal max = (maxStr?.isNumber()) ? new BigDecimal(maxStr) : new BigDecimal(100)
                if (max > 0) {
                    int pct = Math.max(0, Math.min(100, ((score / max) * 100).intValue()))
                    return Math.max(base, pct)
                }
            } catch (Exception ignored) { }
        }

        // 3. Status-based — handles both 1.2 (lesson_status) and 2004 (completion+success).
        String status1_2  = cmiData['cmi.core.lesson_status']
        String completion = cmiData['cmi.completion_status']
        String success    = cmiData['cmi.success_status']
        if (status1_2 in ['completed', 'passed', 'failed'] || completion == 'completed' || success in ['passed', 'failed']) return 100
        if (status1_2 == 'incomplete' || completion == 'incomplete') return Math.max(base, 50)
        if (status1_2 == 'browsed') return Math.max(base, 25)

        // 4. Location change.
        String currentLocation = firstOf(cmiData, 'cmi.core.lesson_location', 'cmi.location')
        if (currentLocation && currentLocation != (previousLocation ?: '').trim()) {
            return Math.min(HEURISTIC_CEILING, base + LOCATION_BUMP)
        }

        // 5. First-suspend signal.
        String currentSuspend = cmiData['cmi.suspend_data']?.toString()?.trim()
        if (currentSuspend && !(previousSuspend?.trim()) && base < LOCATION_BUMP) {
            return LOCATION_BUMP
        }

        return base
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
