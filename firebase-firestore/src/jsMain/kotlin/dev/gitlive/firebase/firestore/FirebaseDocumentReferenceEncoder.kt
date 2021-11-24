package dev.gitlive.firebase.firestore

actual class FirebaseDocumentReferenceEncoder actual constructor() {
    actual fun encode(value: DocumentReferenceWrapper): Any {
        return value.js
    }
}
