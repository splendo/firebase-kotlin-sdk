/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

actual fun FirebaseDecoder.structureDecoder(descriptor: SerialDescriptor, decodeDouble: (value: Any?) -> Double?): CompositeDecoder = when(descriptor.kind) {
    StructureKind.CLASS, StructureKind.OBJECT, PolymorphicKind.SEALED -> when {
        value is Map<*, *> ->
            FirebaseClassDecoder(decodeDouble, value.size, { value.containsKey(it) }) { desc, index ->
                value[desc.getElementName(index)]
            }
        value != null && value::class.qualifiedName == "com.google.firebase.Timestamp" -> {
            makeTimestampJavaReflectionDecoder(decodeDouble, value)
        }
        value != null && value::class.qualifiedName == "com.google.firebase.firestore.GeoPoint" -> {
            makeGeoPointJavaReflectionDecoder(decodeDouble, value)
        }
        value != null && value::class.qualifiedName == "com.google.firebase.firestore.DocumentReference" -> {
            makeDocumentReferenceJavaReflectionDecoder(decodeDouble, value)
        }
        else -> FirebaseEmptyCompositeDecoder()
    }
    StructureKind.LIST, is PolymorphicKind -> (value as List<*>).let {
        FirebaseCompositeDecoder(decodeDouble, it.size) { _, index -> it[index] }
    }
    StructureKind.MAP -> (value as Map<*, *>).entries.toList().let {
        FirebaseCompositeDecoder(decodeDouble, it.size) { _, index -> it[index / 2].run { if (index % 2 == 0) key else value } }
    }
    else -> TODO("The firebase-kotlin-sdk does not support $descriptor for serialization yet")
}

private val timestampKeys = setOf("seconds", "nanoseconds")

private fun makeTimestampJavaReflectionDecoder(decodeDouble: (value: Any?) -> Double?, jvmObj: Any): CompositeDecoder {
    val timestampClass = Class.forName("com.google.firebase.Timestamp")
    val getSeconds = timestampClass.getMethod("getSeconds")
    val getNanoseconds = timestampClass.getMethod("getNanoseconds")

    return FirebaseClassDecoder(
        decodeDouble,
        size = 2,
        containsKey = { timestampKeys.contains(it) }
    ) { descriptor, index ->
        when (descriptor.getElementName(index)) {
            "seconds" -> getSeconds.invoke(jvmObj) as Long
            "nanoseconds" -> getNanoseconds.invoke(jvmObj) as Int
            else -> null
        }
    }
}

private val geoPointKeys = setOf("latitude", "longitude")

private fun makeGeoPointJavaReflectionDecoder(decodeDouble: (value: Any?) -> Double?, jvmObj: Any): CompositeDecoder {
    val timestampClass = Class.forName("com.google.firebase.firestore.GeoPoint")
    val getLatitude = timestampClass.getMethod("getLatitude")
    val getLongitude = timestampClass.getMethod("getLongitude")

    return FirebaseClassDecoder(
        decodeDouble,
        size = 2,
        containsKey = { geoPointKeys.contains(it) }
    ) { descriptor, index ->
        when (descriptor.getElementName(index)) {
            "latitude" -> getLatitude.invoke(jvmObj) as Double
            "longitude" -> getLongitude.invoke(jvmObj) as Double
            else -> null
        }
    }
}

private val documentKeys = setOf("path")

private fun makeDocumentReferenceJavaReflectionDecoder(decodeDouble: (value: Any?) -> Double?, jvmObj: Any): CompositeDecoder {
    val timestampClass = Class.forName("com.google.firebase.firestore.DocumentReference")
    val getPath = timestampClass.getMethod("getPath")

    return FirebaseClassDecoder(
        decodeDouble,
        size = 1,
        containsKey = { documentKeys.contains(it) }
    ) { descriptor, index ->
        when (descriptor.getElementName(index)) {
            "path" -> getPath.invoke(jvmObj) as String
            else -> null
        }
    }
}

actual fun getPolymorphicType(value: Any?, discriminator: String): String =
    (value as Map<*,*>)[discriminator] as String
