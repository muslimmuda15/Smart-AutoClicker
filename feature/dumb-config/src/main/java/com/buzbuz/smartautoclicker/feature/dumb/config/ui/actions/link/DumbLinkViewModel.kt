/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.link

import android.content.Context

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.AppTypeDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.TimeUnitDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.findAppropriateTimeUnit
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.formatDuration
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.toAppTypeDropDown
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.toAppTypeString
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.toDurationMs
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putLinkDescriptionConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putLinkDurationConfig
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putLinkNumberConfig

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class DumbLinkViewModel @Inject constructor() : ViewModel() {

    private val _editedDumbLink: MutableStateFlow<DumbAction.DumbLink?> = MutableStateFlow(null)
    private val editedDumbLink: Flow<DumbAction.DumbLink> = _editedDumbLink.filterNotNull()

    /** Tells if the configured dumb link is valid and can be saved. */
    val isValidDumbLink: Flow<Boolean> = _editedDumbLink
        .map { it != null && it.isValid() }

    /** The name of the link. */
    val name: Flow<String> = editedDumbLink
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = editedDumbLink
        .map { it.name.isEmpty() }

    val number: Flow<String> = editedDumbLink
        .map { it.linkNumber }
        .take(1)

    val description: Flow<String> = editedDumbLink
        .map { it.linkDescription }
        .take(1)

    val appType: Flow<AppTypeDropDownItem> = editedDumbLink
        .map { it.name.toAppTypeDropDown() }

    private val _selectedUnitItem: MutableStateFlow<TimeUnitDropDownItem> =
        MutableStateFlow(TimeUnitDropDownItem.Milliseconds)
    val selectedUnitItem: Flow<TimeUnitDropDownItem> = _selectedUnitItem

    /** The duration of the link. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val linkDuration: Flow<String> = selectedUnitItem
        .flatMapLatest { unitItem ->
            editedDumbLink
                .map { unitItem.formatDuration(it.linkDurationMs) }
                .take(1)
        }
    /** Tells if the press duration value is valid or not. */
    val linkDurationError: Flow<Boolean> = editedDumbLink
        .map { it.linkDurationMs <= 0 }

    fun setEditedDumbLink(link: DumbAction.DumbLink) {
        _selectedUnitItem.value = link.linkDurationMs.findAppropriateTimeUnit()
        _editedDumbLink.value = link.copy()
    }

    fun getEditedDumbLink(): DumbAction.DumbLink? =
        _editedDumbLink.value

    fun setName(newName: String) {
        _editedDumbLink.value = _editedDumbLink.value?.copy(name = newName)
    }

    fun setLinkNumber(newNumber: String) {
        _editedDumbLink.value = _editedDumbLink.value?.copy(linkNumber = newNumber)
    }

    fun setLinkDescription(newDesc: String) {
        _editedDumbLink.value = _editedDumbLink.value?.copy(linkDescription = newDesc)
    }

    fun setLinkDurationMs(duration: Long) {
        _editedDumbLink.value = _editedDumbLink.value?.let { oldValue ->
            val newDurationMs = duration.toDurationMs(_selectedUnitItem.value)

            if (oldValue.linkDurationMs == newDurationMs) return
            oldValue.copy(linkDurationMs = newDurationMs)
        }
    }

    fun setAppType(app: AppTypeDropDownItem) {
        _editedDumbLink.value = _editedDumbLink.value?.copy(name = app.toAppTypeString())
    }

    fun setTimeUnit(unit: TimeUnitDropDownItem) {
        _selectedUnitItem.value = unit
    }

    fun saveLastConfig(context: Context) {
        _editedDumbLink.value?.let { link ->
            context.getDumbConfigPreferences()
                .edit()
                .putLinkDurationConfig(link.linkDurationMs)
                .apply()

            context.getDumbConfigPreferences()
                .edit()
                .putLinkNumberConfig(link.linkNumber)
                .apply()

            context.getDumbConfigPreferences()
                .edit()
                .putLinkDescriptionConfig(link.linkDescription)
                .apply()
        }
    }
}