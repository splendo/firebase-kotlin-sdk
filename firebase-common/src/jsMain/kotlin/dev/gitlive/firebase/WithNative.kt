package dev.gitlive.firebase

actual interface WithNative<N> {
    actual val native: N
    val js get() = native
}
