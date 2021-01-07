package me.tunaxor.apps

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*

fun Route.profileRoutes(env: ApplicationEnvironment) {
    suspend fun PipelineContext<Unit, ApplicationCall>.extractUser(): User {
        val email =
            call.principal<JWTPrincipal>()
                ?.payload
                ?.getClaim("email")
                ?: throw InvalidClaimValuesException("The email is missing from the jwt")
        return User.findByEmail(email.asString()) ?: throw UserNotFoundException("Can't find the user from the jwt")
    }

    get("/profiles") {
        try {
            val user = extractUser()
            call.respond(Profile.findProfiles(user._id))
        } catch (e: InvalidClaimValuesException) {
            call.response.status(HttpStatusCode.UnprocessableEntity)
            call.respond(FailedRequestResponse("Failed to get information from the jwt"))
        } catch (e: UserNotFoundException) {
            call.response.status(HttpStatusCode.NotFound)
            call.respond(FailedRequestResponse("Failed to get the user from the database"))
        }
    }

    post("/profiles") {
        try {
            val name =
                call.receiveOrNull<Map<String, String>>()
                    ?.get("name")
                    ?: throw InvalidContentException("the profile name was not present in the request")
            val user = extractUser()
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
            call.respond(Profile.findProfiles(user._id))
        } catch (e: InvalidClaimValuesException) {
            call.response.status(HttpStatusCode.UnprocessableEntity)
            call.respond(FailedRequestResponse("Failed to get information from the jwt"))
        } catch (e: UserNotFoundException) {
            call.response.status(HttpStatusCode.NotFound)
            call.respond(FailedRequestResponse("Failed to get the user from the database"))
        } catch (e: InvalidContentException) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailedRequestResponse("Profile name is not present in body"))
        }
    }

    put("/profiles") {
        try {
            val profile =
                call.receiveOrNull<Profile>()
                    ?: throw InvalidContentException("the profile was not present in the request")
            val user = extractUser()
            if(profile.owner != user._id) {
                call.response.status(HttpStatusCode.Unauthorized)
                call.respond(FailedRequestResponse("You don't have access to this resource"))
                return@put
            }
            val exists = Profile.exists(profile.name)
            if(exists) {
                call.response.status(HttpStatusCode.BadRequest)
                call.respond(FailedRequestResponse("The this profile name already exists"))
                return@put
            }

            val updated = Profile.rename(profile._id, profile.name)
            if(!updated) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
                call.respond(FailedRequestResponse("The profile name is available, but we failed to update"))
                return@put
            }
            call.respond(profile)
        } catch (e: InvalidClaimValuesException) {
            call.response.status(HttpStatusCode.UnprocessableEntity)
            call.respond(FailedRequestResponse("Failed to get information from the jwt"))
        } catch (e: UserNotFoundException) {
            call.response.status(HttpStatusCode.NotFound)
            call.respond(FailedRequestResponse("Failed to get the user from the database"))
        } catch (e: InvalidContentException) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailedRequestResponse("Profile name is not present in body"))
        }
    }
    delete("/profiles") {
        try {
            val profile =
                call.receiveOrNull<Profile>()
                    ?: throw InvalidContentException("the profile was not present in the request")
            val user = extractUser()
            if(profile.owner != user._id) {
                call.response.status(HttpStatusCode.Unauthorized)
                call.respond(FailedRequestResponse("You don't have access to this resource"))
                return@delete
            }
            val (deleted, count) = Profile.delete(profile._id, profile.owner)
            if(count > 1) {
                env.log.warn("[Profile: delete], Delete count for $profile is $count")
            }
            if(!deleted) {
                call.response.status(HttpStatusCode.UnprocessableEntity)
                call.respond(FailedRequestResponse("The profile name is available, but we failed to update"))
                return@delete
            }
            call.response.status(HttpStatusCode.NoContent)
        } catch (e: InvalidClaimValuesException) {
            call.response.status(HttpStatusCode.UnprocessableEntity)
            call.respond(FailedRequestResponse("Failed to get information from the jwt"))
        } catch (e: UserNotFoundException) {
            call.response.status(HttpStatusCode.NotFound)
            call.respond(FailedRequestResponse("Failed to get the user from the database"))
        } catch (e: InvalidContentException) {
            call.response.status(HttpStatusCode.BadRequest)
            call.respond(FailedRequestResponse("Profile name is not present in body"))
        }
    }
}