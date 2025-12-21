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
 * You should have received  r a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.dumb.engine

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.widget.Toast

import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionType
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbResponse
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.util.isValidUrl
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getLastSyncUrl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileNotFoundException
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
    private lateinit var _context: Context
    /** Execute the dumb actions. */
    private var dumbActionExecutor: DumbActionExecutor? = null

    /** Coroutine scope for the dumb scenario processing. */
    private var processingScope: CoroutineScope? = null
    private var mainScope: CoroutineScope? = null
    /** Job for the scenario auto stop. */
    private var timeoutJob: Job? = null
    /** Job for the scenario execution. */
    private var executionJob: Job? = null
    /** Completion listener on dumb actions tries.*/
    private var onTryCompletedListener: (() -> Unit)? = null

    private lateinit var _url: MutableStateFlow<String?>

    private val dumbScenarioDbId: MutableStateFlow<Long?> = MutableStateFlow(null)
    val dumbScenario: Flow<DumbScenario?> =
        dumbScenarioDbId.flatMapLatest { dbId ->
            if (dbId == null) return@flatMapLatest flowOf(null)
            dumbRepository.getDumbScenarioFlow(dbId)
        }

    private val _isRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private fun downloadJsonTask(urlString: String): String? {
        val finalURL = if(urlString.lowercase() == "do job" || urlString.lowercase() == "get job") {
            // when input is get job or do job
            "${_url.value}/api/job/${Build.MODEL}/${Build.DISPLAY}/get"
        }
        else if (isValidUrl(urlString)) {
            // when input is complete url of actions
            urlString
        } else {
            // when input is keyword of scenario
            "${_url.value}/android/actions/${Build.MODEL}/${Build.DISPLAY}?keyword=${urlString.replace(" ", "%20")}"
        }
        Log.d("API", "URL String : $urlString")
        Log.d("API", "Final URL : $finalURL")
        try {
            val stringBuilder = StringBuilder()
            URL(finalURL).openStream().use {
                BufferedReader(InputStreamReader(it)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { read -> line = read } != null) {
                        stringBuilder.append(line)
                    }
                    return stringBuilder.toString()
                }
            }
        } catch (e: FileNotFoundException) {
            Log.e("API", "Json file not found", e)
            mainScope?.launch {
                Toast.makeText(_context, "Unable to retrieve action data. Please check the URL.", Toast.LENGTH_LONG)
                    .show()
            }
            return null
        } catch (e: java.net.UnknownHostException) {
            Log.e("API", "Host not found", e)
            mainScope?.launch {
                Toast.makeText(
                    _context,
                    "URL host tidak dapat diakses. Cek koneksi atau alamat URL.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return null
        } catch (e: java.net.ConnectException) {
            Log.e("API", "Failed to connect to server", e)
            mainScope?.launch {
                Toast.makeText(_context, "Gagal konek ke server.", Toast.LENGTH_LONG).show()
            }
            return null
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("API", "Connection timeout", e)
            mainScope?.launch {
                Toast.makeText(_context, "Koneksi timeout, coba lagi.", Toast.LENGTH_LONG).show()
            }
            return null
        } catch (e: Exception) {
            Log.e("API", "Common error", e)
            mainScope?.launch {
                Toast.makeText(_context, "Unable to retrieve action data. No internet connection.", Toast.LENGTH_LONG)
                    .show()
            }
            return null
        }
    }

    fun init(context: Context, androidExecutor: AndroidExecutor, dumbScenario: DumbScenario) {
        _context = context

        dumbActionExecutor = DumbActionExecutor(context, androidExecutor)
        dumbScenarioDbId.value = dumbScenario.id.databaseId

        processingScope = CoroutineScope(Dispatchers.IO)
        mainScope = CoroutineScope(Dispatchers.Main)

        val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
        _url = MutableStateFlow(sharedPreferences.getLastSyncUrl(context))
    }

    private fun getTypeJsonToDumbAction(
        id: Identifier,
        scenarioId: Identifier,
        currentPriority: Int,
        urlFrom: String,
        jsonData: String
    ): List<DumbAction> {
        try {
            val json = Json { ignoreUnknownKeys = true }
            Log.d(TAG, "jsonData : $jsonData")
//            val parseData = json.decodeFromString<DumbResponse<DumbActionEntity>>(jsonData)
            val jsonObject = JSONObject(jsonData)
            val success = jsonObject.getBoolean("success")
            if(success) {
                val dumbActions = jsonObject.getJSONArray("data")

                val actions = ArrayList<DumbAction>()

                var mainId = id.databaseId
                var mainTempId = id.tempId
                var priority = currentPriority

                for (i in 0 until dumbActions.length()) {
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

                    Log.d("API", "type : $type")

                    when (type) {
                        "SWIPE" -> actions.add(
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

                        "CLICK" -> actions.add(
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

                        "WAIT", "PAUSE" -> actions.add(
                            DumbAction.DumbPause(
                                id = newId,
                                scenarioId = scenarioId,
                                name = actionObject.optString("summary", type),
                                priority = priority++,
                                pauseDurationMs = actionObject.optLong("pause_duration", 1000L),
                            )
                        )

                        "COPY" -> actions.add(
                            DumbAction.DumbTextCopy(
                                id = newId,
                                scenarioId = scenarioId,
                                name = actionObject.optString("name"),
                                priority = priority++,
                                textCopy = actionObject.optString("text_copy")
                            )
                        )

                        "LINK" -> {
                            actions.add(
                                DumbAction.DumbLink(
                                    id = newId,
                                    scenarioId = scenarioId,
                                    name = actionObject.optString("summary", type),
                                    priority = priority++,
                                    linkDurationMs = actionObject.optLong("pause_duration", 1000L),
                                    urlValue = actionObject.optString("link_url", "")
                                )
                            )
                        }

                        "API" -> if (actionObject.optString("api_url") == urlFrom) {
                            /**
                             * When in url you have same url
                             * do not show anythink
                             */
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
                            /**
                             * If different generate json into dumb action again
                             */
                            downloadJsonTask(actionObject.optString("api_url"))?.let { anotherJson ->
                                actions.addAll(
                                    getTypeJsonToDumbAction(
                                        id = newId,
                                        scenarioId = scenarioId,
                                        currentPriority = priority++,
                                        urlFrom = actionObject.optString("api_url"),
                                        jsonData = anotherJson
                                    )
                                )
                            }
                        }
                        else -> {
                            throw IllegalArgumentException("Not supported yet")
                        }
                    }
                }
                return actions
            }
            else {
                return ArrayList()
            }
        } catch (e: JSONException){
            Log.e("JSON", "Uncaught exception of JSON", e)
            mainScope?.launch {
                Toast.makeText(_context, "Invalid action format. Please check and try again.", Toast.LENGTH_LONG)
                    .show()
            }

            return ArrayList()
        }
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
                           downloadJsonTask(dumbAction.urlValue)?.let { json ->
                               Log.d(TAG, "Dumb action JSON : $json")
                               dumbActions.addAll(
                                   getTypeJsonToDumbAction(
                                       id = dumbAction.id,
                                       scenarioId = dumbAction.scenarioId,
                                       currentPriority = dumbAction.priority,
                                       urlFrom = dumbAction.urlValue,
                                       jsonData = json
                                   )
                               )
                           }
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