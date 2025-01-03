package com.buzbuz.smartautoclicker

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

private fun errorMessage(writer: StringWriter): String {
    val message = "## Auto Clicker Android ${BuildConfig.BUILD_TYPE.uppercase()}\n"
    val device = "**Device** : ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n"
    val androidVersion = "**Android version** : " + android.os.Build.VERSION.SDK_INT + "\n"
    val appVersion = "**App version** :   " + BuildConfig.VERSION_NAME + "\n"
    val title = "**Fatal error** :\n"
    val begin = "```\n"
    val end = "\n```"

//    val limitLineWriter = writer.toString()
//        .lineSequence()
//        .take(30)
//        .joinToString("\n")

//    return message + device + androidVersion  + appVersion + title + begin + writer + end
    val embedContent = JSONObject().apply {
        put("title", title)
        put("description", begin + writer + end)
    }

    val embedArray = JSONArray().apply {
        put(embedContent)
    }

    val mainContent = JSONObject().apply {
        put("content", message + device + androidVersion  + appVersion + title + begin + writer + end)
//        put("embeds", embedArray)
    }
    return mainContent.toString()
}

private fun sendToWebhook(ex: Throwable) {
    Log.d("exception", "Send to webhook")
    val url = URL("https://discord.com/api/webhooks/1324585939674075176/O2779-c3CFfH_M9oQf_NAz94Z1BplVKGxdYwPHh5YNeFSaMmUEZiecF_OoKQTBTtObeZ")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }

    val writer = StringWriter()
    ex.printStackTrace(PrintWriter(writer))
//    val json = "{\"text\":\"${errorMessage(writer)}\"}"
    val json = errorMessage(writer)

    OutputStreamWriter(connection.outputStream).apply {
        write(json)
        flush()
        close()
    }

    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
        Log.i("slack", "Success send to webhook")
    } else {
        Log.i("slack", "Failed send to webhook : ${connection.responseMessage}")
    }
}

fun globalExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler { _, ex ->
        Log.d("exception", "Get global exception")
        CoroutineScope(Dispatchers.Default).launch {
            sendToWebhook(ex)
        }
        Log.e("exception", "Global exception", ex)
    }
}

fun Exception.sendError(){
    CoroutineScope(Dispatchers.Default).launch {
        sendToWebhook(this@sendError)
    }
}