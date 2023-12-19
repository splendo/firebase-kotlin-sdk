package dev.gitlive.firebase

import com.google.firebase.FirebasePlatform

class MockFirebasePlatform : FirebasePlatform() {
    val storage = mutableMapOf<String, String>()
    override fun store(key: String, value: String) = storage.set(key, value)
    override fun retrieve(key: String) = storage[key]
    override fun clear(key: String) { storage.remove(key) }
    override fun log(msg: String) = println(msg)
}
