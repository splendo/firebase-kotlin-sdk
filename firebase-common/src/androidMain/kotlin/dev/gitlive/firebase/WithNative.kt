package dev.gitlive.firebase

actual interface WithNative<T> {
    actual val native: T
    val android: T get() = native
}
