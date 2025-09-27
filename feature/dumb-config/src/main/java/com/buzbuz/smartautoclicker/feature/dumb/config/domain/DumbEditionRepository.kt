/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.dumb.config.domain
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DeviceScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionType
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbResponse
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Scenario

import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getLastSyncUrl
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.serialization.json.Json
import org.json.JSONException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DumbEditionRepository @Inject constructor(
    @ApplicationContext val context: Context,
    private val dumbRepository: IDumbRepository,
) {
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
    private val url: MutableStateFlow<String?> = MutableStateFlow(sharedPreferences.getLastSyncUrl(context))

    private val _editedDumbScenario: MutableStateFlow<DumbScenario?> = MutableStateFlow(null)
    val editedDumbScenario: StateFlow<DumbScenario?> = _editedDumbScenario

    private val otherActions: Flow<List<DumbAction>> = _editedDumbScenario
        .filterNotNull()
        .flatMapLatest { dumbScenario ->
            dumbRepository.getAllDumbActionsFlowExcept(dumbScenario.id.databaseId)
        }
    val actionsToCopy: Flow<List<DumbAction>> = _editedDumbScenario
        .filterNotNull()
        .combine(otherActions) { dumbScenario, otherActions ->
            mutableListOf<DumbAction>().apply {
                addAll(dumbScenario.dumbActions)
                addAll(otherActions)
            }
        }

    /** Tells if the editions made on the scenario are synchronized with the database values. */
    val isEditionSynchronized: Flow<Boolean> = editedDumbScenario.map { it == null }

    val dumbActionBuilder: EditedDumbActionsBuilder = EditedDumbActionsBuilder()

    /** Set the scenario to be configured. */
    suspend fun startEdition(scenarioId: Long): Boolean {
        val scenario = dumbRepository.getDumbScenario(scenarioId) ?: run {
            Log.e(TAG, "Can't start edition, dumb scenario $scenarioId not found")
            return false
        }

        Log.d(TAG, "Start edition of dumb scenario $scenarioId")

        _editedDumbScenario.value = scenario
        dumbActionBuilder.startEdition(scenario.id)

        return true
    }

    private suspend fun updateScenarioDB(): Boolean {
        val scenarioToSave = _editedDumbScenario.value ?: return false

//        val scenario = Scenario(
//            id = scenarioToSave.id.databaseId,
//            deviceId = Build.ID,
//            name = scenarioToSave.name,
//            repeatCount = scenarioToSave.repeatCount,
//            isRepeatInfinite = scenarioToSave.isRepeatInfinite,
//            maxDurationMin = scenarioToSave.maxDurationMin,
//            isDurationInfinite = scenarioToSave.isDurationInfinite,
//            randomize = scenarioToSave.randomize,
//        )

        val scenarioWithAction = DeviceScenarioWithActions(
            scenario = Scenario(
            id = scenarioToSave.id.databaseId,
            deviceId = Build.ID,
            name = scenarioToSave.name,
            repeatCount = scenarioToSave.repeatCount,
            isRepeatInfinite = scenarioToSave.isRepeatInfinite,
            maxDurationMin = scenarioToSave.maxDurationMin,
            isDurationInfinite = scenarioToSave.isDurationInfinite,
            randomize = scenarioToSave.randomize,
        ), dumbActions = scenarioToSave.dumbActions.map { action ->
            Log.d("json", "ACTION : $action")
            when (action) {
                is DumbAction.DumbAll -> action.toAllEntity(scenarioToSave.id.databaseId)
                is DumbAction.DumbClick -> action.toClickEntity(scenarioToSave.id.databaseId)
                is DumbAction.DumbSwipe -> action.toSwipeEntity(scenarioToSave.id.databaseId)
                is DumbAction.DumbPause -> action.toPauseEntity(scenarioToSave.id.databaseId)
                is DumbAction.DumbApi -> action.toApiEntity(scenarioToSave.id.databaseId)
                is DumbAction.DumbTextCopy -> action.toCopyEntity(scenarioToSave.id.databaseId)
                is DumbAction.DumbLink -> action.toLinkEntity(scenarioToSave.id.databaseId)
            }
        })

        val updateScenarioJSON = Json.encodeToString(DeviceScenarioWithActions.serializer(), scenarioWithAction)
        Log.d("json", "UPDATE SCENARIO JSON : $updateScenarioJSON")

        val json = Json { ignoreUnknownKeys = true }

        try {
            val response = withContext(Dispatchers.IO) {
                val connection = (URL("${url.value}/api/scenarios/${scenarioWithAction.scenario.id}")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    useCaches = false
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("Pragma", "no-cache")
                }

                Log.d("json", "SCENARIO JSON : $updateScenarioJSON")
                OutputStreamWriter(connection.outputStream).use {
                    it.write(updateScenarioJSON)
                    it.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    Log.d("API", "Failed: ${connection.responseMessage}")
                    throw Exception("Failed: ${connection.responseMessage}")
                }
            }

            Log.d("json", "RESPONSE SCENARIO JSON : $response")
            // parsing tetap di Main thread
            val parseData = json.decodeFromString<DumbResponse>(response)
            if (parseData.success) {
                Log.d("API", "Success: $parseData")
                return true
            } else {
                Log.d("API", "Failed: $parseData")
                return false
            }
        } catch (e: Exception) {
            Log.d("API", "Error Exception: ${e.message ?: "Unknown error"}")
            return false
        } catch (e: JSONException) {
            Log.d("API", "Error JSONException: ${e.message ?: "Unknown error"}")
            return false
        }
    }

    /** Save editions changes in the database. */
    suspend fun saveEditions() {
        val scenarioToSave = _editedDumbScenario.value ?: return
        Log.d("DumbScenarioDataSource", "Save editions  in dumb edition repository line 83 : $scenarioToSave")

        if(updateScenarioDB()) {
            dumbRepository.updateDumbScenario(scenarioToSave)
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Error to update scenario, check the internet",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        stopEdition()
    }

    fun stopEdition() {
        Log.d(TAG, "Stop editions")

        _editedDumbScenario.value = null
        dumbActionBuilder.clearState()
    }

    fun updateDumbScenario(dumbScenario: DumbScenario) {
        Log.d(TAG, "Updating dumb scenario with $dumbScenario")
        _editedDumbScenario.value = dumbScenario
    }

    fun addNewDumbAction(dumbAction: DumbAction, insertionIndex: Int? = null) {
        val editedScenario = _editedDumbScenario.value ?: return

        Log.d(TAG, "Add dumb action to edited scenario $dumbAction at position $insertionIndex")
        val previousActions = editedScenario.dumbActions
        Log.d(TAG, "Prev action : $previousActions")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = previousActions.toMutableList().apply {
                if (insertionIndex == null || insertionIndex == (previousActions.lastIndex + 1)) {
                    add(dumbAction.copyWithNewPriority(previousActions.lastIndex + 1))
                    return@apply
                }

                if (insertionIndex !in editedScenario.dumbActions.indices) {
                    Log.w(TAG, "Invalid insertion index $insertionIndex")
                    return@apply
                }

                add(insertionIndex, dumbAction.copyWithNewPriority(insertionIndex))
                updatePriorities((insertionIndex + 1)..lastIndex)
            }
        )
        Log.d(TAG, "Last action : ${_editedDumbScenario.value}")
    }

    fun updateDumbAction(dumbAction: DumbAction) {
        val editedScenario = _editedDumbScenario.value ?: return
        val actionIndex = editedScenario.dumbActions.indexOfFirst { it.id == dumbAction.id }
        if (actionIndex == -1) {
            Log.w(TAG, "Can't update action, it is not in the edited scenario.")
            return
        }

        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = editedScenario.dumbActions.toMutableList().apply {
                set(actionIndex, dumbAction)
            }
        )
    }

    fun deleteDumbAction(dumbAction: DumbAction) {
        val editedScenario = _editedDumbScenario.value ?: return
        val deleteIndex = editedScenario.dumbActions.indexOfFirst { it.id == dumbAction.id }

        Log.d(TAG, "Delete dumb action from edited scenario $dumbAction at $deleteIndex")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = editedScenario.dumbActions.toMutableList().apply {
                removeAt(deleteIndex)

                // Update priority for actions after the deleted one
                if (deleteIndex > lastIndex) return@apply
                updatePriorities(deleteIndex..lastIndex)
            }
        )
    }

    fun updateDumbActions(dumbActions: List<DumbAction>) {
        val editedScenario = _editedDumbScenario.value ?: return

        Log.d(TAG, "Updating dumb action list with $dumbActions")
        _editedDumbScenario.value = editedScenario.copy(
            dumbActions = dumbActions.toMutableList().apply {
                updatePriorities()
            }
        )
    }

    private fun MutableList<DumbAction>.updatePriorities(range: IntRange = indices) {
        for (index in range) {
            Log.d(TAG, "Updating priority to $index for action ${get(index)}")
            set(index, get(index).copyWithNewPriority(index))
        }
    }
}

/** Tag for logs */
private const val TAG = "DumbEditionRepository"

// Extension functions untuk konversi DumbAction ke DumbActionEntity
private fun DumbAction.DumbAll.toAllEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, DumbAll is incomplete.")
    
    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = scenarioDbId,
        name = name,
        priority = priority,
        type = DumbActionType.CLICK, // Default to CLICK type for DumbAll
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        pressDuration = pressDurationMs,
        x = position.x,
        y = position.y,
        swipeDuration = swipeDurationMs,
        fromX = fromPosition.x,
        fromY = fromPosition.y,
        toX = toPosition.x,
        toY = toPosition.y,
        pauseDuration = pauseDurationMs,
        urlValue = urlValue,
        textCopy = textCopy,
        linkUrl = urlValue
    )
}

private fun DumbAction.DumbClick.toClickEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Click is incomplete.")
    
    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = scenarioDbId,
        name = name,
        priority = priority,
        type = DumbActionType.CLICK,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        pressDuration = pressDurationMs,
        x = position.x,
        y = position.y
    )
}

private fun DumbAction.DumbSwipe.toSwipeEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Swipe is incomplete.")
    
    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = scenarioDbId,
        name = name,
        priority = priority,
        type = DumbActionType.SWIPE,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        repeatDelay = repeatDelayMs,
        swipeDuration = swipeDurationMs,
        fromX = fromPosition.x,
        fromY = fromPosition.y,
        toX = toPosition.x,
        toY = toPosition.y
    )
}

private fun DumbAction.DumbPause.toPauseEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Pause is incomplete.")
    
    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = scenarioDbId,
        name = name,
        priority = priority,
        type = DumbActionType.PAUSE,
        pauseDuration = pauseDurationMs
    )
}

private fun DumbAction.DumbApi.toApiEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Api is incomplete.")
    
    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = scenarioDbId,
        name = name,
        priority = priority,
        type = DumbActionType.API,
        urlValue = urlValue
    )
}

private fun DumbAction.DumbTextCopy.toCopyEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Copy is incomplete.")
    
    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = scenarioDbId,
        name = name,
        priority = priority,
        type = DumbActionType.COPY,
        textCopy = textCopy
    )
}

private fun DumbAction.DumbLink.toLinkEntity(scenarioDbId: Long): DumbActionEntity {
    if (!isValid()) throw IllegalStateException("Can't transform to entity, Link is incomplete.")
    
    return DumbActionEntity(
        id = id.databaseId,
        dumbScenarioId = scenarioDbId,
        name = name,
        priority = priority,
        type = DumbActionType.LINK,
        linkUrl = urlValue,
        pauseDuration = linkDurationMs
    )
}