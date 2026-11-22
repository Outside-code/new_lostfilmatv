package com.kraat.lostfilmnewtv.ui.guide

import com.kraat.lostfilmnewtv.data.model.SeriesGuideEpisode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit

private val releaseDateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

internal fun SeriesGuideEpisode.availabilityLabel(today: LocalDate): String {
    if (isAvailable) return listOf("Доступна", releaseDateRu).filter { it.isNotBlank() }.joinToString(" · ")
    val date = runCatching { LocalDate.parse(releaseDateRu, releaseDateFormat) }.getOrNull()
        ?: return "Ещё не вышло · дата не объявлена"
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days > 0 -> "До выхода: $days дн. · $releaseDateRu"
        days == 0L -> "Ещё не вышло · ожидается сегодня"
        else -> "Ещё не вышло · $releaseDateRu"
    }
}
