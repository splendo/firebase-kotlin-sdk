package dev.gitlive.firebase

import kotlinx.serialization.modules.SerializersModule

/**
 * Settings used to configure encoding/decoding
 */
public sealed interface EncodeDecodeSettings {

    /**
     * The structure in which Polymorphic classes are to be serialized
     */
    public enum class PolymorphicStructure {

        /**
         * A [PolymorphicStructure] where the polymorphic class is serialized as a Map, with a key for `type` reserved for the polymorphic discriminator
         */
        MAP,

        /**
         * A [PolymorphicStructure] where the polymorphic class is serialized as a List, with the polymorphic discriminator as its first element and the serialized object as its second element
         */
        LIST,
    }

    /**
     * The [SerializersModule] to use for serialization. This allows for polymorphic serialization on runtime
     */
    public val serializersModule: SerializersModule

    /**
     * The [PolymorphicStructure] to use for encoding/decoding polymorphic classes
     */
    public val polymorphicStructure: PolymorphicStructure
}

/**
 * [EncodeDecodeSettings] used when encoding an object
 * @property encodeDefaults if `true` this will explicitly encode elements even if they are their default value
 */
public interface EncodeSettings : EncodeDecodeSettings {

    public val encodeDefaults: Boolean

    public interface Builder {
        public var encodeDefaults: Boolean
        public var serializersModule: SerializersModule
        public var polymorphicStructure: EncodeDecodeSettings.PolymorphicStructure
    }
}

/**
 * [EncodeDecodeSettings] used when decoding an object
 */
public interface DecodeSettings : EncodeDecodeSettings {

    public interface Builder {
        public var serializersModule: SerializersModule
        public var polymorphicStructure: EncodeDecodeSettings.PolymorphicStructure
    }
}

public interface EncodeDecodeSettingsBuilder :
    EncodeSettings.Builder,
    DecodeSettings.Builder
