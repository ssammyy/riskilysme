package io.riskily.sme.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.riskily.sme.config.JwtProperties
import io.riskily.sme.user.User
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

enum class TokenType { ACCESS, REFRESH }

/** Issues and validates stateless JWT access/refresh tokens. */
@Service
class JwtService(private val props: JwtProperties) {

    private val key: SecretKey = Keys.hmacShaKeyFor(props.jwtSecret.toByteArray())

    fun generateAccessToken(user: User): String =
        build(user, TokenType.ACCESS, ChronoUnit.MINUTES, props.accessTtlMinutes)

    fun generateRefreshToken(user: User): String =
        build(user, TokenType.REFRESH, ChronoUnit.DAYS, props.refreshTtlDays)

    private fun build(user: User, type: TokenType, unit: ChronoUnit, amount: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("type", type.name)
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(amount, unit)))
            .signWith(key)
            .compact()
    }

    /** Parses and validates a token's signature and expiry. Throws JwtException if invalid. */
    fun parse(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload

    fun userId(claims: Claims): Long = claims.subject.toLong()

    fun tokenType(claims: Claims): TokenType = TokenType.valueOf(claims["type"] as String)

    fun isValid(token: String): Boolean =
        try {
            parse(token); true
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
}
