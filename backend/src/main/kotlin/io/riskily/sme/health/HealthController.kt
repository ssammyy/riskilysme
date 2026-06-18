package io.riskily.sme.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/health")
class HealthController {

    @GetMapping
    fun health(): Map<String, Any> = mapOf(
        "status" to "UP",
        "service" to "riskily-sme",
        "time" to Instant.now().toString(),
    )
}
