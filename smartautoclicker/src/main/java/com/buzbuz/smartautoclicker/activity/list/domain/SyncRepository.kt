package com.buzbuz.smartautoclicker.activity.list.domain
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import app.amb.autoclick.BuildConfig
import com.buzbuz.smartautoclicker.activity.list.model.DeviceInfo
import com.buzbuz.smartautoclicker.activity.list.model.DeviceScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Scenario
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.sendError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

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

    fun sendUrl(url: String?): Boolean {
        val deviceInfo = mapOf(
            "device_name" to Build.DEVICE,
            "brand" to Build.BRAND,
            "model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE,
            "api_level" to Build.VERSION.SDK_INT,
            "firmware" to Build.DISPLAY
        )

        val keyboardTypes = arrayOf(
            mapOf(
                "name" to Build.MODEL + " Text",
                "type" to "Text",
                "description" to "Text Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " Number",
                "type" to "Number",
                "description" to "Number Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " Phone",
                "type" to "Phone",
                "description" to "Phone Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " Email",
                "type" to "Email",
                "description" to "Email Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " Password",
                "type" to "Password",
                "description" to "Password Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " NumberPassword",
                "type" to "NumberPassword",
                "description" to "Text Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " Uri",
                "type" to "Uri",
                "description" to "Uri Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " Decimal",
                "type" to "Decimal",
                "description" to "Decimal Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " Ascii",
                "type" to "Ascii",
                "description" to "Ascii Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
            mapOf(
                "name" to Build.MODEL + " Screen",
                "type" to "Screen",
                "description" to "Screen Keyboard",
                "is_number_as_symbol" to false,
                "firmware" to Build.DISPLAY
            ),
        )

        val requestBody = JSONObject().apply {
            put("type", "device")  // ubah sesuai kebutuhan
            put("data", JSONObject(deviceInfo))
        }.toString()

        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
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
//                CoroutineScope(Dispatchers.Main).launch {
//                    Toast.makeText(context, "Success send to URL", Toast.LENGTH_SHORT).show()
//                }
                val responseKeyboardType = connection.inputStream.bufferedReader().use { it.readText() }

                try {
                    Log.d("API", "Response Device : $responseKeyboardType")
                    val success = JSONObject(responseKeyboardType).getBoolean("success")

                    if(success){
                        Log.d("API", "Response success : $success")
                        val requestKeyboardTypeBody = JSONObject().apply {
                            put("type", "keyboard_type")  // ubah sesuai kebutuhan
                            put("data", JSONArray(keyboardTypes))
                        }.toString()

                        val connectionKeyboardType = (URL(url).openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            doOutput = false
                            useCaches = false
                            setRequestProperty("Content-Type", "application/json; charset=utf-8")
                            setRequestProperty("Cache-Control", "no-cache")
                            setRequestProperty("Pragma", "no-cache")
                            connectTimeout = 10000  // Added timeout
                            readTimeout = 10000     // Added timeout
                        }

                        connectionKeyboardType.outputStream.use { os ->
                            val input = requestKeyboardTypeBody.toByteArray(Charsets.UTF_8)
                            os.write(input)
                            os.flush()
                        }

                        val responseCodeKeyboardType = connectionKeyboardType.responseCode

                        Log.d("API", "Response code keyboard type : ${connectionKeyboardType.responseMessage}")

                        if (responseCodeKeyboardType == HttpURLConnection.HTTP_OK) {
                            val response = connectionKeyboardType.inputStream.bufferedReader().use { it.readText() }
                            val success = JSONObject(response).getBoolean("success")

                            Log.d("API", "Response keyboard type : $response")
                            return success
                        }

                        return false
                    }
                    return success
                } catch (e: JSONException){
                    Log.e("JSON", "Uncaught exception of JSON", e)
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Invalid json format. Please check and try again.", Toast.LENGTH_LONG)
                            .show()
                    }

                    return false
                }
            } else {
                Log.i("slack", "Failed send to webhook : ${connection.responseMessage}")
//                CoroutineScope(Dispatchers.Main).launch {
//                    Toast.makeText(context, "Failed send to URL", Toast.LENGTH_SHORT).show()
//                }
                return false
            }
        } catch (e: Exception) {
            e.sendError()
            return false
        }
    }
}