package dev.gitlive.firebase.firestore

sealed class FirebaseReference {
    data class Value(val value: DocumentReferenceWrapper) : FirebaseReference()
    object ServerDelete : FirebaseReference()
}

val FirebaseReference.reference: DocumentReferenceWrapper? get() = when (this) {
    is FirebaseReference.Value -> value
    is FirebaseReference.ServerDelete -> null
}
