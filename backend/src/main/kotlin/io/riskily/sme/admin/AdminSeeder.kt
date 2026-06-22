package io.riskily.sme.admin

import io.riskily.sme.audit.AuditService
import io.riskily.sme.user.User
import io.riskily.sme.user.UserRepository
import io.riskily.sme.user.UserRole
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/** Creates the initial admin account on startup if no admin exists. */
@Component
class AdminSeeder(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val auditService: AuditService,
    private val props: AdminProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (users.existsByRole(UserRole.ADMIN)) return
        val admin = User(
            email = props.email,
            passwordHash = passwordEncoder.encode(props.password),
            emailVerified = true,
            role = UserRole.ADMIN,
        )
        users.save(admin)
        auditService.record(
            action = "admin_seeded",
            entity = "users",
            newValue = props.email,
            performedByOverride = "system",
        )
        log.info("Seeded initial admin account: {}", props.email)
    }
}
