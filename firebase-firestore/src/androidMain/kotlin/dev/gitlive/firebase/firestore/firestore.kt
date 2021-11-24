/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("android")
package dev.gitlive.firebase.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import dev.gitlive.firebase.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

@PublishedApi
internal inline fun <reified T> decode(value: Any?): T =
    decode(value) { (it as? Timestamp)?.run { seconds * 1000 + (nanoseconds / 1000000.0) } }

internal fun <T> decode(strategy: DeserializationStrategy<T>, value: Any?): T =
    decode(strategy, value) { (it as? Timestamp)?.run { seconds * 1000 + (nanoseconds / 1000000.0) } }

actual val Firebase.firestore get() =
    FirebaseFirestore(com.google.firebase.firestore.FirebaseFirestore.getInstance())

actual fun Firebase.firestore(app: FirebaseApp) =
    FirebaseFirestore(com.google.firebase.firestore.FirebaseFirestore.getInstance(app.android))

actual class FirebaseFirestore(val android: com.google.firebase.firestore.FirebaseFirestore) : IFirebaseFirestore {

    override fun collection(collectionPath: String) =
        CollectionReferenceWrapper(android.collection(collectionPath))

    override fun document(documentPath: String) =
        DocumentReferenceWrapper(android.document(documentPath))

    override fun collectionGroup(collectionId: String) = Query(android.collectionGroup(collectionId))

    override fun batch() = WriteBatch(android.batch())

    override fun setLoggingEnabled(loggingEnabled: Boolean) =
        com.google.firebase.firestore.FirebaseFirestore.setLoggingEnabled(loggingEnabled)

    override suspend fun <T> runTransaction(func: suspend Transaction.() -> T) =
        android.runTransaction { runBlocking { Transaction(it).func() } }.await()

    override suspend fun clearPersistence() =
        android.clearPersistence().await().run { }

    override fun useEmulator(host: String, port: Int) {
        android.useEmulator(host, port)
        android.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }

    override fun setSettings(persistenceEnabled: Boolean?, sslEnabled: Boolean?, host: String?, cacheSizeBytes: Long?) {
        android.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder().also { builder ->
                persistenceEnabled?.let { builder.setPersistenceEnabled(it) }
                sslEnabled?.let { builder.isSslEnabled = it }
                host?.let { builder.host = it }
                cacheSizeBytes?.let { builder.cacheSizeBytes = it }
            }.build()
        }

    override suspend fun disableNetwork() =
        android.disableNetwork().await().run { }

    override suspend fun enableNetwork() =
        android.enableNetwork().await().run { }

}

actual class WriteBatch(val android: com.google.firebase.firestore.WriteBatch) {

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, merge: Boolean) = when(merge) {
        true -> android.set((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults)!!, SetOptions.merge())
        false -> android.set((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults)!!)
    }.let { this }

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        android.set((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults)!!, SetOptions.mergeFields(*mergeFields))
            .let { this }

    actual inline fun <reified T> set(documentRef: DocumentReference, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        android.set((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults)!!, SetOptions.mergeFieldPaths(mergeFieldPaths.map { it.android }))
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean) = when(merge) {
        true -> android.set((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults)!!, SetOptions.merge())
        false -> android.set((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults)!!)
    }.let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        android.set((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults)!!, SetOptions.mergeFields(*mergeFields))
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        android.set((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults)!!, SetOptions.mergeFieldPaths(mergeFieldPaths.map { it.android }))
            .let { this }

    @Suppress("UNCHECKED_CAST")
    actual inline fun <reified T> update(documentRef: DocumentReference, data: T, encodeDefaults: Boolean) =
        android.update((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults) as Map<String, Any>).let { this }

    @Suppress("UNCHECKED_CAST")
    actual fun <T> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        android.update((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults) as Map<String, Any>).let { this }

    @JvmName("updateFields")
    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>) =
        fieldsAndValues.takeUnless { fieldsAndValues.isEmpty() }
            ?.map { (field, value) -> field to encode(value, true) }
            ?.let { encoded -> android.update((documentRef as DocumentReferenceWrapper).android, encoded.toMap()) }
            .let { this }

    @JvmName("updateFieldPaths")
    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>) =
        fieldsAndValues.takeUnless { fieldsAndValues.isEmpty() }
            ?.map { (field, value) -> field.android to encode(value, true) }
            ?.let { encoded ->
                android.update(
                    (documentRef as DocumentReferenceWrapper).android,
                    encoded.first().first,
                    encoded.first().second,
                    *encoded.drop(1)
                        .flatMap { (field, value) -> listOf(field, value) }
                        .toTypedArray()
                )
            }
            .let { this }

    actual fun delete(documentRef: DocumentReference) =
        android.delete((documentRef as DocumentReferenceWrapper).android).let { this }

    actual suspend fun commit() = android.commit().await().run { Unit }

}

actual class Transaction(val android: com.google.firebase.firestore.Transaction) {

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, merge: Boolean) = when(merge) {
        true -> android.set((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults)!!, SetOptions.merge())
        false -> android.set((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults)!!)
    }.let { this }

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, vararg mergeFields: String) =
        android.set((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults)!!, SetOptions.mergeFields(*mergeFields))
            .let { this }

    actual fun set(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        android.set((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults)!!, SetOptions.mergeFieldPaths(mergeFieldPaths.map { it.android }))
            .let { this }

    actual fun <T> set(
        documentRef: DocumentReference,
        strategy: SerializationStrategy<T>,
        data: T,
        encodeDefaults: Boolean,
        merge: Boolean
    ) = when(merge) {
        true -> android.set((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults)!!, SetOptions.merge())
        false -> android.set((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults)!!)
    }.let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        android.set((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults)!!, SetOptions.mergeFields(*mergeFields))
            .let { this }

    actual fun <T> set(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        android.set((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults)!!, SetOptions.mergeFieldPaths(mergeFieldPaths.map { it.android }))
            .let { this }

    @Suppress("UNCHECKED_CAST")
    actual fun update(documentRef: DocumentReference, data: Any, encodeDefaults: Boolean) =
        android.update((documentRef as DocumentReferenceWrapper).android, encode(data, encodeDefaults) as Map<String, Any>).let { this }

    @Suppress("UNCHECKED_CAST")
    actual fun <T> update(documentRef: DocumentReference, strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        android.update((documentRef as DocumentReferenceWrapper).android, encode(strategy, data, encodeDefaults) as Map<String, Any>).let { this }

    @JvmName("updateFields")
    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<String, Any?>) =
        fieldsAndValues.takeUnless { fieldsAndValues.isEmpty() }
            ?.map { (field, value) -> field to encode(value, true) }
            ?.let { encoded -> android.update((documentRef as DocumentReferenceWrapper).android, encoded.toMap()) }
            .let { this }

    @JvmName("updateFieldPaths")
    actual fun update(documentRef: DocumentReference, vararg fieldsAndValues: Pair<FieldPath, Any?>) =
        fieldsAndValues.takeUnless { fieldsAndValues.isEmpty() }
            ?.map { (field, value) -> field.android to encode(value, true) }
            ?.let { encoded ->
                android.update(
                    (documentRef as DocumentReferenceWrapper).android,
                    encoded.first().first,
                    encoded.first().second,
                    *encoded.drop(1)
                        .flatMap { (field, value) -> listOf(field, value) }
                        .toTypedArray()
                )
            }.let { this }

    actual fun delete(documentRef: DocumentReference) =
        android.delete((documentRef as DocumentReferenceWrapper).android).let { this }

    actual suspend fun get(documentRef: DocumentReference) =
        DocumentSnapshot(android.get((documentRef as DocumentReferenceWrapper).android))
}

actual class DocumentReferenceWrapper(val android: com.google.firebase.firestore.DocumentReference) : DocumentReference {

    override val id: String
        get() = android.id

    override val path: String
        get() = android.path

    override fun collection(collectionPath: String) =
        CollectionReferenceWrapper(android.collection(collectionPath))

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, merge: Boolean) = when(merge) {
        true -> android.set(encode(data, encodeDefaults)!!, SetOptions.merge())
        false -> android.set(encode(data, encodeDefaults)!!)
    }.await().run { Unit }

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        android.set(encode(data, encodeDefaults)!!, SetOptions.mergeFields(*mergeFields))
            .await().run { Unit }

    actual suspend inline fun <reified T> set(data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        android.set(encode(data, encodeDefaults)!!, SetOptions.mergeFieldPaths(mergeFieldPaths.map { it.android }))
            .await().run { Unit }

    override suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, merge: Boolean) = when(merge) {
        true -> android.set(encode(strategy, data, encodeDefaults)!!, SetOptions.merge())
        false -> android.set(encode(strategy, data, encodeDefaults)!!)
    }.await().run { Unit }

    override suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFields: String) =
        android.set(encode(strategy, data, encodeDefaults)!!, SetOptions.mergeFields(*mergeFields))
            .await().run { Unit }

    override suspend fun <T> set(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean, vararg mergeFieldPaths: FieldPath) =
        android.set(encode(strategy, data, encodeDefaults)!!, SetOptions.mergeFieldPaths(mergeFieldPaths.map { it.android }))
            .await().run { Unit }

    @Suppress("UNCHECKED_CAST")
    actual suspend inline fun <reified T> update(data: T, encodeDefaults: Boolean) =
        android.update(encode(data, encodeDefaults) as Map<String, Any>).await().run { Unit }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> update(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        android.update(encode(strategy, data, encodeDefaults) as Map<String, Any>).await().run { Unit }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("updateFieldsByKey")
    override suspend fun update(vararg fieldsAndValues: Pair<String, Any?>) =
        android.takeUnless { fieldsAndValues.isEmpty() }
            ?.update(
                fieldsAndValues[0].first,
                fieldsAndValues[0].second,
                *fieldsAndValues.drop(1).flatMap { (field, value) ->
                    listOf(field, value?.let { encode(value, true) })
                }.toTypedArray()
            )
            ?.await()
            .run { Unit }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("updateFieldsByFieldPath")
    override suspend fun update(vararg fieldsAndValues: Pair<FieldPath, Any?>) =
        fieldsAndValues.takeUnless { fieldsAndValues.isEmpty() }
            ?.map { (field, value) -> field.android to encode(value, true) }
            ?.let { encoded ->
                android.update(
                    encoded.first().first,
                    encoded.first().second,
                    *encoded.drop(1)
                        .flatMap { (field, value) -> listOf(field, value) }
                        .toTypedArray()
                ).await()
            }.run { Unit }

    override suspend fun delete() =
        android.delete().await().run { Unit }

    override suspend fun get() =
        DocumentSnapshot(android.get().await())

    override val snapshots get() = callbackFlow<DocumentSnapshot> {
        val listener = android.addSnapshotListener { snapshot, exception ->
            snapshot?.let { safeOffer(DocumentSnapshot(snapshot)) }
            exception?.let { close(exception) }
        }
        awaitClose { listener.remove() }
    }
}

actual open class Query(open val android: com.google.firebase.firestore.Query) {

    actual suspend fun get() = QuerySnapshot(android.get().await())

    actual fun limit(limit: Number) = Query(android.limit(limit.toLong()))

    actual val snapshots get() = callbackFlow<QuerySnapshot> {
        val listener = android.addSnapshotListener { snapshot, exception ->
            snapshot?.let { safeOffer(QuerySnapshot(snapshot)) }
            exception?.let { close(exception) }
        }
        awaitClose { listener.remove() }
    }

    internal actual fun _where(field: String, equalTo: Any?) = Query(android.whereEqualTo(field, equalTo))
    internal actual fun _where(path: FieldPath, equalTo: Any?) = Query(android.whereEqualTo(path.android, equalTo))

    internal actual fun _where(field: String, equalTo: DocumentReference) = Query(android.whereEqualTo(field, (equalTo as DocumentReferenceWrapper).android))
    internal actual fun _where(path: FieldPath, equalTo: DocumentReference) = Query(android.whereEqualTo(path.android, (equalTo as DocumentReferenceWrapper).android))

    internal actual fun _where(
        field: String, lessThan: Any?, greaterThan: Any?, arrayContains: Any?, notEqualTo: Any?,
        lessThanOrEqualTo: Any?, greaterThanOrEqualTo: Any?
    ) = Query(
            when {
                lessThan != null -> android.whereLessThan(field, lessThan)
                greaterThan != null -> android.whereGreaterThan(field, greaterThan)
                arrayContains != null -> android.whereArrayContains(field, arrayContains)
                notEqualTo != null -> android.whereNotEqualTo(field, notEqualTo)
                lessThanOrEqualTo != null -> android.whereLessThanOrEqualTo(field, lessThanOrEqualTo)
                greaterThanOrEqualTo != null -> android.whereGreaterThanOrEqualTo(field, greaterThanOrEqualTo)
                else -> android
            }
        )

    internal actual fun _where(
        path: FieldPath, lessThan: Any?, greaterThan: Any?, arrayContains: Any?, notEqualTo: Any?,
        lessThanOrEqualTo: Any?, greaterThanOrEqualTo: Any?
    ) = Query(
            when {
                lessThan != null -> android.whereLessThan(path.android, lessThan)
                greaterThan != null -> android.whereGreaterThan(path.android, greaterThan)
                arrayContains != null -> android.whereArrayContains(path.android, arrayContains)
                notEqualTo != null -> android.whereNotEqualTo(path.android, notEqualTo)
                lessThanOrEqualTo != null -> android.whereLessThanOrEqualTo(path.android, lessThanOrEqualTo)
                greaterThanOrEqualTo != null -> android.whereGreaterThanOrEqualTo(path.android, greaterThanOrEqualTo)
                else -> android
            }
        )

    internal actual fun _where(
        field: String, inArray: List<Any>?, arrayContainsAny: List<Any>?, notInArray: List<Any>?
    ) = Query(
            when {
                inArray != null -> android.whereIn(field, inArray)
                arrayContainsAny != null -> android.whereArrayContainsAny(field, arrayContainsAny)
                notInArray != null -> android.whereNotIn(field, notInArray)
                else -> android
            }
        )

    internal actual fun _where(
        path: FieldPath, inArray: List<Any>?, arrayContainsAny: List<Any>?, notInArray: List<Any>?
    ) = Query(
            when {
                inArray != null -> android.whereIn(path.android, inArray)
                arrayContainsAny != null -> android.whereArrayContainsAny(path.android, arrayContainsAny)
                notInArray != null -> android.whereNotIn(path.android, notInArray)
                else -> android
            }
        )

    internal actual fun _orderBy(field: String, direction: Direction) = Query(android.orderBy(field, direction))
    internal actual fun _orderBy(field: FieldPath, direction: Direction) = Query(android.orderBy(field.android, direction))
}

actual typealias Direction = com.google.firebase.firestore.Query.Direction
actual typealias ChangeType = com.google.firebase.firestore.DocumentChange.Type

actual class CollectionReferenceWrapper(override val android: com.google.firebase.firestore.CollectionReference) : Query(android), CollectionReference {

    override val path: String
        get() = android.path

    override fun document(documentPath: String) = DocumentReferenceWrapper(android.document(documentPath))

    override fun document() = DocumentReferenceWrapper(android.document())

    actual suspend inline fun <reified T> add(data: T, encodeDefaults: Boolean): DocumentReference =
        DocumentReferenceWrapper(android.add(encode(data, encodeDefaults)!!).await())

    override suspend fun <T> add(data: T, strategy: SerializationStrategy<T>, encodeDefaults: Boolean) =
        DocumentReferenceWrapper(android.add(encode(strategy, data, encodeDefaults)!!).await())
    override suspend fun <T> add(strategy: SerializationStrategy<T>, data: T, encodeDefaults: Boolean) =
        DocumentReferenceWrapper(android.add(encode(strategy, data, encodeDefaults)!!).await())
}

actual typealias FirebaseFirestoreException = com.google.firebase.firestore.FirebaseFirestoreException

actual val FirebaseFirestoreException.code: FirestoreExceptionCode get() = code

actual typealias FirestoreExceptionCode = com.google.firebase.firestore.FirebaseFirestoreException.Code

actual class QuerySnapshot(val android: com.google.firebase.firestore.QuerySnapshot) {
    actual val documents
        get() = android.documents.map { DocumentSnapshot(it) }
    actual val documentChanges
        get() = android.documentChanges.map { DocumentChange(it) }
    actual val metadata: SnapshotMetadata get() = SnapshotMetadata(android.metadata)
}

actual class DocumentChange(val android: com.google.firebase.firestore.DocumentChange) {
    actual val document: DocumentSnapshot
        get() = DocumentSnapshot(android.document)
    actual val newIndex: Int
        get() = android.newIndex
    actual val oldIndex: Int
        get() = android.oldIndex
    actual val type: ChangeType
        get() = android.type
}

@Suppress("UNCHECKED_CAST")
actual class DocumentSnapshot(val android: com.google.firebase.firestore.DocumentSnapshot) {

    actual val id get() = android.id
    actual val reference: DocumentReference
        get() = DocumentReferenceWrapper(android.reference)

    actual inline fun <reified T: Any> data() = decode<T>(value = android.data)

    actual fun <T> data(strategy: DeserializationStrategy<T>) = decode(strategy, android.data)

    actual inline fun <reified T> get(field: String) = decode<T>(value = android.get(field))

    actual fun <T> get(field: String, strategy: DeserializationStrategy<T>) =
        decode(strategy, android.get(field))

    actual fun contains(field: String) = android.contains(field)

    actual val exists get() = android.exists()

    actual val metadata: SnapshotMetadata get() = SnapshotMetadata(android.metadata)
}

actual class SnapshotMetadata(val android: com.google.firebase.firestore.SnapshotMetadata) {
    actual val hasPendingWrites: Boolean get() = android.hasPendingWrites()
    actual val isFromCache: Boolean get() = android.isFromCache
}

actual class FieldPath private constructor(val android: com.google.firebase.firestore.FieldPath) {
    actual constructor(vararg fieldNames: String) : this(com.google.firebase.firestore.FieldPath.of(*fieldNames))
    actual val documentId: FieldPath get() = FieldPath(com.google.firebase.firestore.FieldPath.documentId())
}

actual object FieldValue {
    actual fun serverTimestamp(): Any = FieldValue.serverTimestamp()
    actual val delete: Any get() = FieldValue.delete()
    actual fun arrayUnion(vararg elements: Any): Any = FieldValue.arrayUnion(*elements)
    actual fun arrayRemove(vararg elements: Any): Any = FieldValue.arrayRemove(*elements)
    actual fun delete(): Any = delete
}

