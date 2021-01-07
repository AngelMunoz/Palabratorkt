package me.tunaxor.apps

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.or

class InvalidClaimValuesException(message: String?) : Exception(message)
class UserNotFoundException(message: String?): Exception(message)
class FailedToCreateJwtException(message: String?): Exception(message)
class InvalidContentException(message: String?): Exception(message)

data class AuthResponse(val email: String, val token: String)
data class FailedRequestResponse(val message: String)
data class PaginationResult<T>(val count: Long, val list: List<T>)
data class ProfilePayload(val owner: String, val name: String)

data class User(@BsonId val _id: String, val name: String, val lastName: String, val email: String) {
    companion object {

        suspend fun findById(id: String): User? {
            return palUsers.findOneById(id)
        }

        suspend fun findByEmail(email: String): User? {
            return palUsers.findOne(User::email eq email)
        }

        @Throws(IllegalArgumentException::class)
        suspend fun checkExists(id: String? = null, email: String? = null): Boolean {
            return when {
                id != null && email != null ->
                    palUsers.countDocuments(or(User::_id eq id, User::email eq email)) > 0
                id != null ->
                    palUsers.countDocuments(User::_id eq id) > 0
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

data class Profile(@BsonId val _id: String, val owner: String, val name: String) {
    companion object {
        suspend fun findProfiles(owner: String, page: Int = 1, limit: Int = 10): PaginationResult<Profile> {
            val filter = Profile::owner eq owner
            val offset = (page - 1) * limit
            val count = palProfiles.countDocuments(filter)
            val results = palProfiles.find(filter).limit(limit).skip(offset).toList()
            return PaginationResult(count, results)
        }

        suspend fun insertProfile(payload: ProfilePayload): Boolean {
            val result = palProfiles.withDocumentClass<ProfilePayload>().insertOne(payload)
            return result.wasAcknowledged()
        }

        suspend fun profileExists(name: String): Boolean {
            return palProfiles.countDocuments(Profile::name eq name) > 0
        }
    }
}

