package com.buzbuz.smartautoclicker.activity.list.model

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbActionType
import kotlinx.serialization.Serializable

@Serializable
data class ScenarioJson(
    val name: String,
    val appVersion: String,
    val actions: List<DumbAction>
)

@Serializable
data class DumbAction(
    val id: Long,
    val dumbScenarioId: Long,
    val priority: Int = 0,
    val name: String,
    val type: String,
    val repeatCount: Int? = null,
    val isRepeatInfinite: Boolean? = null,
    val repeatDelay: Long? = null,
    val pressDuration: Long? = null,
    val x: Int? = null,
    val y: Int? = null,
    val swipeDuration: Long? = null,
    val fromX: Int? = null,
    val fromY: Int? = null,
    val toX: Int? = null,
    val toY: Int? = null,
    val pauseDuration: Long? = null,
    val urlValue: String? = null,
    val textCopy: String? = null,
    val linkNumber: String? = null,
    val linkDescription: String? = null
)