package com.kraat.lostfilmnewtv.ui.guide

import com.kraat.lostfilmnewtv.data.model.SeriesGuideEpisode
import com.kraat.lostfilmnewtv.data.model.SeriesGuideSeason
import com.kraat.lostfilmnewtv.playback.season.playbackEpisodes
import com.kraat.lostfilmnewtv.playback.season.torrentStreamUrl
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class SeasonPlaylistTest {
    @Test fun playlistOrdersAvailableEpisodesAndExcludesUpcomingAndSeasonPacks() {
        fun episode(n: Int, available: Boolean = true) = SeriesGuideEpisode(
            "https://www.lostfilm.today/series/Test/season_1/episode_$n/", null, 1, n, null, "", false, available,
        )
        val season = SeriesGuideSeason(1, listOf(episode(4, false), episode(3), episode(1), episode(2), episode(2), episode(999)))
        assertEquals(listOf(1, 2, 3), season.playbackEpisodes().map { it.episodeNumber })
    }
    @Test fun streamSelectsFullVideoInsteadOfSubtitlesAndSample() {
        val response = """{"hash":"1234567890123456789012345678901234567890","file_stats":[
          {"id":1,"path":"Sample/sample.mkv","length":100},
          {"id":2,"path":"Show.S01E01.1080p.mkv","length":2000000},
          {"id":3,"path":"Show.srt","length":3000000}
        ]}"""
        val url = torrentStreamUrl("http://127.0.0.1:8090", response)
        assertTrue(url.contains("/stream/Show.S01E01.1080p.mkv"))
        assertTrue(url.contains("index=2"))
        assertTrue(url.contains("play="))
        assertEquals(url, torrentStreamUrl("http://127.0.0.1:8090", "[$response]"))
    }
    @Test(expected = IOException::class) fun missingVideoDoesNotProduceUnplayableLink() {
        torrentStreamUrl("http://127.0.0.1:8090", """{"hash":"1234567890123456789012345678901234567890","file_stats":[]}""")
    }
}
