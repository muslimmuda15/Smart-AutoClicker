package com.buzbuz.smartautoclicker.activity.list.domain

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.buzbuz.smartautoclicker.BuildConfig
import com.buzbuz.smartautoclicker.activity.list.model.DumbAction
import com.buzbuz.smartautoclicker.activity.list.model.ScenarioJson
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.sendError
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Singleton
class UploadRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val dumbDatabase: DumbDatabase,
) {
    suspend fun createScenarioUpload(
        smartScenarios: List<Long>,
        dumbScenarios: List<Long>,
    ): String {
        if(smartScenarios.isNotEmpty()){
            Toast.makeText(context, "Smart scenario not supported yet", Toast.LENGTH_SHORT).show()
        }
        /**
         * Smart scenario not available yet
         */
        val dumbScenario: List<DumbScenarioWithActions> = dumbScenarios.mapNotNull {
            dumbDatabase.dumbScenarioDao().getDumbScenariosWithAction(it)
        }
        val fixedDumbScenario = dumbScenario.map { scenario ->
            ScenarioJson(
                name = scenario.scenario.name,
                appVersion = BuildConfig.VERSION_NAME,
                mobileBrand = Build.MANUFACTURER,
                mobileType = Build.MODEL,
                actions = scenario.dumbActions.map { action ->
                    DumbAction(
                        id = action.id,
                        dumb_scenario_id = action.dumbScenarioId,
                        priority = action.priority,
                        name = action.name,
                        type = action.type.toString().lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        repeat_count = action.repeatCount,
                        repeat_delay = action.repeatDelay,
                        press_duration = action.pressDuration,
                        x = action.x,
                        y = action.y,
                        swipe_duration = action.swipeDuration,
                        from_x = action.fromX,
                        from_y = action.fromY,
                        to_x = action.toX,
                        to_y = action.toY,
                        pause_duration = action.pauseDuration,
                        url_value = action.urlValue,
                        text_copy = action.textCopy,
                        link_url = action.linkUrl
                    )
                }
            )
        }

        return Json.encodeToString(fixedDumbScenario)
    }

    fun sendUrl(scenarios: String, url: String?): Boolean {
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                return true
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