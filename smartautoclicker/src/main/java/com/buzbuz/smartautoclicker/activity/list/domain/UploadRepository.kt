package com.buzbuz.smartautoclicker.activity.list.domain

import android.content.Context
import android.util.Log
import com.buzbuz.smartautoclicker.BuildConfig
import com.buzbuz.smartautoclicker.activity.list.model.DumbAction
import com.buzbuz.smartautoclicker.activity.list.model.ScenarioJson
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Singleton
class UploadRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val dumbDatabase: DumbDatabase,
) {
    suspend fun createScenarioUpload(
        smartScenarios: List<Long>,
        dumbScenarios: List<Long>,
    ){
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
                actions = scenario.dumbActions.map { action ->
                    DumbAction(
                        id = action.id,
                        dumbScenarioId = action.dumbScenarioId,
                        priority = action.priority,
                        name = action.name,
                        type = action.type.toString().lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        repeatCount = action.repeatCount,
                        repeatDelay = action.repeatDelay,
                        pressDuration = action.pressDuration,
                        x = action.x,
                        y = action.y,
                        swipeDuration = action.swipeDuration,
                        fromX = action.fromX,
                        fromY = action.fromY,
                        toX = action.toX,
                        toY = action.toY,
                        pauseDuration = action.pauseDuration,
                        urlValue = action.urlValue,
                        textCopy = action.textCopy,
                        linkNumber = action.linkNumber,
                        linkDescription = action.linkDescription
                    )
                }
            )
        }

        val json = Json.encodeToString(fixedDumbScenario)
        Log.d("json", "Fixed JSON : $json")
    }
}