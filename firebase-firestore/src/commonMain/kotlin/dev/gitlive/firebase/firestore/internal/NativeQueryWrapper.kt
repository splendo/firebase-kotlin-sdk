package dev.gitlive.firebase.firestore.internal

import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.EncodedFieldPath
import dev.gitlive.firebase.firestore.Filter
import dev.gitlive.firebase.firestore.NativeDocumentSnapshot
import dev.gitlive.firebase.firestore.NativeQuery
import dev.gitlive.firebase.firestore.QuerySnapshot
import dev.gitlive.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow

@PublishedApi
internal expect abstract class BaseNativeQueryWrapper<Q : NativeQuery> internal constructor(native: Q) {

    val native: Q

    fun limit(limit: Number): NativeQuery
    val snapshots: Flow<QuerySnapshot>
    fun snapshots(includeMetadataChanges: Boolean = false): Flow<QuerySnapshot>
    suspend fun get(source: Source = Source.DEFAULT): QuerySnapshot

    fun where(filter: Filter): NativeQuery

    fun orderBy(field: String, direction: Direction): NativeQuery
    fun orderBy(field: EncodedFieldPath, direction: Direction): NativeQuery

    fun startAfter(document: NativeDocumentSnapshot): NativeQuery
    fun startAfter(vararg fieldValues: Any): NativeQuery
    fun startAt(document: NativeDocumentSnapshot): NativeQuery
    fun startAt(vararg fieldValues: Any): NativeQuery

    fun endBefore(document: NativeDocumentSnapshot): NativeQuery
    fun endBefore(vararg fieldValues: Any): NativeQuery
    fun endAt(document: NativeDocumentSnapshot): NativeQuery
    fun endAt(vararg fieldValues: Any): NativeQuery
}

@PublishedApi
internal class NativeQueryWrapper internal constructor(native: NativeQuery) : BaseNativeQueryWrapper<NativeQuery>(native)
