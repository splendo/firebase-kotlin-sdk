package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.NativeCollectionReference
import dev.gitlive.firebase.firestore.externals.CollectionReference
import dev.gitlive.firebase.firestore.externals.addDoc
import dev.gitlive.firebase.firestore.externals.doc
import dev.gitlive.firebase.firestore.rethrow
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.js
import kotlinx.coroutines.await

@PublishedApi
internal actual class NativeCollectionReferenceWrapper internal actual constructor(native: NativeCollectionReference) : BaseNativeQueryWrapper<NativeCollectionReference>(native) {

    constructor(js: CollectionReference) : this(NativeCollectionReference(js))

    actual val path: String
        get() =  rethrow { native.path }

    actual val document get() = rethrow { doc(native) }

    actual val parent get() = rethrow { native.parent }

    actual fun document(documentPath: String) = rethrow {
        doc(
            native,
            documentPath
        )
    }

    actual suspend fun addEncoded(data: EncodedObject) = rethrow {
        addDoc(native, data.js).await()
    }
}