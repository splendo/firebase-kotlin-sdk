package dev.gitlive.firebase.firestore.internal

import cocoapods.FirebaseFirestoreInternal.FIRFilter
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.EncodedFieldPath
import dev.gitlive.firebase.firestore.Filter
import dev.gitlive.firebase.firestore.NativeDocumentSnapshot
import dev.gitlive.firebase.firestore.NativeQuery
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.WhereConstraint
import dev.gitlive.firebase.firestore.awaitResult
import dev.gitlive.firebase.firestore.toException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNull

@PublishedApi
internal actual abstract class BaseNativeQueryWrapper internal actual constructor(actual val nativeQuery: NativeQuery) {

    actual fun limit(limit: Number) = nativeQuery.queryLimitedTo(limit.toLong())

    actual suspend fun get(source: Source) =
        QuerySnapshot(awaitResult { nativeQuery.getDocumentsWithSource(source.toIosSource(), it) })

    actual val snapshots get() = callbackFlow<QuerySnapshot> {
        val listener = nativeQuery.addSnapshotListener { snapshot, error ->
            snapshot?.let { trySend(QuerySnapshot(snapshot)) }
            error?.let { close(error.toException()) }
        }
        awaitClose { listener.remove() }
    }

    actual fun snapshots(includeMetadataChanges: Boolean) = callbackFlow<QuerySnapshot> {
        val listener =
            nativeQuery.addSnapshotListenerWithIncludeMetadataChanges(includeMetadataChanges) { snapshot, error ->
                snapshot?.let { trySend(QuerySnapshot(snapshot)) }
                error?.let { close(error.toException()) }
            }
        awaitClose { listener.remove() }
    }

    actual fun where(filter: Filter) = nativeQuery.queryWhereFilter(filter.toFIRFilter())

    private fun Filter.toFIRFilter(): FIRFilter = when (this) {
        is Filter.And -> FIRFilter.andFilterWithFilters(filters.map { it.toFIRFilter() })
        is Filter.Or -> FIRFilter.orFilterWithFilters(filters.map { it.toFIRFilter() })
        is Filter.Field -> when (constraint) {
            is WhereConstraint.EqualTo -> FIRFilter.filterWhereField(field, isEqualTo = constraint.safeValue ?: NSNull.`null`())
            is WhereConstraint.NotEqualTo -> FIRFilter.filterWhereField(field, isNotEqualTo = constraint.safeValue ?: NSNull.`null`())
            is WhereConstraint.LessThan -> FIRFilter.filterWhereField(field, isLessThan = constraint.safeValue)
            is WhereConstraint.GreaterThan -> FIRFilter.filterWhereField(field, isGreaterThan = constraint.safeValue)
            is WhereConstraint.LessThanOrEqualTo -> FIRFilter.filterWhereField(field, isLessThanOrEqualTo = constraint.safeValue)
            is WhereConstraint.GreaterThanOrEqualTo -> FIRFilter.filterWhereField(field, isGreaterThanOrEqualTo = constraint.safeValue)
            is WhereConstraint.ArrayContains -> FIRFilter.filterWhereField(field, arrayContains = constraint.safeValue)
            is WhereConstraint.ArrayContainsAny -> FIRFilter.filterWhereField(field, arrayContainsAny = constraint.safeValues)
            is WhereConstraint.InArray -> FIRFilter.filterWhereField(field, `in` = constraint.safeValues)
            is WhereConstraint.NotInArray -> FIRFilter.filterWhereField(field, notIn = constraint.safeValues)
        }
        is Filter.Path -> when (constraint) {
            is WhereConstraint.EqualTo -> FIRFilter.filterWhereFieldPath(path.ios, isEqualTo = constraint.safeValue ?: NSNull.`null`())
            is WhereConstraint.NotEqualTo -> FIRFilter.filterWhereFieldPath(path.ios, isNotEqualTo = constraint.safeValue ?: NSNull.`null`())
            is WhereConstraint.LessThan -> FIRFilter.filterWhereFieldPath(path.ios, isLessThan = constraint.safeValue)
            is WhereConstraint.GreaterThan -> FIRFilter.filterWhereFieldPath(path.ios, isGreaterThan = constraint.safeValue)
            is WhereConstraint.LessThanOrEqualTo -> FIRFilter.filterWhereFieldPath(path.ios, isLessThanOrEqualTo = constraint.safeValue)
            is WhereConstraint.GreaterThanOrEqualTo -> FIRFilter.filterWhereFieldPath(path.ios, isGreaterThanOrEqualTo = constraint.safeValue)
            is WhereConstraint.ArrayContains -> FIRFilter.filterWhereFieldPath(path.ios, arrayContains = constraint.safeValue)
            is WhereConstraint.ArrayContainsAny -> FIRFilter.filterWhereFieldPath(path.ios, arrayContainsAny = constraint.safeValues)
            is WhereConstraint.InArray -> FIRFilter.filterWhereFieldPath(path.ios, `in` = constraint.safeValues)
            is WhereConstraint.NotInArray -> FIRFilter.filterWhereFieldPath(path.ios, notIn = constraint.safeValues)
        }
    }

    actual fun orderBy(field: String, direction: Direction) = nativeQuery.queryOrderedByField(field, direction == Direction.DESCENDING)
    actual fun orderBy(field: EncodedFieldPath, direction: Direction) = nativeQuery.queryOrderedByFieldPath(field, direction == Direction.DESCENDING)

    actual fun startAfter(document: NativeDocumentSnapshot) = nativeQuery.queryStartingAfterDocument(document)
    actual fun startAfter(vararg fieldValues: Any) = nativeQuery.queryStartingAfterValues(fieldValues.asList())
    actual fun startAt(document: NativeDocumentSnapshot) = nativeQuery.queryStartingAtDocument(document)
    actual fun startAt(vararg fieldValues: Any) = nativeQuery.queryStartingAtValues(fieldValues.asList())

    actual fun endBefore(document: NativeDocumentSnapshot) = nativeQuery.queryEndingBeforeDocument(document)
    actual fun endBefore(vararg fieldValues: Any) = nativeQuery.queryEndingBeforeValues(fieldValues.asList())
    actual fun endAt(document: NativeDocumentSnapshot) = nativeQuery.queryEndingAtDocument(document)
    actual fun endAt(vararg fieldValues: Any) = nativeQuery.queryEndingAtValues(fieldValues.asList())
}