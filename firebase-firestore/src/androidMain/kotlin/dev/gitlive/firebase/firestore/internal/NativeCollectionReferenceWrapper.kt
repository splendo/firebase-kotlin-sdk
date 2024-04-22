package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.NativeCollectionReference
import dev.gitlive.firebase.firestore.NativeDocumentReference
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.android
import kotlinx.coroutines.tasks.await

@PublishedApi
internal actual class NativeCollectionReferenceWrapper internal actual constructor(actual val native: NativeCollectionReference) : BaseNativeQueryWrapper(native) {

    actual val path: String
        get() = native.path

    actual val document: NativeDocumentReference
        get() = native.document()

    actual val parent: NativeDocumentReference?
        get() = native.parent

    actual fun document(documentPath: String) =
        native.document(documentPath)

    actual suspend fun addEncoded(data: EncodedObject) =
        native.add(data.android).await()
}