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
package com.buzbuz.smartautoclicker.activity.creation

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import android.widget.Toast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.BuildConfig

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.activity.list.model.CreateDeviceInfo
import com.buzbuz.smartautoclicker.activity.list.model.CreateScenario
import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbResponse
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getLastSyncUrl
import com.buzbuz.smartautoclicker.sendError

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class ScenarioCreationViewModel @Inject constructor(
    @ApplicationContext context: Context,
    revenueRepository: IRevenueRepository,
    private val smartRepository: IRepository,
    private val dumbRepository: IDumbRepository,
) : ViewModel() {
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
    private val url: MutableStateFlow<String?> = MutableStateFlow(sharedPreferences.getLastSyncUrl(context))

    private val _name: MutableStateFlow<String?> =
        MutableStateFlow(context.getString(R.string.default_scenario_name))
    val name: Flow<String> = _name
        .map { it ?: "" }
        .take(1)
    val nameError: Flow<Boolean> = _name
        .map { it.isNullOrEmpty() }

    private val _selectedType: MutableStateFlow<ScenarioTypeSelection> =
        MutableStateFlow(ScenarioTypeSelection.SMART)
    val scenarioTypeSelectionState: Flow<ScenarioTypeSelectionState> =
        combine(_selectedType, revenueRepository.userBillingState) { selectedType, billingState ->
            ScenarioTypeSelectionState(
                dumbItem = ScenarioTypeItem.Dumb,
                smartItem = ScenarioTypeItem.Smart,
                selectedItem = selectedType,
                showPaidLimitationWarning =
                    billingState == UserBillingState.PURCHASED && selectedType == ScenarioTypeSelection.SMART
            )
        }

    private val canBeCreated: Flow<Boolean> = _name.map { name -> !name.isNullOrEmpty() }
    private val _creationState: MutableStateFlow<CreationState> =
        MutableStateFlow(CreationState.CONFIGURING)
    val creationState: Flow<CreationState> = _creationState.combine(canBeCreated) { state, valid ->
        if (state == CreationState.CONFIGURING && !valid) CreationState.CONFIGURING_INVALID
        else state
    }

    fun setName(newName: String?) {
        _name.value = newName
    }

    fun setSelectedType(type: ScenarioTypeSelection) {
        _selectedType.value = type
    }

    fun createScenario(context: Context) {
        if (isInvalidForCreation() || _creationState.value != CreationState.CONFIGURING) return

        _creationState.value = CreationState.CREATING
        viewModelScope.launch(Dispatchers.IO) {
            when (_selectedType.value) {
                ScenarioTypeSelection.DUMB -> createDumbScenario(context)
                ScenarioTypeSelection.SMART -> createSmartScenario(context)
            }
            _creationState.value = CreationState.SAVED
        }
    }

    private suspend fun createScenarioDB(insertId: Long): Boolean {
        val scenario = CreateScenario(
            device = CreateDeviceInfo(
                deviceId = Build.ID,
                appVersion = BuildConfig.VERSION_NAME,
                mobileBrand = Build.MANUFACTURER,
                mobileType = Build.MODEL
            ),
            scenario = com.buzbuz.smartautoclicker.core.dumb.domain.model.Scenario(
                id = insertId,
                deviceId = Build.ID,
                name = _name.value!!,
                repeatCount = 1,
                isRepeatInfinite = false,
                maxDurationMin = 1,
                isDurationInfinite = true,
                randomize = false
            )
        )

        val createScenarioJSON = Json.encodeToString(
            CreateScenario.serializer(),
            scenario
        )

        val json = Json { ignoreUnknownKeys = true }

        try {
            val response = withContext(Dispatchers.IO) {
                val connection = (URL("${url.value}/api/scenarios/create")
                    .openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    useCaches = false
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("Pragma", "no-cache")
                }

                Log.d("json", "SCENARIO JSON : $createScenarioJSON")
                OutputStreamWriter(connection.outputStream).use {
                    it.write(createScenarioJSON)
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
            val parseData = json.decodeFromString<DumbResponse<DumbScenarioWithActions>>(response)
            if (parseData.success) {
                Log.d("API", "Success: $parseData")
                return true
            } else {
                Log.d("API", "Failed: $parseData")
                return false
            }

        } catch (e: Exception) {
            Log.d("API", "Error Exception: ${e.message ?: "Unknown error"}")
            e.sendError()
            return false
        } catch (e: JSONException) {
            Log.d("API", "Error JSONException: ${e.message ?: "Unknown error"}")
            e.sendError()
            return false
        }
    }

    private suspend fun createDumbScenario(context: Context) {
        val insertId = dumbRepository.addDumbScenarioGetId(
            DumbScenario(
                id = Identifier(databaseId = DATABASE_ID_INSERTION, tempId = 0L),
                name = _name.value!!,
                dumbActions = emptyList(),
                repeatCount = 1,
                isRepeatInfinite = false,
                maxDurationMin = 1,
                isDurationInfinite = true,
                randomize = false,
            )
        )

        val createScenarioDB = createScenarioDB(insertId)
        if(!createScenarioDB) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Error to insert scenario, check the internet",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /*
    private fun createDumbScenario(context: Context) {
        val createScenario = DumbScenario(
            id = Identifier(databaseId = DATABASE_ID_INSERTION, tempId = 0L),
            name = _name.value!!,
            dumbActions = emptyList(),
            repeatCount = 1,
            isRepeatInfinite = false,
            maxDurationMin = 1,
            isDurationInfinite = true,
            randomize = false,
        )

        val scenario = CreateScenario(
            device = CreateDeviceInfo(
                deviceId = Build.ID,
                appVersion = BuildConfig.VERSION_NAME,
                mobileBrand = Build.MANUFACTURER,
                mobileType = Build.MODEL
            ),
            scenario = com.buzbuz.smartautoclicker.activity.list.model.Scenario(
                id = DATABASE_ID_INSERTION,
                deviceId = Build.ID,
                name = _name.value!!,
                repeatCount = 1,
                isRepeatInfinite = false,
                maxDurationMin = 1,
                isDurationInfinite = true,
                randomize = false
            )
        )
        val createScenarioJSON = Json.encodeToString(
            CreateScenario.serializer(),
            scenario
        )

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val connection = (URL("${url.value}/api/scenarios/create")
                        .openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        useCaches = false
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setRequestProperty("Cache-Control", "no-cache")
                        setRequestProperty("Pragma", "no-cache")
                    }

                    Log.d("json", "SCENARIO JSON : $createScenarioJSON")
                    OutputStreamWriter(connection.outputStream).use {
                        it.write(createScenarioJSON)
                        it.flush()
                    }

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        Log.d("API", "Failed: ${connection.responseMessage}")
                        throw Exception("Failed: ${connection.responseMessage}")
                    }
                }

                // parsing tetap di Main thread
                val parseData = Json.decodeFromString<DumbResponse>(response)
                if (parseData.success) {
                    Log.d("API", "Success: $parseData")
                    dumbRepository.addDumbScenario(createScenario)
                } else {
                    Log.d("API", "Failed: $parseData")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to send scenario", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                Log.d("API", "Error Exception: ${e.message ?: "Unknown error"}")
                e.sendError()
            } catch (e: JSONException) {
                Log.d("API", "Error JSONException: ${e.message ?: "Unknown error"}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.sendError()
            }
        }
    }
    */

    private suspend fun createSmartScenario(context: Context) {
        smartRepository.addScenario(
            Scenario(
                id = Identifier(databaseId = DATABASE_ID_INSERTION, tempId = 0L),
                name = _name.value!!,
                detectionQuality = context.resources.getInteger(R.integer.default_detection_quality),
                randomize = false,
            )
        )
    }

    private fun isInvalidForCreation(): Boolean = _name.value.isNullOrEmpty()
}


data class ScenarioTypeSelectionState(
    val dumbItem: ScenarioTypeItem.Dumb,
    val smartItem: ScenarioTypeItem.Smart,
    val selectedItem: ScenarioTypeSelection,
    val showPaidLimitationWarning: Boolean,
)

sealed class ScenarioTypeItem(val titleRes: Int, val iconRes: Int, val descriptionText: Int) {

    data object Dumb: ScenarioTypeItem(
        titleRes = R.string.item_title_dumb_scenario,
        iconRes = R.drawable.ic_dumb,
        descriptionText = R.string.item_desc_dumb_scenario,
    )

    data object Smart: ScenarioTypeItem(
        titleRes = R.string.item_title_smart_scenario,
        iconRes = R.drawable.ic_smart,
        descriptionText = R.string.item_desc_smart_scenario,
    )
}
enum class ScenarioTypeSelection {
    DUMB,
    SMART,
}

enum class CreationState {
    CONFIGURING_INVALID,
    CONFIGURING,
    CREATING,
    SAVED,
}