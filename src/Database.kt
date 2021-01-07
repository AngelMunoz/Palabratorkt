package me.tunaxor.apps

import org.bson.types.ObjectId
import org.litote.kmongo.*
import org.litote.kmongo.reactivestreams.*
import org.litote.kmongo.coroutine.*

private val dburl = System.getenv("PALABRATOR_DB_URL") ?: "mongodb://localhost:27017"
private val client = KMongo.createClient(dburl).coroutine
private val database = client.getDatabase("palabrator")

val palUsers = database.getCollection<User>("pal_users")

