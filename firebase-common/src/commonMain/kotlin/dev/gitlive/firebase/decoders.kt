/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

inline fun <reified T> decode(value: Any?): T = decode(value, DecodeSettings())
inline fun <reified T> decode(value: Any?, settings: DecodeSettings): T {
    val strategy = serializer<T>()
    return decode(strategy as DeserializationStrategy<T>, value, settings)
}
fun <T> decode(strategy: DeserializationStrategy<T>, value: Any?): T = decode(strategy, value, DecodeSettings())
fun <T> decode(strategy: DeserializationStrategy<T>, value: Any?, settings: DecodeSettings): T {
    require(value != null || strategy.descriptor.isNullable) { "Value was null for non-nullable type ${strategy.descriptor.serialName}" }
    return FirebaseDecoder(value, settings).decodeSerializableValue(strategy)
}
expect fun FirebaseDecoder.structureDecoder(descriptor: SerialDescriptor, polymorphicIsNested: Boolean): CompositeDecoder
expect fun getPolymorphicType(value: Any?, discriminator: String): String

class FirebaseDecoder(val value: Any?, internal val settings: DecodeSettings) : Decoder {

    constructor(value: Any?) : this(value, DecodeSettings())

    override val serializersModule: SerializersModule = settings.serializersModule

    override fun beginStructure(descriptor: SerialDescriptor) = structureDecoder(descriptor, true)

    override fun decodeString() = decodeString(value)

    override fun decodeDouble() = decodeDouble(value)

    override fun decodeLong() = decodeLong(value)

    override fun decodeByte() = decodeByte(value)

    override fun decodeFloat() = decodeFloat(value)

    override fun decodeInt() = decodeInt(value)

    override fun decodeShort() = decodeShort(value)

    override fun decodeBoolean() = decodeBoolean(value)

    override fun decodeChar() = decodeChar(value)

    override fun decodeEnum(enumDescriptor: SerialDescriptor) = decodeEnum(value, enumDescriptor)

    override fun decodeNotNullMark() = decodeNotNullMark(value)

    override fun decodeNull() = decodeNull(value)

    override fun decodeInline(descriptor: SerialDescriptor) = FirebaseDecoder(value, settings)

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return decodeSerializableValuePolymorphic(value, settings, deserializer)
    }
}

class FirebaseClassDecoder(
    size: Int,
    settings: DecodeSettings,
    private val containsKey: (name: String) -> Boolean,
    get: (descriptor: SerialDescriptor, index: Int) -> Any?
) : FirebaseCompositeDecoder(size, settings, get) {
    private var index: Int = 0

    override fun decodeSequentially() = false

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return (index until descriptor.elementsCount)
            .firstOrNull {
                !descriptor.isElementOptional(it) || containsKey(
                    descriptor.getElementName(
                        it
                    )
                )
            }
            ?.also { index = it + 1 }
            ?: DECODE_DONE
    }
}

open class FirebaseCompositeDecoder(
    private val size: Int,
    internal val settings: DecodeSettings,
    private val get: (descriptor: SerialDescriptor, index: Int) -> Any?,
): CompositeDecoder {

    override val serializersModule: SerializersModule = settings.serializersModule

    override fun decodeSequentially() = true

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = throw NotImplementedError()

    override fun decodeCollectionSize(descriptor: SerialDescriptor) = size

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ) = decodeElement(descriptor, index) {
        deserializer.deserialize(FirebaseDecoder(it, settings))
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeBoolean)

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeByte)

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeChar)

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeDouble)

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeFloat)

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeInt)

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeLong)

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? {
        val isNullabilitySupported = deserializer.descriptor.isNullable
        return if (isNullabilitySupported || decodeElement(descriptor, index, ::decodeNotNullMark)) {
            decodeSerializableElement(descriptor, index, deserializer, previousValue)
        } else {
            decodeElement(descriptor, index, ::decodeNull)
        }
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeShort)

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int) =
        decodeElement(descriptor, index, ::decodeString)

    override fun endStructure(descriptor: SerialDescriptor) {}

    @ExperimentalSerializationApi
    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder =
        decodeElement(descriptor, index) {
            FirebaseDecoder(it, settings)
        }

    private fun <T> decodeElement(descriptor: SerialDescriptor, index: Int, decoder: (Any?) -> T): T {
        return try {
          decoder(get(descriptor, index))
        } catch (e: Exception) {
            throw SerializationException(
                message = "Exception during decoding ${descriptor.serialName} ${descriptor.getElementName(index)}",
                cause = e
            )
        }
    }
}

private fun decodeString(value: Any?) = value.toString()

private fun decodeDouble(value: Any?) = when(value) {
    is Number -> value.toDouble()
    is String -> value.toDouble()
    else -> throw SerializationException("Expected $value to be double")
}

private fun decodeLong(value: Any?) = when(value) {
    is Number -> value.toLong()
    is String -> value.toLong()
    else -> throw SerializationException("Expected $value to be long")
}

private fun decodeByte(value: Any?) = when(value) {
    is Number -> value.toByte()
    is String -> value.toByte()
    else -> throw SerializationException("Expected $value to be byte")
}

private fun decodeFloat(value: Any?) = when(value) {
    is Number -> value.toFloat()
    is String -> value.toFloat()
    else -> throw SerializationException("Expected $value to be float")
}

private fun decodeInt(value: Any?) = when(value) {
    is Number -> value.toInt()
    is String -> value.toInt()
    else -> throw SerializationException("Expected $value to be int")
}

private fun decodeShort(value: Any?) = when(value) {
    is Number -> value.toShort()
    is String -> value.toShort()
    else -> throw SerializationException("Expected $value to be short")
}

private fun decodeBoolean(value: Any?) = value as Boolean

private fun decodeChar(value: Any?) = when(value) {
    is Number -> value.toChar()
    is String -> value[0]
    else -> throw SerializationException("Expected $value to be char")
}

private fun decodeEnum(value: Any?, enumDescriptor: SerialDescriptor) = when(value) {
    is Number -> value.toInt()
    is String -> enumDescriptor.getElementIndexOrThrow(value)
    else -> throw SerializationException("Expected $value to be enum")
}

//Made internal after 1.0 stabilization
internal fun SerialDescriptor.getElementIndexOrThrow(name: String): Int {
    val index = getElementIndex(name)
    if (index == CompositeDecoder.UNKNOWN_NAME)
        throw SerializationException("$serialName does not contain element with name '$name'")
    return index
}

private fun decodeNotNullMark(value: Any?) = value != null

private fun decodeNull(value: Any?) = value as Nothing?
