package com.buzbuz.smartautoclicker

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    val limitLineWriter = writer.toString()
        .lineSequence()
        .take(30)
        .joinToString("\n")

    return message + device + androidVersion  + appVersion + title + begin + limitLineWriter + end
}

private fun sendToSlack(ex: Throwable) {
    Log.d("exception", "Send to slack")
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
        Log.i("slack", "Failed send to slack : ${connection.responseMessage}")
    }
}

fun globalExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler { _, ex ->
        Log.d("exception", "Get global exception")
        CoroutineScope(Dispatchers.Default).launch {
            sendToSlack(ex)
        }
        Log.e("exception", "Global exception", ex)
    }
}

fun Exception.sendError(){
    CoroutineScope(Dispatchers.Default).launch {
        sendToSlack(this@sendError)
    }
}