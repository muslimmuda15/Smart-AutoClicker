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

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.provider.MediaStore
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
import java.net.HttpURLConnection
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

        val finalURL = "${_url.value}/api/job/${Build.MODEL}/${Build.DISPLAY}/done"

        Log.d(TAG, "stopDumbScenario")

        timeoutJob?.cancel()
        timeoutJob = null
        executionJob?.cancel()
        executionJob = null

        onTryCompletedListener?.invoke()
        onTryCompletedListener = null

        try {
            val connection = (URL(finalURL).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
            }
            Log.e("API", "Connection : ${connection.responseCode}")
        } catch (e: FileNotFoundException) {
            Log.e("API", "Json file not found", e)
            mainScope?.launch {
                Toast.makeText(_context, "Unable to retrieve action data. Please check the URL.", Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e("API", "Host not found", e)
            mainScope?.launch {
                Toast.makeText(
                    _context,
                    "URL host tidak dapat diakses. Cek koneksi atau alamat URL.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: java.net.ConnectException) {
            Log.e("API", "Failed to connect to server", e)
            mainScope?.launch {
                Toast.makeText(_context, "Gagal konek ke server.", Toast.LENGTH_LONG).show()
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("API", "Connection timeout", e)
            mainScope?.launch {
                Toast.makeText(_context, "Koneksi timeout, coba lagi.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("API", "Common error", e)
            mainScope?.launch {
                Toast.makeText(_context, "Unable to retrieve action data. No internet connection.", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    suspend fun takeScreenshot(completion: suspend (android.graphics.Bitmap?) -> Unit) {
        // Check permissions first before taking screenshot
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            _context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Android 6-12
            _context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (!hasPermission) {
            Log.e(TAG, "Cannot take screenshot: Storage/Media permission not granted")
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    _context, 
                    "Storage permission required to save screenshots. Please grant permission in app settings.", 
                    Toast.LENGTH_LONG
                ).show()
            }
            completion(null)
            return
        }
        
        // Trigger system screenshot
        val screenshotTaken = dumbActionExecutor?.takeScreenshot() ?: false
        
        if (!screenshotTaken) {
            completion(null)
            return
        }
        
        // Wait for screenshot to be saved
        delay(1500)
        
        // Try to get the latest screenshot from MediaStore
        try {
            val bitmap = getLatestScreenshotFromMediaStore()
            completion(bitmap)
        } catch (e: Exception) {
            android.util.Log.e("DumbEngine", "Failed to get screenshot from MediaStore", e)
            completion(null)
        }
    }

    suspend fun uploadScreenshotToServer(bitmap: Bitmap, onResult: (Boolean, String) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                // TODO: Replace with your actual server URL
                val serverUrl = "${_url.value}/api/upload?model=${Build.MODEL}&firmware=${Build.DISPLAY}"

                // Upload using multipart/form-data
                val result = ScreenshotUploader.uploadScreenshot(
                    serverUrl = serverUrl,
                    bitmap = bitmap,
                    fieldName = "image",
                    fileName = "screenshot_${System.currentTimeMillis()}.png",
                    additionalHeaders = mapOf(
                        // Add authentication headers if needed
                        // "Authorization" to "Bearer YOUR_TOKEN"
                    )
                )

                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { response ->
                            Log.d(TAG, "Server response: $response")
                            onResult(true, "Screenshot uploaded successfully!")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Upload error", error)
                            onResult(false, "Upload failed: ${error.message}")
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error uploading screenshot", e)
                    onResult(false, "Error: ${e.message}")
                }
            }
        }
    }

    private suspend fun getLatestScreenshotFromMediaStore(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            )

            val selection = """
            (
                ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR
                ${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? OR
                ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?
            )
            """.trimIndent()

            val selectionArgs = arrayOf(
                "%Screenshots%",
                "Screenshots",
                "%Screenshot%"
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            Log.d(TAG, "Querying MediaStore with selection: $selection")
            Log.d(TAG, "Selection args: ${selectionArgs.joinToString()}")

            _context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                Log.d(TAG, "Query returned cursor with ${cursor.count} items")

                if (cursor.moveToFirst()) {
                    // Log first few results for debugging
                    do {
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                        val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH))
                        } else {
                            "N/A"
                        }
                        val bucketName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                        
                        Log.d(TAG, "Image found - Name: $displayName, Path: $relativePath, Bucket: $bucketName, Date: $dateAdded")
                        
                        // Only process the first (latest) one
                        if (cursor.position == 0) {
                            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                            val id = cursor.getLong(idColumn)

                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                            Log.d(TAG, "Found screenshot contentUri = $contentUri")

                            _context.contentResolver.openInputStream(contentUri)?.use { inputStream ->
                                return@withContext BitmapFactory.decodeStream(inputStream)
                            }
                        }
                    } while (cursor.moveToNext() && cursor.position < 3) // Log max 3 items
                } else {
                    Log.d(TAG, "No screenshot found in MediaStore (cursor is empty)")
                }
            } ?: run {
                Log.e(TAG, "Query returned null cursor")
            }

            null
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: No permission to read MediaStore", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading screenshot from MediaStore: ${e.javaClass.simpleName} - ${e.message}", e)
            null
        }
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