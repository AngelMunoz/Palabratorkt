package me.tunaxor.apps

import com.auth0.jwt.interfaces.Claim
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*

fun Route.profileRoutes(env: ApplicationEnvironment) {
    suspend fun PipelineContext<Unit, ApplicationCall>.extractUserFromRequest(): User? {
        val email =
            call.principal<JWTPrincipal>()
                ?.payload
                ?.getClaim("email")
        return when (email) {
            is Claim -> User.findByEmail(email.asString())
            else -> null
        }
    }

    get("/profiles") {
        val user = extractUserFromRequest()
        if (user != null) {
            call.respond(Profile.find(user._id))
            return@get
        }
        call.response.status(HttpStatusCode.NotFound)
        call.respond(FailedRequestResponse("Failed to get the user from the database"))
    }

    post("/profiles") {
        val name =
            try {
                call.receiveOrNull<Map<String, String>>()?.get("name")
            } catch (e: Exception) {
                null
            }
        if (name == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailedRequestResponse("Profile name is not present in body"))
            return@post
        }
        val user = extractUserFromRequest()
        if (user == null) {
            call.response.status(HttpStatusCode.NotFound)
            call.respond(FailedRequestResponse("Failed to get the user from the database"))
            return@post
        }
        if (Profile.exists(name)) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailedRequestResponse("The Profile Already Exists"))
            return@post
        }
        val created = Profile.create(ProfilePayload(user._id, name))
        if (!created) {
            call.response.status(HttpStatusCode.InternalServerError)
            call.respond(FailedRequestResponse("Failed to create the profile"))
            return@post
        }
        call.response.status(HttpStatusCode.Created)
        call.respond(Profile.find(user._id))
    }

    put("/profiles") {
        val profile =
            try {
                call.receiveOrNull<Profile>()
            } catch (e: Exception) {
                null
            }
        if (profile == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailedRequestResponse("Profile name is not present in body"))
            return@put
        }
        val user = extractUserFromRequest()
        if (user == null) {
            call.response.status(HttpStatusCode.NotFound)
            call.respond(FailedRequestResponse("Failed to get the user from the database"))
            return@put
        }
        if (profile.owner != user._id) {
            call.response.status(HttpStatusCode.Unauthorized)
            call.respond(FailedRequestResponse("You don't have access to this resource"))
            return@put
        }
        val exists = Profile.exists(profile.name)
        if (exists) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailedRequestResponse("The this profile name already exists"))
            return@put
        }
        val updated = Profile.rename(profile._id, profile.name)
        if (!updated) {
            call.response.status(HttpStatusCode.UnprocessableEntity)
            call.respond(FailedRequestResponse("The profile name is available, but we failed to update"))
            return@put
        }
        call.respond(profile)
    }

    delete("/profiles") {
        val profile =
            try {
                call.receiveOrNull<Profile>()
            } catch (e: Exception) {
                null
            }
        if (profile == null) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailedRequestResponse("Profile name is not present in body"))
            return@delete
        }
        val user = extractUserFromRequest()
        if (user == null) {
            call.response.status(HttpStatusCode.NotFound)
            call.respond(FailedRequestResponse("Failed to get the user from the database"))
            return@delete
        }
        if (profile.owner != user._id) {
            call.response.status(HttpStatusCode.Unauthorized)
            call.respond(FailedRequestResponse("You don't have access to this resource"))
            return@delete
        }
        val (deleted, count) = Profile.delete(profile._id, profile.owner)
        if (count > 1) {
            env.log.warn("[Profile: delete], Delete count for $profile is ${count}")
        }
        if (!deleted) {
            call.response.status(HttpStatusCode.UnprocessableEntity)
            call.respond(FailedRequestResponse("The profile name is available, but we failed to update"))
            return@delete
        }
        call.response.status(HttpStatusCode.NoContent)
    }
}