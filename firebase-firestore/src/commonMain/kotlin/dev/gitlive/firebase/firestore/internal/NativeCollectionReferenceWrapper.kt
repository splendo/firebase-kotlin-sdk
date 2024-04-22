package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.NativeCollectionReference
import dev.gitlive.firebase.firestore.NativeDocumentReference
import dev.gitlive.firebase.internal.EncodedObject

@PublishedApi
internal expect class NativeCollectionReferenceWrapper internal constructor(native: NativeCollectionReference) :
    BaseNativeQueryWrapper {

    val native: NativeCollectionReference

    val path: String
    val document: NativeDocumentReference
    val parent: NativeDocumentReference?

    fun document(documentPath: String): NativeDocumentReference
    suspend fun addEncoded(data: EncodedObject): NativeDocumentReference
}