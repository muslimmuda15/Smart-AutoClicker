package com.buzbuz.smartautoclicker.activity.list.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.PrimaryKey
import com.buzbuz.smartautoclicker.core.base.interfaces.EntityWithId
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioEntity
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Scenario
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String,
    val statusCode: Int
)

@Serializable
data class DeviceScenarioWithActions(
    val scenario: Scenario,
    val dumbActions: List<DumbActionEntity>
)

@Serializable
data class CreateScenario (
    val device: CreateDeviceInfo,
    val scenario: Scenario
)

@Serializable
data class CreateDeviceInfo(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("mobile_brand")
    val mobileBrand: String,
    @SerialName("mobile_type")
    val mobileType: String,
)

@Serializable
data class DeviceInfo(
    @SerialName("device_id")
    val id: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("mobile_brand")
    val mobileBrand: String,
    @SerialName("mobile_type")
    val mobileType: String,
    val scenarios: List<DeviceScenarioWithActions>
)


@Serializable
data class ScenarioJson(
    val name: String,
    val appVersion: String,
    val mobileBrand: String,
    val mobileType: String,
    val actions: List<DumbAction>
)

@Serializable
data class DumbAction(
    val id: Long,
    val dumb_scenario_id: Long,
    val priority: Int = 0,
    val name: String,
    val type: String,
    val repeat_count: Int? = null,
    val is_repeat_infinite: Boolean? = null,
    val repeat_delay: Long? = null,
    val press_duration: Long? = null,
    val x: Int? = null,
    val y: Int? = null,
    val swipe_duration: Long? = null,
    val from_x: Int? = null,
    val from_y: Int? = null,
    val to_x: Int? = null,
    val to_y: Int? = null,
    val pause_duration: Long? = null,
    val url_value: String? = null,
    val text_copy: String? = null,
    val link_number: String? = null,
    val link_url: String? = null
)