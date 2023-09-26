package dev.gitlive.firebase.firestore

import cocoapods.FirebaseFirestore.FIRFieldValue

actual fun isSpecialValue(value: Any) = when(value) {
    is FIRFieldValue,
    is NativeGeoPoint,
    is NativeTimestamp,
    is NativeDocumentReference -> true
    else -> false
}