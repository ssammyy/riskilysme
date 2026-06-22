package io.riskily.sme.auth

/** Thrown when registering an email that already exists (mapped to HTTP 409). */
class EmailAlreadyExistsException(email: String) :
    RuntimeException("An account with email $email already exists")

/** Thrown when a refresh token is missing, malformed, expired, or the wrong type (HTTP 401). */
class InvalidTokenException(message: String = "Invalid or expired token") : RuntimeException(message)
