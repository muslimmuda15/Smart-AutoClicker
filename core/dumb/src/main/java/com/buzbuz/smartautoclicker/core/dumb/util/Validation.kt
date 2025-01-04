package com.buzbuz.smartautoclicker.core.dumb.util

import android.net.Uri

fun isValidUrl(url: String): Boolean {
    return try {
        val uri = Uri.parse(url)
        uri.scheme in listOf("http", "https", "ftp") && uri.host != null
    } catch (e: Exception) {
        false
    }
}