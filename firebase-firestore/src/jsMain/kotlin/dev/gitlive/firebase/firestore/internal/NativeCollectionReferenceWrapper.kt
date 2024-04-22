package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.NativeCollectionReference
import dev.gitlive.firebase.firestore.NativeDocumentReference
import dev.gitlive.firebase.firestore.asNativeQuery
import dev.gitlive.firebase.firestore.externals.CollectionReference
import dev.gitlive.firebase.firestore.externals.addDoc
import dev.gitlive.firebase.firestore.externals.doc
import dev.gitlive.firebase.firestore.rethrow
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.js
import kotlinx.coroutines.await

@PublishedApi
internal actual class NativeCollectionReferenceWrapper internal actual constructor(actual val native: NativeCollectionReference) : BaseNativeQueryWrapper(native.asNativeQuery()) {

    constructor(js: CollectionReference) : this(NativeCollectionReference(js))

    private val js = native.js

    actual val path: String
        get() =  rethrow { js.path }

    actual val document get() = rethrow { doc(js) }

    actual val parent get() = rethrow { js.parent }

    actual fun document(documentPath: String) = rethrow {
            doc(
                js,
                documentPath
            )
    }

    actual suspend fun addEncoded(data: EncodedObject) = rethrow {
        addDoc(js, data.js).await()
    }
}