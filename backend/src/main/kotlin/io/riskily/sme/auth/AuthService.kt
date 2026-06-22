package io.riskily.sme.auth

import io.jsonwebtoken.JwtException
import io.riskily.sme.user.SubscriptionTier
import io.riskily.sme.user.User
import io.riskily.sme.user.UserRepository
import io.riskily.sme.user.UserRole
import io.riskily.sme.user.toSummary
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
) {

    /** Register a new SME user (defaults to USER role + BASIC tier) and issue tokens. */
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (users.existsByEmailIgnoreCase(request.email)) {
            throw EmailAlreadyExistsException(request.email)
        }
        val user = User(
            email = request.email.trim(),
            passwordHash = passwordEncoder.encode(request.password),
            firstName = request.firstName?.trim(),
            role = UserRole.USER,
            subscriptionTier = SubscriptionTier.BASIC,
        )
        val saved = users.save(user)
        return tokensFor(saved)
    }

    /** Authenticate by email + password (via the AuthenticationManager) and issue tokens. */
    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password),
        )
        val user = users.findByEmailIgnoreCase(request.email)
            .orElseThrow { InvalidTokenException("Authentication succeeded but user not found") }
        return tokensFor(user)
    }

    /** Exchange a valid refresh token for a fresh access + refresh pair (rotation). */
    @Transactional(readOnly = true)
    fun refresh(request: RefreshRequest): AuthResponse {
        val claims = try {
            jwtService.parse(request.refreshToken)
        } catch (_: JwtException) {
            throw InvalidTokenException()
        } catch (_: IllegalArgumentException) {
            throw InvalidTokenException()
        }
        if (jwtService.tokenType(claims) != TokenType.REFRESH) {
            throw InvalidTokenException("Not a refresh token")
        }
        val user = users.findById(jwtService.userId(claims))
            .orElseThrow { InvalidTokenException("User no longer exists") }
        return tokensFor(user)
    }

    private fun tokensFor(user: User): AuthResponse = AuthResponse(
        accessToken = jwtService.generateAccessToken(user),
        refreshToken = jwtService.generateRefreshToken(user),
        user = user.toSummary(),
    )
}
