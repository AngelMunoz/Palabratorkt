package me.tunaxor.apps

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.profileRoutes(env: ApplicationEnvironment) {
    get("profiles") {
        try {
            val principal =
                call.principal<JWTPrincipal>() ?: throw InvalidClaimValuesException("No Principal in the request")
            val email =
                principal.payload.getClaim("email")
                    ?: throw InvalidClaimValuesException("The email is missing from the jwt")
            val user =
                User.findByEmail(email.asString()) ?: throw UserNotFoundException("Can't find the user from the jwt")
            call.respond(Profile.findProfiles(user._id))
        } catch (e: InvalidClaimValuesException) {
            call.response.status(HttpStatusCode.UnprocessableEntity)
            call.respond(FailedRequestResponse("Failed to get information from the jwt"))
        } catch (e: UserNotFoundException) {
            call.response.status(HttpStatusCode.NotFound)
            call.respond(FailedRequestResponse("Failed to get the user from the database"))
        }
    }

    post("profiles") {
        try {
            val params = call.receiveOrNull<Map<String, String>>()
                ?: throw InvalidContentException("Body was not present in the request")
            val name =
                params["name"] ?: throw InvalidContentException("the profile name was not present in the request")
            val principal =
                call.principal<JWTPrincipal>() ?: throw InvalidClaimValuesException("No Principal in the request")
            val email =
                principal.payload.getClaim("email")
                    ?: throw InvalidClaimValuesException("The email is missing from the jwt")
            val user =
                User.findByEmail(email.asString()) ?: throw UserNotFoundException("Can't find the user from the jwt")
            if (Profile.profileExists(name)) {
                call.response.status(HttpStatusCode.BadRequest)
                call.respond(FailedRequestResponse("The Profile Already Exists"))
                return@post
            }
            val created = Profile.insertProfile(ProfilePayload(user._id, name))
            if(!created) {
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
}