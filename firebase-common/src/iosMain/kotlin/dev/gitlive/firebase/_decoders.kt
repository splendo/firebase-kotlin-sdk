/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import platform.Foundation.*
import platform.darwin.NSObject

actual fun FirebaseDecoder.structureDecoder(descriptor: SerialDescriptor, decodeDouble: DecodeDouble): CompositeDecoder = when(descriptor.kind) {
    StructureKind.CLASS, StructureKind.OBJECT, PolymorphicKind.SEALED -> when {
        value is Map<*, *> ->
            FirebaseClassDecoder(decodeDouble, value.size, { value.containsKey(it) }) { desc, index ->
                value[desc.getElementName(index)]
            }
        value is NSObject && NSClassFromString("FIRTimestamp") == value.`class`() -> {
            makeFIRTimestampDecoder(value, decodeDouble)
        }
        value is NSObject && NSClassFromString("FIRGeoPoint") == value.`class`() -> {
            makeFIRGeoPointDecoder(value, decodeDouble)
        }
        value is NSObject && NSClassFromString("FIRDocumentReference") == value.`class`() -> {
            makeFIRDocumentReferenceDecoder(value, decodeDouble)
        }
        else -> FirebaseEmptyCompositeDecoder()
    }
    StructureKind.LIST, is PolymorphicKind -> (value as List<*>).let {
        FirebaseCompositeDecoder(decodeDouble, it.size) { _, index -> it[index] }
    }
    StructureKind.MAP -> (value as Map<*, *>).entries.toList().let {
        FirebaseCompositeDecoder(decodeDouble, it.size) { _, index -> it[index/2].run { if(index % 2 == 0) key else value }  }
    }
    else -> TODO("The firebase-kotlin-sdk does not support $descriptor for serialization yet")
}

private val timestampKeys = setOf("seconds", "nanoseconds")
private fun makeFIRTimestampDecoder(objcObj: NSObject, decodeDouble: DecodeDouble) = FirebaseClassDecoder(
    decodeDouble = decodeDouble,
    size = 2,
    containsKey = { timestampKeys.contains(it) }
) { descriptor, index ->
    objcObj.valueForKeyPath(descriptor.getElementName(index))
}

private val geoPointKeys = setOf("latitude", "longitude")
private fun makeFIRGeoPointDecoder(objcObj: NSObject, decodeDouble: DecodeDouble) = FirebaseClassDecoder(
    decodeDouble = decodeDouble,
    size = 2,
    containsKey = { geoPointKeys.contains(it) }
) { descriptor, index ->
    objcObj.valueForKeyPath(descriptor.getElementName(index))
}

private val documentKeys = setOf("path")
private fun makeFIRDocumentReferenceDecoder(objcObj: NSObject, decodeDouble: DecodeDouble) = FirebaseClassDecoder(
    decodeDouble = decodeDouble,
    size = 1,
    containsKey = { documentKeys.contains(it) }
) { descriptor, index ->
    objcObj.valueForKeyPath(descriptor.getElementName(index))
}

actual fun getPolymorphicType(value: Any?, discriminator: String): String =
    (value as Map<*,*>)[discriminator] as String

