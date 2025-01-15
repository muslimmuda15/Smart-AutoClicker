package com.buzbuz.smartautoclicker.activity.list.model

import kotlinx.serialization.Serializable

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