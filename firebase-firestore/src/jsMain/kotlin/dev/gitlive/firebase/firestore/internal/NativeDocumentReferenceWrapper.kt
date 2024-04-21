package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.EncodedFieldPath
import dev.gitlive.firebase.firestore.NativeCollectionReference
import dev.gitlive.firebase.firestore.NativeDocumentReference
import dev.gitlive.firebase.firestore.NativeDocumentSnapshot
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.errorToException
import dev.gitlive.firebase.firestore.externals.deleteDoc
import dev.gitlive.firebase.firestore.externals.getDoc
import dev.gitlive.firebase.firestore.externals.getDocFromCache
import dev.gitlive.firebase.firestore.externals.getDocFromServer
import dev.gitlive.firebase.firestore.externals.onSnapshot
import dev.gitlive.firebase.firestore.externals.refEqual
import dev.gitlive.firebase.firestore.externals.setDoc
import dev.gitlive.firebase.firestore.externals.updateDoc
import dev.gitlive.firebase.firestore.performUpdate
import dev.gitlive.firebase.firestore.rethrow
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.js
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.js.json

@PublishedApi
internal actual class NativeDocumentReferenceWrapper actual constructor(actual val native: NativeDocumentReference) {

    actual val id: String
        get() = rethrow { native.id }

    actual val path: String
        get() = rethrow { native.path }

    actual val parent: NativeCollectionReferenceWrapper
        get() = rethrow { NativeCollectionReferenceWrapper(native.parent) }

    actual fun collection(collectionPath: String) = rethrow {
        NativeCollectionReference(
            dev.gitlive.firebase.firestore.externals.collection(
                native,
                collectionPath
            )
        )
    }

    actual suspend fun get(source: Source) = rethrow {
        NativeDocumentSnapshot(
            native.get(source).await()
        )
    }

    actual val snapshots: Flow<NativeDocumentSnapshot> get() = snapshots()

    actual fun snapshots(includeMetadataChanges: Boolean) = callbackFlow<NativeDocumentSnapshot> {
        val unsubscribe = onSnapshot(
            native,
            json("includeMetadataChanges" to includeMetadataChanges),
            { trySend(NativeDocumentSnapshot(it)) },
            { close(errorToException(it)) }
        )
        awaitClose { unsubscribe() }
    }

    actual suspend fun setEncoded(encodedData: EncodedObject, setOptions: SetOptions) = rethrow {
        setDoc(native, encodedData.js, setOptions.js).await()
    }

    actual suspend fun updateEncoded(encodedData: EncodedObject) = rethrow { updateDoc(
        native,
        encodedData.js
    ).await() }

    actual suspend fun updateEncodedFieldsAndValues(encodedFieldsAndValues: List<Pair<String, Any?>>) {
        rethrow {
            encodedFieldsAndValues.takeUnless { encodedFieldsAndValues.isEmpty() }
                ?.performUpdate { field, value, moreFieldsAndValues ->
                    updateDoc(native, field, value, *moreFieldsAndValues)
                }
                ?.await()
        }
    }

    actual suspend fun updateEncodedFieldPathsAndValues(encodedFieldsAndValues: List<Pair<EncodedFieldPath, Any?>>) {
        rethrow {
            encodedFieldsAndValues.takeUnless { encodedFieldsAndValues.isEmpty() }
                ?.performUpdate { field, value, moreFieldsAndValues ->
                    updateDoc(native, field, value, *moreFieldsAndValues)
                }?.await()
        }
    }

    actual suspend fun delete() = rethrow { deleteDoc(native).await() }

    override fun equals(other: Any?): Boolean =
        this === other || other is NativeDocumentReferenceWrapper && refEqual(
            native,
            other.native
        )
    override fun hashCode(): Int = native.hashCode()
    override fun toString(): String = "DocumentReference(path=$path)"
}

private fun NativeDocumentReference.get(source: Source) = when (source) {
    Source.DEFAULT -> getDoc(this)
    Source.CACHE -> getDocFromCache(this)
    Source.SERVER -> getDocFromServer(this)
}