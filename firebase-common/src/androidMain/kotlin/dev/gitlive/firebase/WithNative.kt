package dev.gitlive.firebase

actual interface WithNative<N> {
    actual val native: N
    val android: N get() = native
}
