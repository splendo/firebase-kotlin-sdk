package dev.gitlive.firebase

import kotlinx.serialization.modules.SerializersModule

/**
 * Settings used to configure encoding/decoding
 */
sealed interface EncodeDecodeSettings {

    /**
     * The structure in which Polymorphic classes are to be serialized
     */
    enum class PolymorphicStructure {

        /**
         * A [PolymorphicStructure] where the polymorphic class is serialized as a Map, with a key for `type` reserved for the polymorphic discriminator
         */
        MAP,

        /**
         * A [PolymorphicStructure] where the polymorphic class is serialized as a List, with the polymorphic discriminator as its first element and the serialized object as its second element
         */
        LIST
    }

    /**
     * The [SerializersModule] to use for serialization. This allows for polymorphic serialization on runtime
     */
    val serializersModule: SerializersModule

    /**
     * The [PolymorphicStructure] to use for encoding/decoding polymorphic classes
     */
    val polymorphicStructure: PolymorphicStructure
}

/**
 * [EncodeDecodeSettings] used when encoding an object
 * @property encodeDefaults if `true` this will explicitly encode elements even if they are their default value
 */
interface EncodeSettings : EncodeDecodeSettings {

    val encodeDefaults: Boolean

    interface Builder {
        var encodeDefaults: Boolean
        var serializersModule: SerializersModule
        var polymorphicStructure: EncodeDecodeSettings.PolymorphicStructure

    }
}

/**
 * [EncodeDecodeSettings] used when decoding an object
 * @param serializersModule the [SerializersModule] to use for deserialization. This allows for polymorphic serialization on runtime
 */
interface DecodeSettings : EncodeDecodeSettings {

    interface Builder {
        var serializersModule: SerializersModule
        var polymorphicStructure: EncodeDecodeSettings.PolymorphicStructure
    }
}

interface EncodeDecodeSettingsBuilder : EncodeSettings.Builder, DecodeSettings.Builder
