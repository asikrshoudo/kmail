package com.kmail.thekami.ui.support

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/** The project's GitHub page, used for the "Star on GitHub" row. */
const val GITHUB_URL = "https://github.com/thekami-co/kmail"

/** Donation page. While this is empty the "Support this project" row stays hidden. */
const val DONATE_URL = "https://www.supportkori.com/thekamiofficial"

private const val PLAY_STORE_PACKAGE = "com.android.vending"

/** True when Google Play installed this app. GitHub downloads and adb installs return false. */
fun isInstalledFromPlayStore(context: Context): Boolean {
    val installer = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()
    return installer == PLAY_STORE_PACKAGE
}

/** Opens this app's page in the Play Store app, or in the browser if the store app is missing. */
fun openPlayListing(context: Context) {
    val id = context.packageName
    val store = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id"))
        .setPackage(PLAY_STORE_PACKAGE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(store)
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(web) }
    }
}
