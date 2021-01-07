package me.tunaxor.apps

import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.or

data class AuthResponse(val email: String, val token: String)
data class FailAuthResponse(val message: String)

data class User(val id: ObjectId, val name: String, val lastName: String, val email: String) {
    companion object {
        @Throws(IllegalArgumentException::class)
        suspend fun checkExists(id: ObjectId? = null, email: String? = null): Boolean {
            return when {
                id != null && email != null ->
                    palUsers.countDocuments(or(User::id eq id, User::email eq email)) > 0
                id != null ->
                    palUsers.countDocuments(User::id eq id) > 0
                email != null ->
                    palUsers.countDocuments(User::email eq email) > 0
                else -> throw IllegalArgumentException("At least id or email must be present")
            }
        }
    }
}

data class LoginPayload(val email: String, val password: String) {
    companion object {
        suspend fun findByEmail(email: String): LoginPayload? {
            return palUsers
                .withDocumentClass<LoginPayload>()
                .findOne(LoginPayload::email eq email)
        }
    }
}

data class SignupPayload(val name: String, val lastName: String, val email: String, val password: String) {
    companion object {
        suspend fun createUser(payload: SignupPayload): Boolean {
            val result = palUsers.withDocumentClass<SignupPayload>().insertOne(payload)
            return result.wasAcknowledged()
        }
    }
}
