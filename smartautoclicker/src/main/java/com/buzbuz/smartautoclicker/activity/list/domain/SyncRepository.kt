package com.buzbuz.smartautoclicker.activity.list.domain
import android.content.Context
import android.os.Build
import android.util.Log
import app.amb.autoclick.BuildConfig
import com.buzbuz.smartautoclicker.activity.list.model.DeviceInfo
import com.buzbuz.smartautoclicker.activity.list.model.DeviceScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Scenario
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.sendError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val deviceId: String? = null,
    val message: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean get() = deviceId != null && error == null
}

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val dumbDatabase: DumbDatabase,
) {
    suspend fun createScenarioSync(): String {
        /**
         * Smart scenario not available yet
         */
        val dumbScenario: List<DumbScenarioWithActions> = dumbDatabase.dumbScenarioDao().getDumbScenariosWithActions()

        val fixedDumbScenario = DeviceInfo(
            id = Build.ID,
            appVersion = BuildConfig.VERSION_NAME,
            mobileBrand = Build.MANUFACTURER,
            mobileType = Build.MODEL,
            scenarios = dumbScenario.map { sc ->
                Log.d("sync", "Request Scenario : ${sc.scenario.id} - ${sc.scenario.name}")
                DeviceScenarioWithActions(
                    scenario = Scenario(
                        id = sc.scenario.id,
                        deviceId = Build.ID,
                        name = sc.scenario.name,
                        repeatCount = sc.scenario.repeatCount,
                        isRepeatInfinite = sc.scenario.isRepeatInfinite,
                        maxDurationMin = sc.scenario.maxDurationMin,
                        isDurationInfinite = sc.scenario.isDurationInfinite,
                        randomize = sc.scenario.randomize
                    ),
                    dumbActions = sc.dumbActions
                )
            }
        )

        return Json.encodeToString(fixedDumbScenario)
    }

    suspend fun checkDevice(baseUrl: String?): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (baseUrl.isNullOrEmpty()) return@withContext false
        try {
            val urlString = "${baseUrl}/api/devices/check"

            val model = java.net.URLEncoder.encode(Build.MODEL, "UTF-8")
            val firmware = java.net.URLEncoder.encode(Build.DISPLAY, "UTF-8")
            val url = "$urlString?model=$model&firmware=$firmware"

            val fixedUrl = if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
                val prefix = if (url.startsWith("localhost") || url.startsWith("192.168.")) "http" else "https"
                "$prefix://$url"
            } else url

            val connection = (URL(fixedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            Log.d("API", "Response check device code : $url - $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("API", "Response Check Device : $responseString")
                try {
                    val jsonObject = JSONObject(responseString)
                    return@withContext jsonObject.optBoolean("success", false)
                } catch (e: JSONException) {
                    Log.e("JSON", "Uncaught exception of JSON", e)
                    return@withContext false
                }
            }
            return@withContext false
        } catch (e: Exception) {
            e.sendError()
            Log.e("API", "Exception Check Device", e)
            return@withContext false
        }
    }

    fun sendUrl(deviceId: String?, username: String, url: String?): SyncResult {
        val deviceInfo = mapOf(
            "name" to username,
            "device_name" to Build.DEVICE,
            "brand" to Build.BRAND,
            "model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE,
            "api_level" to Build.VERSION.SDK_INT,
            "firmware" to Build.DISPLAY
        )

        val requestBody = JSONObject().apply {
            if(deviceId != null){
                put("device_id", deviceId)
            }
            put("type", "device")  // ubah sesuai kebutuhan
            put("data", JSONObject(deviceInfo))
        }.toString()

        try {
            val fixedUrl = if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
                val prefix = if (url.startsWith("localhost") || url.startsWith("192.168.")) "http" else "https"
                "$prefix://$url"
            } else url

            Log.d("API", "URL is in sync in sendUrl: $fixedUrl")

            val connection = (URL(fixedUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = false
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
                connectTimeout = 10000  // Added timeout
                readTimeout = 10000     // Added timeout
            }

            connection.outputStream.use { os ->
                val input = requestBody.toByteArray(Charsets.UTF_8)
                os.write(input)
                os.flush()
            }

//            OutputStreamWriter(connection.outputStream).apply {
//                write(scenarios)
//                flush()
//                close()
//            }

            val responseCode = connection.responseCode
            Log.d("API", "Response device code : $url - $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("API", "Response Device : $responseString")
                try {
                    val jsonObject = JSONObject(responseString)
                    val success = jsonObject.optBoolean("success", false)
                    val message = jsonObject.optString("message", null)
                    val error = jsonObject.optString("error", null)

                    if (success) {
                        val returnDeviceId = jsonObject.optJSONObject("data")?.optJSONObject("device")?.optString("id")
                        return SyncResult(deviceId = returnDeviceId, message = message)
                    } else {
                        return SyncResult(error = error ?: "Unknown error")
                    }
                } catch (e: JSONException) {
                    Log.e("JSON", "Uncaught exception of JSON", e)
                    return SyncResult(error = "Invalid JSON response")
                }
                // val responseKeyboardType = connection.inputStream.bufferedReader().use { it.readText() }

                // try {
                //     Log.d("API", "Response Device : $responseKeyboardType")
                //     val success = JSONObject(responseKeyboardType).getBoolean("success")

                //     if(success){
                //         Log.d("API", "Response success : $success")
                //         val requestKeyboardTypeBody = JSONObject().apply {
                //             put("type", "keyboard_type")  // ubah sesuai kebutuhan
                //             put("data", JSONArray(keyboardTypes))
                //         }.toString()

                //         val connectionKeyboardType = (URL(url).openConnection() as HttpURLConnection).apply {
                //             requestMethod = "POST"
                //             doOutput = false
                //             useCaches = false
                //             setRequestProperty("Content-Type", "application/json; charset=utf-8")
                //             setRequestProperty("Cache-Control", "no-cache")
                //             setRequestProperty("Pragma", "no-cache")
                //             connectTimeout = 10000  // Added timeout
                //             readTimeout = 10000     // Added timeout
                //         }

                //         connectionKeyboardType.outputStream.use { os ->
                //             val input = requestKeyboardTypeBody.toByteArray(Charsets.UTF_8)
                //             os.write(input)
                //             os.flush()
                //         }

                //         val responseCodeKeyboardType = connectionKeyboardType.responseCode

                //         Log.d("API", "Response code keyboard type : ${connectionKeyboardType.responseMessage}")

                //         if (responseCodeKeyboardType == HttpURLConnection.HTTP_OK) {
                //             val response = connectionKeyboardType.inputStream.bufferedReader().use { it.readText() }
                //             val success = JSONObject(response).getBoolean("success")

                //             Log.d("API", "Response keyboard type : $response")
                //             return success
                //         }

                //         return false
                //     }
                //     return success
                // } catch (e: JSONException){
                //     Log.e("JSON", "Uncaught exception of JSON", e)
                //     CoroutineScope(Dispatchers.Main).launch {
                //         Toast.makeText(context, "Invalid json format. Please check and try again.", Toast.LENGTH_LONG)
                //             .show()
                //     }

                //     return false
                // }
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                val errorMessage = try {
                    errorStream?.let { JSONObject(it).optString("error", connection.responseMessage) }
                        ?: connection.responseMessage
                } catch (_: JSONException) {
                    connection.responseMessage
                }
                Log.i("slack", "Failed send to webhook : $errorMessage")
                return SyncResult(error = errorMessage)
            }
        } catch (e: Exception) {
            e.sendError()
            return SyncResult(error = e.message ?: "Unknown error")
        }
    }
}