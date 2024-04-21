package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.EncodedFieldPath
import dev.gitlive.firebase.firestore.NativeDocumentSnapshot
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.SnapshotMetadata
import dev.gitlive.firebase.firestore.externals.DocumentSnapshot
import dev.gitlive.firebase.firestore.rethrow
import kotlin.js.json

@PublishedApi
internal actual class NativeDocumentSnapshotWrapper actual internal constructor(actual val native: NativeDocumentSnapshot) {

    constructor(js: DocumentSnapshot) : this(NativeDocumentSnapshot(js))

    actual val id get() = rethrow { native.id }
    actual val reference get() = rethrow { native.ref }

    actual fun getEncoded(field: String, serverTimestampBehavior: ServerTimestampBehavior): Any? = rethrow {
        native.get(field, getTimestampsOptions(serverTimestampBehavior))
    }

    actual fun getEncoded(fieldPath: EncodedFieldPath, serverTimestampBehavior: ServerTimestampBehavior): Any? = rethrow {
        native.get(fieldPath, getTimestampsOptions(serverTimestampBehavior))
    }

    actual fun encodedData(serverTimestampBehavior: ServerTimestampBehavior): Any? = rethrow {
        native.data(getTimestampsOptions(serverTimestampBehavior))
    }

    actual fun contains(field: String) = rethrow { native.get(field) != undefined }
    actual fun contains(fieldPath: EncodedFieldPath) = rethrow { native.get(fieldPath) != undefined }
    actual val exists get() = rethrow { native.exists() }
    actual val metadata: SnapshotMetadata get() = SnapshotMetadata(native.metadata)

    fun getTimestampsOptions(serverTimestampBehavior: ServerTimestampBehavior) =
        json("serverTimestamps" to serverTimestampBehavior.name.lowercase())
}