package com.kraat.lostfilmnewtv.ui.details

import com.kraat.lostfilmnewtv.playback.PlaybackQualityPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailsPlaybackPreferenceResolverTest {
    @Test
    fun fullHdPreferenceSelectsFullHdTorrentUrlFromRealPageStructure() {
        val html = javaClass.classLoader!!.getResource("fixtures/torrent-options-page.html")!!.readText()
        val links = com.kraat.lostfilmnewtv.data.parser.LostFilmDetailsParser().parseTorrentLinks(html)
        val rows = links.mapIndexed { index, link -> DetailsTorrentRowUiModel("row-$index", link.label, link.url) }
        val selected = resolvePreferredTorrentRow(PlaybackQualityPreference.Q1080, rows)
        assertEquals("1080p", selected?.label)
        assertEquals("https://n.tracktor.site/td.php?s=fullhd-token", selected?.url)
    }

    @Test
    fun resolvePreferredTorrentRow_prefersExactMatch_beforeFallback() {
        val rows = listOf(
            row("r0", "1080p", "https://example.com/1080.torrent"),
            row("r1", "720p", "https://example.com/720.torrent"),
        )

        val result = resolvePreferredTorrentRow(PlaybackQualityPreference.Q720, rows)

        assertEquals("r1", result?.rowId)
    }

    @Test
    fun resolvePreferredTorrentRow_usesNearestAvailableQuality_andBreaksTiesUpward() {
        val rows = listOf(
            row("sd", "SD", "https://example.com/sd.torrent"),
            row("fullhd", "1080p", "https://example.com/1080.torrent"),
        )

        val result = resolvePreferredTorrentRow(PlaybackQualityPreference.Q720, rows)

        assertEquals("fullhd", result?.rowId)
    }

    @Test
    fun resolvePreferredTorrentRow_returnsFirstRow_whenOnlyUnknownLabelsExist() {
        val rows = listOf(
            row("unknown-0", "WEBRip", "https://example.com/webrip.torrent"),
            row("unknown-1", "Dubbed", "https://example.com/dubbed.torrent"),
        )

        val result = resolvePreferredTorrentRow(PlaybackQualityPreference.Q1080, rows)

        assertEquals("unknown-0", result?.rowId)
    }

    @Test
    fun resolvePreferredTorrentRow_returnsNull_forEmptyRows() {
        val result = resolvePreferredTorrentRow(PlaybackQualityPreference.Q1080, emptyList())

        assertNull(result)
    }

    private fun row(rowId: String, label: String, url: String): DetailsTorrentRowUiModel {
        return DetailsTorrentRowUiModel(
            rowId = rowId,
            label = label,
            url = url,
        )
    }
}
