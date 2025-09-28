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
package com.buzbuz.smartautoclicker.core.dumb.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast

import com.buzbuz.smartautoclicker.core.base.DatabaseListUpdater
import com.buzbuz.smartautoclicker.core.base.extensions.mapList
import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioDao
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbResponse
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.toDomain
import com.buzbuz.smartautoclicker.core.dumb.domain.model.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getLastSyncUrl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONException
import java.io.OutputStreamWriter
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DumbScenarioDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    database: DumbDatabase,
) {
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
    private val url: MutableStateFlow<String?> = MutableStateFlow(sharedPreferences.getLastSyncUrl(context))

    private val dumbScenarioDao: DumbScenarioDao = database.dumbScenarioDao()

    /** Updater for a list of actions. */
    private val dumbActionsUpdater = DatabaseListUpdater<DumbAction, DumbActionEntity>()

    val getAllDumbScenarios: Flow<List<DumbScenario>> =
        dumbScenarioDao.getDumbScenariosWithActionsFlow()
            .mapList { it.toDomain() }

    suspend fun getDumbScenario(dbId: Long): DumbScenario? =
        dumbScenarioDao.getDumbScenariosWithAction(dbId)
            ?.toDomain()

    fun getDumbScenarioFlow(dbId: Long): Flow<DumbScenario?> =
        dumbScenarioDao.getDumbScenariosWithActionFlow(dbId)
            .map { it?.toDomain() }

    fun getAllDumbActionsExcept(scenarioDbId: Long): Flow<List<DumbAction>> =
        dumbScenarioDao.getAllDumbActionsExcept(scenarioDbId)
            .mapList { it.toDomain() }

    suspend fun addDumbScenarioGetId(scenario: DumbScenario): Long {
        val insertId = dumbScenarioDao.addDumbScenario(scenario.toEntity())
        updateDumbScenarioActions(
            scenarioDbId = insertId,
            actions = scenario.dumbActions,
        )

        Log.d(TAG, "Add dumb scenario get id $insertId")

        return insertId
    }

    suspend fun addDumbScenario(scenario: DumbScenario) {
        Log.d(TAG, "Add dumb scenario $scenario")

        updateDumbScenarioActions(
            scenarioDbId = dumbScenarioDao.addDumbScenario(scenario.toEntity()),
            actions = scenario.dumbActions,
        )
    }

    suspend fun addDumbScenarioCopy(scenarioDbId: Long, copyName: String): Long? =
        dumbScenarioDao.getDumbScenariosWithAction(scenarioDbId)?.let { scenarioWithActions ->
            addDumbScenarioCopy(scenarioWithActions, copyName)
        }

    suspend fun addDumbScenarioCopy(scenarioWithActions: DumbScenarioWithActions, copyName: String? = null): Long? {
        Log.d(TAG, "Add dumb scenario to copy ${scenarioWithActions.scenario}")

        return try {
            val scenarioId = dumbScenarioDao.addDumbScenario(
                scenarioWithActions.scenario.copy(
                    id = DATABASE_ID_INSERTION,
                    name = copyName ?: scenarioWithActions.scenario.name,
                )
            )

            dumbScenarioDao.addDumbActions(
                scenarioWithActions.dumbActions.map { dumbAction ->
                    dumbAction.copy(
                        id = DATABASE_ID_INSERTION,
                        dumbScenarioId = scenarioId,
                    )
                }
            )

            scenarioId
        } catch (ex: Exception) {
            Log.e(TAG, "Error while inserting scenario copy", ex)
            null
        }
    }

    suspend fun updateDumbScenario(scenario: DumbScenario) {
        Log.d(TAG, "Update dumb scenario data source line 107 : $scenario")
        val scenarioEntity = scenario.toEntity()
        Log.d(TAG, "Update dumb scenario entity data source line 109 : $scenarioEntity")
        dumbScenarioDao.updateDumbScenario(scenarioEntity)
        updateDumbScenarioActions(scenarioEntity.id, scenario.dumbActions)
    }

    private suspend fun updateDumbScenarioActions(scenarioDbId: Long, actions: List<DumbAction>) {
        val updater = DatabaseListUpdater<DumbAction, DumbActionEntity>()
        updater.refreshUpdateValues(
            currentEntities = dumbScenarioDao.getDumbActions(scenarioDbId),
            newItems = actions,
            mappingClosure = { action -> action.toEntity(scenarioDbId = scenarioDbId) }
        )

        Log.d(TAG, "Dumb actions updater: $dumbActionsUpdater")
        Log.d(TAG, "Dumb actions is: $actions")

        updater.executeUpdate(
            addList = dumbScenarioDao::addDumbActions,
            updateList = dumbScenarioDao::updateDumbActions,
            removeList = dumbScenarioDao::deleteDumbActions,
        )
    }

    private suspend fun deleteDumbScenarioDB(scenarioDbId: Long): Boolean {
        try {
            val response = withContext(Dispatchers.IO) {
                val connection = (URL("${url.value}/api/scenarios/$scenarioDbId")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    doOutput = true
                    useCaches = false
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("Pragma", "no-cache")
                }

                OutputStreamWriter(connection.outputStream).apply {
                    flush()
                    close()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    Log.d("API", "Failed: ${connection.responseMessage}")
                    throw Exception("Failed: ${connection.responseMessage}")
                }
            }

            val json = Json { ignoreUnknownKeys = true }

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

    suspend fun deleteDumbScenario(scenario: DumbScenario) {
        Log.d(TAG, "Delete dumb scenario $scenario")

        if (!deleteDumbScenarioDB(scenario.id.databaseId)) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Error to delete scenario",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        dumbScenarioDao.deleteDumbScenario(scenario.id.databaseId)
    }
}

private const val TAG = "DumbScenarioDataSource"