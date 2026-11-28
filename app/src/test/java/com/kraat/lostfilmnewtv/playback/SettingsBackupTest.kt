package com.kraat.lostfilmnewtv.playback

import androidx.test.core.app.ApplicationProvider
import com.kraat.lostfilmnewtv.tvchannel.AndroidTvChannelMode
import com.kraat.lostfilmnewtv.updates.UpdateCheckMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsBackupTest {
    @Test
    fun settingsRoundTripWithoutCredentialsOrDeviceChannelIds() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = PlaybackPreferencesStore(context, "backup-source")
        source.writeDefaultQuality(PlaybackQualityPreference.Q720)
        source.writeHomeSeriesEnabled(false)
        source.writeHomeMenuLabelsEnabled(false)
        source.writeUpdateCheckMode(UpdateCheckMode.MANUAL)
        source.writeAndroidTvChannelMode(AndroidTvChannelMode.UNWATCHED)
        source.writeAndroidTvChannelId(128L)
        source.writeTorrServeBaseUrl("https://stream.example.test")
        source.writeTorrServeCredentials("example-user", "example-password")
        val encoded = source.settingsBackup().encode()
        assertFalse(encoded.contains("example-user"))
        assertFalse(encoded.contains("example-password"))

        val target = PlaybackPreferencesStore(context, "backup-target")
        target.writeAndroidTvChannelId(256L)
        target.writeTorrServeCredentials("previous-user", "previous-password")
        target.restoreSettingsBackup(SettingsBackup.decode(encoded))
        assertEquals(source.settingsBackup(), target.settingsBackup())
        assertEquals(256L, target.readAndroidTvChannelId())
        assertEquals("", target.readTorrServePassword())
    }

    @Test
    fun rejectsUnknownVersionsAndUserInfoInImportedAddresses() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = PlaybackPreferencesStore(context, "backup-invalid")
        val backup = store.settingsBackup()
        assertThrows(IllegalArgumentException::class.java) {
            SettingsBackup.decode(backup.copy(version = 999).encode())
        }
        assertThrows(IllegalArgumentException::class.java) {
            SettingsBackup.decode(backup.copy(torrServeUrl = "https://user:secret@stream.example.test").encode())
        }
        assertEquals(backup, store.settingsBackup())
    }
}
