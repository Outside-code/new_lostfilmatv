package com.kraat.lostfilmnewtv.data.model

import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class ScheduleMonth(
    val title: String,
    val days: List<ScheduleDay>,
)

@Serializable
data class ScheduleDay(
    @Serializable(with = ScheduleDateSerializer::class) val date: LocalDate,
    val label: String,
    val isToday: Boolean,
    val items: List<ScheduleItem>,
)

@Serializable
data class ScheduleItem(
    val title: String,
    val episodeLabel: String?,
    val targetUrl: String,
    val kind: ReleaseKind,
    val posterUrl: String? = null,
    val releaseDateLabel: String? = null,
    val episodeTitle: String? = null,
    val relativeDateLabel: String? = null,
)


internal object ScheduleDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("ScheduleDate", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeLong(value.toEpochDay())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.ofEpochDay(decoder.decodeLong())
}
