package com.buzbuz.smartautoclicker

import android.util.Log
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

private fun errorMessage(writer: StringWriter): String {
    val message = "Auto clicker Android ${BuildConfig.BUILD_TYPE.uppercase()}\n"
    val device = "Device : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n"
    val androidVersion = "Android version : " + android.os.Build.VERSION.SDK_INT + "\n"
    val appVersion = "App version : " + BuildConfig.VERSION_NAME + "\n"
    val title = "Fatal error :\n"
    val begin = "```\n"
    val end = "\n```"
//
    return message + device + androidVersion  + appVersion + title + begin + writer + end
}

private fun sendToSlack(ex: Throwable) {
    val url = URL("https://hooks.slack.com/services/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")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }

    val writer = StringWriter()
    ex.printStackTrace(PrintWriter(writer))
    val json = "{\"text\":\"${errorMessage(writer)}\"}"

    OutputStreamWriter(connection.outputStream).apply {
        write(json)
        flush()
        close()
    }

    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
        Log.i("slack", "Success send to slack")
    } else {
        Log.i("slack", "Failed send to slack")
    }
}

fun globalExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler { _, ex ->
        sendToSlack(ex)
    }
}

fun Exception.sendError(){
    sendToSlack(this)
}