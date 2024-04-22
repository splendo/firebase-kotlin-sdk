package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.NativeCollectionReference
import dev.gitlive.firebase.firestore.await
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.ios

@PublishedApi
internal actual class NativeCollectionReferenceWrapper internal actual constructor(actual val native: NativeCollectionReference) : BaseNativeQueryWrapper(native) {

    actual val path: String
        get() = native.path

    actual val document get() = native.documentWithAutoID()

    actual val parent get() = native.parent

    actual fun document(documentPath: String) =
        native.documentWithPath(documentPath)

    actual suspend fun addEncoded(data: EncodedObject) =
        await { native.addDocumentWithData(data.ios, it) }
}