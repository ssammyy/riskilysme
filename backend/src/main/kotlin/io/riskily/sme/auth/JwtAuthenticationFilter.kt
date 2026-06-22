package io.riskily.sme.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Reads a Bearer access token, validates it, and populates the SecurityContext.
 * Invalid/absent tokens are ignored here — authorization rules reject the request later.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userDetailsService: AppUserDetailsService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ") &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val token = header.removePrefix("Bearer ").trim()
            runCatching {
                val claims = jwtService.parse(token)
                if (jwtService.tokenType(claims) == TokenType.ACCESS) {
                    val principal = userDetailsService.loadById(jwtService.userId(claims))
                    if (principal.isEnabled && principal.isAccountNonLocked) {
                        val auth = UsernamePasswordAuthenticationToken(
                            principal, null, principal.authorities,
                        )
                        auth.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = auth
                    }
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
