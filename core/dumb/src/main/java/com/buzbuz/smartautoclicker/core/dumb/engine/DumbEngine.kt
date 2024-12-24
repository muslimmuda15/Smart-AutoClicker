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
package com.buzbuz.smartautoclicker.core.dumb.engine

import android.content.Context
import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.Duration.Companion.minutes

import java.io.PrintWriter
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DumbEngine @Inject constructor(
    private val dumbRepository: IDumbRepository,
): Dumpable {

    /** Execute the dumb actions. */
    private var dumbActionExecutor: DumbActionExecutor? = null

    /** Coroutine scope for the dumb scenario processing. */
    private var processingScope: CoroutineScope? = null
    /** Job for the scenario auto stop. */
    private var timeoutJob: Job? = null
    /** Job for the scenario execution. */
    private var executionJob: Job? = null
    /** Completion listener on dumb actions tries.*/
    private var onTryCompletedListener: (() -> Unit)? = null

    private val dumbScenarioDbId: MutableStateFlow<Long?> = MutableStateFlow(null)
    val dumbScenario: Flow<DumbScenario?> =
        dumbScenarioDbId.flatMapLatest { dbId ->
            if (dbId == null) return@flatMapLatest flowOf(null)
            dumbRepository.getDumbScenarioFlow(dbId)
        }

    private val _isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private fun downloadJsonTask(urlString: String): String {
        val stringBuilder = StringBuilder()
        URL(urlString).openStream().use {
            BufferedReader(InputStreamReader(it)).use { reader ->
                var line: String?
                while (reader.readLine().also { read -> line = read } != null) {
                    stringBuilder.append(line)
                }
                return stringBuilder.toString()
            }
        }
    }

    fun init(context: Context, androidExecutor: AndroidExecutor, dumbScenario: DumbScenario) {
        dumbActionExecutor = DumbActionExecutor(context, androidExecutor)
        dumbScenarioDbId.value = dumbScenario.id.databaseId

        processingScope = CoroutineScope(Dispatchers.IO)
    }

    private fun getTypeJsonToDumbAction(
        id: Identifier,
        scenarioId: Identifier,
        currentPriority: Int,
        urlFrom: String,
        json: String
    ): List<DumbAction> {
        val jsonObject = JSONObject(json)
        val dumbActions = jsonObject.getJSONArray("actions")

        val actions = ArrayList<DumbAction>()

        var mainId = id.databaseId
        var mainTempId = id.tempId
        var priority = currentPriority

        for (i in 0 until dumbActions.length()){
            val actionObject = dumbActions.getJSONObject(i)
            val type = actionObject.optString("type", "")

            val newId = Identifier(
                ++mainId,
                mainTempId?.let {
                    ++mainTempId
                } ?: run {
                    null
                }
            )

            when(type) {
                "Swipe" -> actions.add(
                    DumbAction.DumbSwipe(
                        id = newId,
                        scenarioId = scenarioId,
                        name = actionObject.optString("summary", type),
                        priority = priority++,
                        repeatCount = actionObject.optInt("repeat_count", 1),
                        isRepeatInfinite = false,
                        repeatDelayMs = actionObject.optLong("repeat_delay", 1000L),
                        fromPosition = Point(
                            actionObject.optInt("from_x"),
                            actionObject.optInt("from_y"),
                        ),
                        toPosition = Point(
                            actionObject.optInt("to_x"),
                            actionObject.optInt("to_y"),
                        ),
                        swipeDurationMs = actionObject.optLong("swipe_duration", 500L),
                    )
                )

                "Click" -> actions.add(
                    DumbAction.DumbClick(
                        id = newId,
                        scenarioId = scenarioId,
                        name = actionObject.optString("summary", type),
                        priority = priority++,
                        repeatCount = actionObject.optInt("repeat_count", 1),
                        isRepeatInfinite = false,
                        repeatDelayMs = actionObject.optLong("repeat_delay", 1000L),
                        position = Point(
                            actionObject.optInt("x"),
                            actionObject.optInt("y"),
                        ),
                        pressDurationMs = actionObject.optLong("press_duration"),
                    )
                )

                "Pause" -> actions.add(
                    DumbAction.DumbPause(
                        id = newId,
                        scenarioId = scenarioId,
                        name = actionObject.optString("summary", type),
                        priority = priority++,
                        pauseDurationMs = actionObject.optLong("pause_duration", 1000L),
                    )
                )

                "Copy" -> actions.add(
                    DumbAction.DumbTextCopy(
                        id = newId,
                        scenarioId = scenarioId,
                        name = actionObject.optString("summary"),
                        priority = priority++,
                        textCopy = actionObject.optString("text")
                    )
                )

                "Api" -> if (actionObject.optString("api_url") == urlFrom) {
                    actions.add(
                        DumbAction.DumbApi(
                            id = newId,
                            scenarioId = scenarioId,
                            name = actionObject.optString("summary"),
                            priority = priority++,
                            urlValue = actionObject.optString("api_url")
                        )
                    )
                } else {
                    val anotherJson = downloadJsonTask(actionObject.optString("api_url"))
                    actions.addAll(
                        getTypeJsonToDumbAction(
                            id = newId,
                            scenarioId = scenarioId,
                            currentPriority = priority++,
                            urlFrom = actionObject.optString("api_url"),
                            json = anotherJson
                        )
                    )
                }
            }
        }
        return actions
    }

    fun startDumbScenario() {
        if (_isRunning.value) return
        processingScope?.launch {
            dumbScenarioDbId.value?.let { dbId ->
                dumbRepository.getDumbScenario(dbId)?.let { scenario ->
                    /**
                     * Process scenario when found API
                     */
                    val dumbActions = ArrayList<DumbAction>()
                    scenario.dumbActions.forEach { dumbAction ->
                        if(dumbAction is DumbAction.DumbApi) {
                            Log.d(TAG, "Dumb action is API : $dumbAction")
                            val json = downloadJsonTask(dumbAction.urlValue)
                            Log.d(TAG, "Dumb action JSON : $json")
                            dumbActions.addAll(
                                getTypeJsonToDumbAction(
                                    id = dumbAction.id,
                                    scenarioId = dumbAction.scenarioId,
                                    currentPriority = dumbAction.priority,
                                    urlFrom = dumbAction.urlValue,
                                    json = json
                                )
                            )
                        } else {
                            dumbActions.add(dumbAction.copyWithNewPriority(dumbActions.size))
                        }
                    }
                    val newScenario = scenario.copy(dumbActions = dumbActions)
                    Log.d(TAG, "Old scenario : $scenario")
                    Log.d(TAG, "New scenario : $newScenario")
                    startEngine(newScenario)
                }
            }
        }
    }

    fun tryDumbAction(dumbAction: DumbAction, completionListener: () -> Unit) {
        Log.i(TAG, "Trying dumb action: $dumbAction")
        onTryCompletedListener = completionListener
        startEngine(dumbAction.toDumbScenarioTry())
    }

    fun stopDumbScenario() {
        if (!isRunning.value) return
        _isRunning.value = false

        Log.d(TAG, "stopDumbScenario")

        timeoutJob?.cancel()
        timeoutJob = null
        executionJob?.cancel()
        executionJob = null

        onTryCompletedListener?.invoke()
        onTryCompletedListener = null
    }

    fun release() {
        if (isRunning.value) stopDumbScenario()

        dumbScenarioDbId.value = null
        processingScope?.cancel()
        processingScope = null

        dumbActionExecutor = null
    }

    private fun startEngine(scenario: DumbScenario) {
        if (_isRunning.value || scenario.dumbActions.isEmpty()) return
        _isRunning.value = true

        Log.d(TAG, "startDumbScenario ${scenario.id} with ${scenario.dumbActions.size} actions")

        if (!scenario.isDurationInfinite) timeoutJob = startTimeoutJob(scenario.maxDurationMin)
        executionJob = startScenarioExecutionJob(scenario)
    }

    private fun startTimeoutJob(timeoutDurationMinutes: Int): Job? =
        processingScope?.launch {
            Log.d(TAG, "startTimeoutJob: timeoutDurationMinutes=$timeoutDurationMinutes")
            delay(timeoutDurationMinutes.minutes.inWholeMilliseconds)

            processingScope?.launch { stopDumbScenario() }
        }

    private fun startScenarioExecutionJob(dumbScenario: DumbScenario): Job? =
        processingScope?.launch {
            dumbScenario.repeat {
                dumbScenario.dumbActions.forEach { dumbAction ->
                    dumbActionExecutor?.executeDumbAction(dumbAction, dumbScenario.randomize)
                }
            }

            processingScope?.launch { stopDumbScenario() }
        }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* DumbEngine:")

            append(contentPrefix)
                .append("- scenarioId=${dumbScenarioDbId.value}; ")
                .append("isRunning=${isRunning.value}; ")
                .println()
        }
    }
}

private const val TAG = "DumbEngine"