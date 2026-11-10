package dev.aaa1115910.biliapi.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

@Serializable
data class TrackLoudness(
    @SerialName("measured_i") val measuredI: Double,
    @SerialName("measured_tp") val measuredTp: Double,
    @SerialName("target_i") val targetI: Double,
    @SerialName("target_tp") val targetTp: Double,
    @SerialName("measured_lra") val measuredLra: Double? = null,
    @SerialName("measured_threshold") val measuredThreshold: Double? = null,
    @SerialName("target_offset") val targetOffset: Double? = null,
) {
    /** Static gain, bounded by the measured true peak and the output headroom. */
    val gainDb: Double?
        get() {
            if (measuredI !in -100.0..0.0 || targetI !in -70.0..-5.0 ||
                measuredTp !in -100.0..12.0 || targetTp !in -9.0..0.0) return null
            return minOf(targetI - measuredI, targetTp - measuredTp).coerceIn(-20.0, 12.0)
        }

    companion object {
        fun fromJson(element: JsonElement?): TrackLoudness? {
            val fields = element as? JsonObject ?: return null
            fun number(key: String) = (fields[key] as? JsonPrimitive)?.doubleOrNull?.takeIf(Double::isFinite)
            return TrackLoudness(number("measured_i") ?: return null, number("measured_tp") ?: return null,
                number("target_i") ?: return null, number("target_tp") ?: return null,
                number("measured_lra"), number("measured_threshold"), number("target_offset"))
                .takeIf { it.gainDb != null }
        }
    }
}
