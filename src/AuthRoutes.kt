package me.tunaxor.apps

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import com.toxicbakery.bcrypt.Bcrypt
import io.ktor.server.engine.*
import java.time.Instant
import java.util.*


fun Route.authRoutes(env: ApplicationEnvironment) {

    val algorithm = Algorithm.HMAC256(env.config.property("jwt.secret").getString())
    val audience = env.config.property("jwt.audience").getString()
    val issuer = env.config.property("jwt.issuer").getString()

    post("/auth/login") {
        val payload =
            try {
                call.receiveOrNull<LoginPayload>()
            } catch (e: Exception) {
                env.log.warn("[Signup] Parsing Payload", e)
                null
            }
        if (payload == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailAuthResponse("Failed to receive credentials"))
            return@post
        }
        val user = LoginPayload.findByEmail(payload.email.toLowerCase())

        if (user == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailAuthResponse("Invalid Credentials"))
            return@post
        }

        val isValid = Bcrypt.verify(payload.password, user.password.toByteArray())

        if (isValid) {
            val token =
                JWT
                    .create()
                    .withClaim("email", user.email)
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .withIssuedAt(Date.from(Instant.now()))
                    .withNotBefore(Date.from(Instant.now()))
                    .sign(algorithm)

            call.respond(AuthResponse(user.email, token))
            return@post
        }
        call.respond(FailAuthResponse("Invalid Credentials"))
    }

    post("/auth/signup") {
        val payload =
            try {
                call.receiveOrNull<SignupPayload>()
            } catch (e: Exception) {
                env.log.warn("[Signup] Parsing Payload", e)
                null
            }
        if (payload == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailAuthResponse("Failed to receive signup data"))
            return@post
        }

        val exists = User.checkExists(email = payload.email)

        if(exists) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailAuthResponse("The User Already Exists"))
            return@post
        }
        val hash = Bcrypt.hash(payload.password, 12)
        val preSave = payload.copy(email = payload.email.toLowerCase(), password = String(hash, Charsets.UTF_8) )
        val created = SignupPayload.createUser(preSave)

        if(!created) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respond(FailAuthResponse("Failed to create the user"))
            return@post
        }
        val token =
            JWT
                .create()
                .withClaim("email", preSave.email)
                .withIssuer(issuer)
                .withAudience(audience)
                .withIssuedAt(Date.from(Instant.now()))
                .withNotBefore(Date.from(Instant.now()))
                .sign(algorithm)
        call.response.status(HttpStatusCode.Created)
        call.respond(AuthResponse(preSave.email, token))
        return@post
    }
}