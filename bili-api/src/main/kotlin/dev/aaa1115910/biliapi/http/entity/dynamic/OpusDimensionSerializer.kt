package dev.aaa1115910.biliapi.http.entity.dynamic

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/** Bilibili sends dimensions as integer, decimal or numeric string. Invalid dimensions use the UI fallback. */
object OpusDimensionSerializer : KSerializer<Int?> {
    override val descriptor = PrimitiveSerialDescriptor("OpusDimension", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Int? {
        val primitive = (decoder as JsonDecoder).decodeJsonElement() as? JsonPrimitive ?: return null
        return primitive.contentOrNull?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0 && it <= Int.MAX_VALUE }
            ?.toInt()?.coerceAtLeast(1)
    }
    override fun serialize(encoder: Encoder, value: Int?) {
        (encoder as JsonEncoder).encodeJsonElement(value?.let(::JsonPrimitive) ?: JsonNull)
    }
}

object OpusSizeSerializer : KSerializer<Double?> {
    override val descriptor = PrimitiveSerialDescriptor("OpusSize", PrimitiveKind.DOUBLE)
    override fun deserialize(decoder: Decoder): Double? = ((decoder as JsonDecoder).decodeJsonElement() as? JsonPrimitive)
        ?.contentOrNull?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    override fun serialize(encoder: Encoder, value: Double?) {
        (encoder as JsonEncoder).encodeJsonElement(value?.let(::JsonPrimitive) ?: JsonNull)
    }
}
