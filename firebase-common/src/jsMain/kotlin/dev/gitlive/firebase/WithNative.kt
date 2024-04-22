package dev.gitlive.firebase

interface JsAccessor<JS> {
    val js: JS
}

val <JS, N : JsAccessor<JS>> WithNative<N>.js: JS get() = native.js
val <N> WithNative<N>.js: N get() = native
