package dev.gitlive.firebase.firestore.internal

import com.google.android.gms.tasks.TaskExecutors
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.MetadataChanges
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.EncodedFieldPath
import dev.gitlive.firebase.firestore.Filter
import dev.gitlive.firebase.firestore.NativeDocumentSnapshot
import dev.gitlive.firebase.firestore.NativeQuery
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.WhereConstraint
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@PublishedApi
internal actual abstract class BaseNativeQueryWrapper internal actual constructor(actual val nativeQuery: NativeQuery) {

    actual fun limit(limit: Number) = nativeQuery.limit(limit.toLong())

    actual val snapshots get() = callbackFlow<QuerySnapshot> {
        val listener = nativeQuery.addSnapshotListener { snapshot, exception ->
            snapshot?.let { trySend(QuerySnapshot(snapshot)) }
            exception?.let { close(exception) }
        }
        awaitClose { listener.remove() }
    }

    actual fun snapshots(includeMetadataChanges: Boolean) = callbackFlow<QuerySnapshot> {
        val metadataChanges =
            if (includeMetadataChanges) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE
        val listener = nativeQuery.addSnapshotListener(metadataChanges) { snapshot, exception ->
            snapshot?.let { trySend(QuerySnapshot(snapshot)) }
            exception?.let { close(exception) }
        }
        awaitClose { listener.remove() }
    }

    actual suspend fun get(source: Source): QuerySnapshot =
        QuerySnapshot(nativeQuery.get(source.toAndroidSource()).await())

    actual fun where(filter: Filter) = nativeQuery.where(filter.toAndroidFilter())

    private fun Filter.toAndroidFilter(): com.google.firebase.firestore.Filter = when (this) {
        is Filter.And -> com.google.firebase.firestore.Filter.and(*filters.map { it.toAndroidFilter() }
            .toTypedArray())
        is Filter.Or -> com.google.firebase.firestore.Filter.or(*filters.map { it.toAndroidFilter() }
            .toTypedArray())
        is Filter.Field -> {
            when (constraint) {
                is WhereConstraint.ForNullableObject -> {
                    val modifier: (String, Any?) -> com.google.firebase.firestore.Filter = when (constraint) {
                        is WhereConstraint.EqualTo -> com.google.firebase.firestore.Filter::equalTo
                        is WhereConstraint.NotEqualTo -> com.google.firebase.firestore.Filter::notEqualTo
                    }
                    modifier.invoke(field, constraint.safeValue)
                }
                is WhereConstraint.ForObject -> {
                    val modifier: (String, Any) -> com.google.firebase.firestore.Filter = when (constraint) {
                        is WhereConstraint.LessThan -> com.google.firebase.firestore.Filter::lessThan
                        is WhereConstraint.GreaterThan -> com.google.firebase.firestore.Filter::greaterThan
                        is WhereConstraint.LessThanOrEqualTo -> com.google.firebase.firestore.Filter::lessThanOrEqualTo
                        is WhereConstraint.GreaterThanOrEqualTo -> com.google.firebase.firestore.Filter::greaterThanOrEqualTo
                        is WhereConstraint.ArrayContains -> com.google.firebase.firestore.Filter::arrayContains
                    }
                    modifier.invoke(field, constraint.safeValue)
                }
                is WhereConstraint.ForArray -> {
                    val modifier: (String, List<Any>) -> com.google.firebase.firestore.Filter = when (constraint) {
                        is WhereConstraint.InArray -> com.google.firebase.firestore.Filter::inArray
                        is WhereConstraint.ArrayContainsAny -> com.google.firebase.firestore.Filter::arrayContainsAny
                        is WhereConstraint.NotInArray -> com.google.firebase.firestore.Filter::notInArray
                    }
                    modifier.invoke(field, constraint.safeValues)
                }
            }
        }
        is Filter.Path -> {
            when (constraint) {
                is WhereConstraint.ForNullableObject -> {
                    val modifier: (FieldPath, Any?) -> com.google.firebase.firestore.Filter = when (constraint) {
                        is WhereConstraint.EqualTo -> com.google.firebase.firestore.Filter::equalTo
                        is WhereConstraint.NotEqualTo -> com.google.firebase.firestore.Filter::notEqualTo
                    }
                    modifier.invoke(path.android, constraint.safeValue)
                }
                is WhereConstraint.ForObject -> {
                    val modifier: (FieldPath, Any) -> com.google.firebase.firestore.Filter = when (constraint) {
                        is WhereConstraint.LessThan -> com.google.firebase.firestore.Filter::lessThan
                        is WhereConstraint.GreaterThan -> com.google.firebase.firestore.Filter::greaterThan
                        is WhereConstraint.LessThanOrEqualTo -> com.google.firebase.firestore.Filter::lessThanOrEqualTo
                        is WhereConstraint.GreaterThanOrEqualTo -> com.google.firebase.firestore.Filter::greaterThanOrEqualTo
                        is WhereConstraint.ArrayContains -> com.google.firebase.firestore.Filter::arrayContains
                    }
                    modifier.invoke(path.android, constraint.safeValue)
                }
                is WhereConstraint.ForArray -> {
                    val modifier: (FieldPath, List<Any>) -> com.google.firebase.firestore.Filter = when (constraint) {
                        is WhereConstraint.InArray -> com.google.firebase.firestore.Filter::inArray
                        is WhereConstraint.ArrayContainsAny -> com.google.firebase.firestore.Filter::arrayContainsAny
                        is WhereConstraint.NotInArray -> com.google.firebase.firestore.Filter::notInArray
                    }
                    modifier.invoke(path.android, constraint.safeValues)
                }
            }
        }
    }

    actual fun orderBy(field: String, direction: Direction) = nativeQuery.orderBy(field, direction)
    actual fun orderBy(field: EncodedFieldPath, direction: Direction) = nativeQuery.orderBy(field, direction)

    actual fun startAfter(document: NativeDocumentSnapshot) = nativeQuery.startAfter(document)
    actual fun startAfter(vararg fieldValues: Any) = nativeQuery.startAfter(*fieldValues)
    actual fun startAt(document: NativeDocumentSnapshot) = nativeQuery.startAt(document)
    actual fun startAt(vararg fieldValues: Any) = nativeQuery.startAt(*fieldValues)

    actual fun endBefore(document: NativeDocumentSnapshot) = nativeQuery.endBefore(document)
    actual fun endBefore(vararg fieldValues: Any) = nativeQuery.endBefore(*fieldValues)
    actual fun endAt(document: NativeDocumentSnapshot) = nativeQuery.endAt(document)
    actual fun endAt(vararg fieldValues: Any) = nativeQuery.endAt(*fieldValues)

    private fun addSnapshotListener(
        includeMetadataChanges: Boolean = false,
        listener: ProducerScope<QuerySnapshot>.(com.google.firebase.firestore.QuerySnapshot?, com.google.firebase.firestore.FirebaseFirestoreException?) -> Unit
    ) = callbackFlow {
        val executor = callbackExecutorMap[nativeQuery.firestore] ?: TaskExecutors.MAIN_THREAD
        val metadataChanges =
            if (includeMetadataChanges) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE
        val registration =
            nativeQuery.addSnapshotListener(executor, metadataChanges) { snapshots, exception ->
                listener(snapshots, exception)
            }
        awaitClose { registration.remove() }
    }
}