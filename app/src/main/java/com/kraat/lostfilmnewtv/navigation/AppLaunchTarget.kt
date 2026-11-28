package com.kraat.lostfilmnewtv.navigation

import android.content.Context
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import com.kraat.lostfilmnewtv.MainActivity

object AppLaunchTarget {
    private const val EXTRA_DETAILS_URL = "app_launch_target.details_url"

    fun createDetailsIntent(
        context: Context,
        detailsUrl: String,
    ): Intent {
        return Intent(context, MainActivity::class.java).putExtra(EXTRA_DETAILS_URL, detailsUrl)
    }

    fun parseDetailsUrl(intent: Intent?): String? {
        return intent?.getStringExtra(EXTRA_DETAILS_URL)
    }

    fun parseSettingsBackupUri(intent: Intent?): Uri? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW && it.type == "application/json" }
            ?.data?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }
}
