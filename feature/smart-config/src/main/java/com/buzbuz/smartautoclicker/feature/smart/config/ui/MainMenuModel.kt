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
package com.buzbuz.smartautoclicker.feature.smart.config.ui

import android.content.Context
import android.util.Log
import android.view.View

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.processing.domain.DetectionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DetectionState
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject

/** View model for the [MainMenu]. */
class MainMenuModel @Inject constructor(
    private val detectionRepository: DetectionRepository,
    private val editionRepository: EditionRepository,
    private val tutorialRepository: TutorialRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
    private val debugRepository: DebuggingRepository,
) : ViewModel() {

    private val scenarioDbId: StateFlow<Long?> = detectionRepository.scenarioId
        .map { it?.databaseId }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    private var paywallResultJob: Job? = null

    /** Tells if the paywall is currently displayed. */
    val paywallIsVisible: Flow<Boolean> = flowOf(false)

    /** The current of the detection. */
    val detectionState: StateFlow<UiState> = detectionRepository.detectionState
        .map { if (it == DetectionState.DETECTING) UiState.Detecting else UiState.Idle }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState.Idle,
        )

    val nativeLibError: Flow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.ERROR_NO_NATIVE_LIB }
        .distinctUntilChanged()

    val isStartButtonEnabled: Flow<Boolean> = detectionRepository.detectionState
        .map { it != DetectionState.ERROR_NO_NATIVE_LIB }
        .distinctUntilChanged()

    val isMediaProjectionStarted: Flow<Boolean> = detectionRepository.detectionState
        .map { it == DetectionState.DETECTING || it == DetectionState.RECORDING }
        .distinctUntilChanged()

    /** Load an advertisement, if needed. Should be called before showing the paywall to reduce user waiting time. */
    fun loadAdIfNeeded(context: Context) {
        // No ads - loading not needed
    }

    /** Start/Stop the detection. */
    fun toggleDetection(context: Context) {
        when (detectionState.value) {
            UiState.Detecting -> stopDetection()
            UiState.Idle -> {
                startDetection(context)
            }
        }
    }

    private fun stopDetection() {
        viewModelScope.launch {
            detectionRepository.stopDetection()
        }
    }

    private fun startDetection(context: Context) {
        viewModelScope.launch {
            detectionRepository.startDetection(
                context,
                debugRepository.getDebugDetectionListenerIfNeeded(context),
                null,
            )
        }
    }

    fun startScenarioEdition(onEditionStarted: () -> Unit) {
        scenarioDbId.value?.let { scenarioDatabaseId ->
            viewModelScope.launch(Dispatchers.IO) {
                if (editionRepository.startEdition(scenarioDatabaseId)) {
                    withContext(Dispatchers.Main) { onEditionStarted() }
                }
            }
        }
    }

    /** Save the configured scenario in the database. */
    fun saveScenarioChanges(onCompleted: (success: Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = editionRepository.saveEditions()

            withContext(Dispatchers.Main) {
                onCompleted(result)
            }
        }
    }

    /** Cancel all changes made by the user. */
    fun cancelScenarioChanges() {
        viewModelScope.launch(Dispatchers.IO) {
            editionRepository.stopEdition()
        }
    }

    fun monitorViews(playMenuButton: View, configMenuButton: View) {
        monitoredViewsManager.apply {
            attach(MonitoredViewType.MAIN_MENU_BUTTON_PLAY, playMenuButton, ViewPositioningType.SCREEN)
            attach(MonitoredViewType.MAIN_MENU_BUTTON_CONFIG, configMenuButton, ViewPositioningType.SCREEN)
        }
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.apply {
            detach(MonitoredViewType.MAIN_MENU_BUTTON_PLAY)
            detach(MonitoredViewType.MAIN_MENU_BUTTON_CONFIG)
        }
    }

    fun shouldRestartMediaProjection(): Boolean =
        detectionState.value != UiState.Detecting

    fun shouldShowStopVolumeDownTutorialDialog(): Boolean =
        detectionState.value == UiState.Idle && tutorialRepository.shouldShowStopWithVolumeDownTutorialDialog()

    fun setStopWithVolumeDownDontShowAgain(): Unit =
        tutorialRepository.setStopWithVolumeDownDontShowAgain()

}

sealed class UiState {
    data object Detecting: UiState()
    data object Idle: UiState()
}

private const val TAG = "MainMenuViewModel"