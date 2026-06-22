package io.riskily.sme.auth

import io.riskily.sme.user.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/** Loads users for authentication (by email) and for the JWT filter (by id). */
@Service
class AppUserDetailsService(private val users: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails =
        users.findByEmailIgnoreCase(email)
            .map { AppUserDetails(it) }
            .orElseThrow { UsernameNotFoundException("No user with email $email") }

    fun loadById(id: Long): AppUserDetails =
        users.findById(id)
            .map { AppUserDetails(it) }
            .orElseThrow { UsernameNotFoundException("No user with id $id") }
}
