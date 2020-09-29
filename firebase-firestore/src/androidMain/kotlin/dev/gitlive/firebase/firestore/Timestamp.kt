package dev.gitlive.firebase.firestore

actual typealias Timestamp = com.google.firebase.Timestamp

actual fun timestampNow(): Timestamp = Timestamp.now()
actual val Timestamp.seconds: Long get() = seconds
actual val Timestamp.nanoseconds: Int get() = nanoseconds
