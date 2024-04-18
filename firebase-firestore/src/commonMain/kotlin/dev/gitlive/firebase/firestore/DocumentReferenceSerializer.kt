package dev.gitlive.firebase.firestore

import dev.gitlive.firebase.firestore.internal.NativeDocumentReference
import dev.gitlive.firebase.internal.FirebaseEncoder
import dev.gitlive.firebase.internal.SpecialValueSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

/**
 * A serializer for [DocumentReference]. If used with [FirebaseEncoder] performs serialization using native Firebase mechanisms.
 */
object DocumentReferenceSerializer : KSerializer<DocumentReference> by SpecialValueSerializer(
    serialName = "DocumentReference",
    toNativeValue = { it.wrapper.native },
    fromNativeValue = { value ->
        when (value) {
            is NativeDocumentReference -> DocumentReference(NativeDocumentReferenceWrapper(value))
            else -> throw SerializationException("Cannot deserialize $value")
        }
    }
)
