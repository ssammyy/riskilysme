package io.riskily.sme.auth

import io.riskily.sme.user.User
import io.riskily.sme.user.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/** Spring Security principal wrapping the domain [User]. */
class AppUserDetails(val user: User) : UserDetails {

    val id: Long get() = user.id ?: error("User must be persisted")

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))

    override fun getPassword(): String = user.passwordHash

    override fun getUsername(): String = user.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = user.status != UserStatus.SUSPENDED

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = user.status == UserStatus.ACTIVE
}
