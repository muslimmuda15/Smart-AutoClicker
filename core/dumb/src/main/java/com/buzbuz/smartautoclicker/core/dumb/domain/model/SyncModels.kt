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
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response model for dumb scenario sync operations.
 */
@Serializable
data class DumbResponse(
    val success: Boolean,
    val data: List<DumbScenarioWithActions>,
    val message: String,
    val statusCode: Int
)

/**
 * Scenario model for sync operations.
 */
@Serializable
data class Scenario(
    val id: Long,
    @SerialName("device_id")
    val deviceId: String,
    val name: String,
    @SerialName("repeat_count")
    val repeatCount: Int,
    @SerialName("is_repeat_infinite")
    val isRepeatInfinite: Boolean,
    @SerialName("max_duration_min")
    val maxDurationMin: Int,
    @SerialName("is_duration_infinite")
    val isDurationInfinite: Boolean,
    val randomize: Boolean,
)

/**
 * Device scenario with actions model for sync operations.
 */
@Serializable
data class DeviceScenarioWithActions(
    val scenario: Scenario,
    val dumbActions: List<DumbActionEntity>
)
