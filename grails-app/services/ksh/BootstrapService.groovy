package ksh

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder

@Transactional
class BootstrapService {

    @Autowired
    PasswordEncoder passwordEncoder

    def createRoles() {
        if (!Role.count()) {
            Role.findOrSaveWhere(authority: 'ROLE_ADMIN')
            Role.findOrSaveWhere(authority: 'ROLE_USER')
            Role.findOrSaveWhere(authority: 'ROLE_TEACHER')
        }
    }

    def createDevelopmentUsers() {
        if (!User.count()) {
            User admin = new User(username: 'admin',
                    password: passwordEncoder.encode('password'),
                    name: 'Admin',
                    email: 'admin@ksh.com',
                    roleType: 'admin',
                    kCredits: 1000,
                    points: 500)
            admin.save(failOnError: true)
            UserRole.create(admin, Role.findByAuthority('ROLE_USER'))
            UserRole.create(admin, Role.findByAuthority('ROLE_ADMIN'))
            UserRole.create(admin, Role.findByAuthority('ROLE_TEACHER'))

            User learner = new User(username: 'learner',
                    password: passwordEncoder.encode('password'),
                    name: 'Student Kim',
                    email: 'kim@ksh.com',
                    roleType: 'learner',
                    kCredits: 200,
                    points: 50)
            learner.save(failOnError: true)
            UserRole.create(learner, Role.findByAuthority('ROLE_USER'))

            User creator = new User(username: 'creator',
                    password: passwordEncoder.encode('password'),
                    name: 'Teacher Park',
                    email: 'park@ksh.com',
                    roleType: 'native_korean',
                    kCredits: 500,
                    points: 300)
            creator.save(failOnError: true)
            UserRole.create(creator, Role.findByAuthority('ROLE_USER'))
            UserRole.create(creator, Role.findByAuthority('ROLE_TEACHER'))

            User teacher = new User(username: 'teacher',
                    password: passwordEncoder.encode('password'),
                    name: 'Seonsaengnim Lee',
                    email: 'lee@ksh.com',
                    roleType: 'teacher',
                    kCredits: 300,
                    points: 200)
            teacher.save(failOnError: true)
            UserRole.create(teacher, Role.findByAuthority('ROLE_USER'))
            UserRole.create(teacher, Role.findByAuthority('ROLE_TEACHER'))

            log.info("Created dev users: admin, learner, creator, teacher (all password: password)")
        }
    }

    def createSampleCourses() {
        if (!Course.count()) {
            User creator = User.findByUsername('creator') ?: User.findByUsername('admin')

            new Course(shortTitle: 'Hangul Basics',
                    longTitle: 'Learn to Read and Write Hangul - The Korean Alphabet',
                    shortDescription: 'Master the Korean writing system from scratch. Perfect for absolute beginners.',
                    longDescription: 'This course covers all 14 basic consonants and 10 basic vowels of the Korean alphabet (Hangul). You will learn how to read, write, and pronounce each character, then combine them into syllable blocks. By the end, you will be able to read any Korean text phonetically.',
                    tags: 'hangul,beginner,writing,alphabet',
                    costKCredits: 0,
                    pointReward: 100,
                    badgeReward: 'Hangul Master',
                    creator: creator).save(failOnError: true)

            new Course(shortTitle: 'Korean Grammar 1',
                    longTitle: 'Essential Korean Grammar - Sentence Structure and Particles',
                    shortDescription: 'Learn the building blocks of Korean sentences including particles, verb conjugation, and word order.',
                    longDescription: 'Dive into Korean grammar fundamentals. This course covers subject/object/topic particles (은/는, 이/가, 을/를), basic verb conjugation in present tense, the SOV sentence structure, and essential connecting patterns. Includes practice exercises and real-world examples.',
                    tags: 'grammar,beginner,particles,verbs',
                    costKCredits: 50,
                    pointReward: 150,
                    creator: creator).save(failOnError: true)

            new Course(shortTitle: 'Everyday Korean',
                    longTitle: 'Everyday Korean Conversations - Survival Phrases and Daily Life',
                    shortDescription: 'Essential phrases for greetings, shopping, dining, and getting around in Korea.',
                    longDescription: 'A practical course focused on real conversations you will have in Korea. Covers greetings, introductions, ordering food, shopping, asking for directions, and common daily interactions. Each lesson includes audio examples and role-play scenarios.',
                    tags: 'conversation,beginner,travel,phrases',
                    costKCredits: 75,
                    pointReward: 120,
                    badgeReward: 'Conversation Starter',
                    creator: creator).save(failOnError: true)

            new Course(shortTitle: 'Korean Numbers',
                    longTitle: 'Korean Number Systems - Native and Sino-Korean',
                    shortDescription: 'Master both Korean number systems used for counting, time, dates, and money.',
                    longDescription: 'Korean has two number systems: native Korean (하나, 둘, 셋) and Sino-Korean (일, 이, 삼). This course teaches when and how to use each system, covering counters, time, dates, phone numbers, prices, and ages.',
                    tags: 'numbers,beginner,counting',
                    costKCredits: 30,
                    pointReward: 80,
                    creator: creator).save(failOnError: true)

            new Course(shortTitle: 'TOPIK I Prep',
                    longTitle: 'TOPIK I Exam Preparation - Levels 1 & 2',
                    shortDescription: 'Prepare for the TOPIK I exam with practice tests, vocabulary, and test-taking strategies.',
                    longDescription: 'Comprehensive preparation for the Test of Proficiency in Korean (TOPIK) Level I exam. Covers reading comprehension strategies, listening practice, essential vocabulary lists, grammar patterns frequently tested, and full-length practice exams with detailed answer explanations.',
                    tags: 'topik,exam,intermediate,test-prep',
                    costKCredits: 150,
                    pointReward: 300,
                    badgeReward: 'TOPIK Ready',
                    creator: creator).save(failOnError: true)

            new Course(shortTitle: 'Korean Culture',
                    longTitle: 'Understanding Korean Culture Through Language',
                    shortDescription: 'Learn the cultural context behind Korean honorifics, customs, and social norms.',
                    longDescription: 'Language and culture are inseparable in Korean. This course explores honorific speech levels (존댓말/반말), age-based social dynamics, Korean holidays and traditions, dining etiquette, and cultural references commonly found in K-dramas and Korean media.',
                    tags: 'culture,intermediate,honorifics,customs',
                    costKCredits: 60,
                    pointReward: 100,
                    creator: creator).save(failOnError: true)

            log.info("Created ${Course.count()} sample courses")
        }
    }

    def createSampleBadges() {
        if (!Badge.count()) {
            new Badge(name: 'Hangul Master', description: 'Completed the Hangul Basics course').save(failOnError: true)
            new Badge(name: 'Conversation Starter', description: 'Completed the Everyday Korean course').save(failOnError: true)
            new Badge(name: 'TOPIK Ready', description: 'Completed the TOPIK I Prep course').save(failOnError: true)
            new Badge(name: 'First Steps', description: 'Enrolled in your first course').save(failOnError: true)
            new Badge(name: 'Dedicated Learner', description: 'Completed 5 courses').save(failOnError: true)
            new Badge(name: 'Korean Scholar', description: 'Earned 1000 points').save(failOnError: true)

            log.info("Created ${Badge.count()} badges")
        }
    }
}
