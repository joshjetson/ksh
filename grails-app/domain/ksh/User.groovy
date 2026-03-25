package ksh

import grails.compiler.GrailsCompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@GrailsCompileStatic
@EqualsAndHashCode(includes='username')
@ToString(includes='username', includeNames=true, includePackage=false)
class User implements Serializable {

	private static final long serialVersionUID = 1

	String username
	String password
	boolean enabled = true
	boolean accountExpired
	boolean accountLocked
	boolean passwordExpired

	String firstName
	String lastName
	String name
	String email
	String phoneNumber
	String avatar
	String title
	String country
	Date dateOfBirth
	String roleType = 'learner'
	Integer kCredits = 0
	Integer points = 0

	Set<Role> getAuthorities() {
		(UserRole.findAllByUser(this) as List<UserRole>)*.role as Set<Role>
	}

	static constraints = {
		password nullable: false, blank: false, password: true, maxSize: 255
		username nullable: false, blank: false, unique: true, maxSize: 100
		firstName nullable: true, maxSize: 255
		lastName nullable: true, maxSize: 255
		name nullable: true, maxSize: 255
		email nullable: true, maxSize: 255
		phoneNumber nullable: true, maxSize: 50
		avatar nullable: true, maxSize: 500
		title nullable: true, maxSize: 255
		country nullable: true, maxSize: 255
		dateOfBirth nullable: true
		roleType inList: ['admin', 'learner', 'teacher', 'native_korean', 'community_leader']
	}

	static mapping = {
		table 'app_user'
		password column: '`password`'
		accountExpired column: 'account_expired'
		accountLocked column: 'account_locked'
		passwordExpired column: 'password_expired'
		firstName column: 'first_name'
		lastName column: 'last_name'
		phoneNumber column: 'phone_number'
		dateOfBirth column: 'date_of_birth'
		roleType column: 'role_type'
		kCredits column: 'k_credits'
	}
}
