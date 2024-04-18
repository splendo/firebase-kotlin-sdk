package dev.gitlive.firebase

actual interface WithNative<T> {
    actual val native: T
    val ios: T get() = native
}
