package com.buzbuz.smartautoclicker.activity.list.domain

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.buzbuz.smartautoclicker.BuildConfig
import com.buzbuz.smartautoclicker.activity.list.model.DeviceInfo
import com.buzbuz.smartautoclicker.activity.list.model.DeviceScenarioWithActions
import com.buzbuz.smartautoclicker.activity.list.model.DumbAction
import com.buzbuz.smartautoclicker.activity.list.model.Scenario
import com.buzbuz.smartautoclicker.activity.list.model.ScenarioJson
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.sendError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DumbResponse(
    val success: Boolean,
    val data: List<DumbScenarioWithActions>,
    val message: String,
    val statusCode: Int
)

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

    fun sendUrl(scenarios: String, url: String?): Boolean {
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }

            OutputStreamWriter(connection.outputStream).apply {
                write(scenarios)
                flush()
                close()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
//                CoroutineScope(Dispatchers.Main).launch {
//                    Toast.makeText(context, "Success send to URL", Toast.LENGTH_SHORT).show()
//                }
                val response = connection.inputStream.bufferedReader().use { it.readText() }

                try {
                    val parseData = Json.decodeFromString<DumbResponse>(response)
//                    Log.d("API", "Response : $parseData")
                    if (parseData.success) {
//                        CoroutineScope(Dispatchers.Main).launch {
//                            Toast.makeText(context, "Success send to URL", Toast.LENGTH_SHORT).show()
//                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            parseData.data.forEach { data ->
                                dumbDatabase.dumbScenarioDao().addDumbScenario(
                                    data.scenario
                                )
                                dumbDatabase.dumbScenarioDao().addDumbActions(
                                    data.dumbActions
                                )
                            }
                        }
                        return true
                    }
                    return false
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