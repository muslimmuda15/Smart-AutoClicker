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
package com.buzbuz.smartautoclicker.feature.dumb.config.domain

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Custom serializer for DumbAction that outputs snake_case JSON format.
 */
object DumbActionSnakeCaseSerializer : KSerializer<DumbAction> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DumbAction")

    override fun serialize(encoder: Encoder, value: DumbAction) {
        require(encoder is JsonEncoder) { "This serializer can only be used with JSON" }
        
        val jsonObject = when (value) {
            is DumbAction.DumbClick -> buildJsonObject {
                put("name", value.name)
                put("type", "click")
                put("repeat_count", value.repeatCount)
                put("repeat_delay", value.repeatDelayMs)
                put("press_duration", value.pressDurationMs)
                put("x", value.position.x)
                put("y", value.position.y)
                put("is_repeat_infinite", value.isRepeatInfinite)
                put("pause_duration", 500)
            }
            
            is DumbAction.DumbSwipe -> buildJsonObject {
                put("name", value.name)
                put("type", "swipe")
                put("repeat_count", value.repeatCount)
                put("repeat_delay", value.repeatDelayMs)
                put("from_x", value.fromPosition.x)
                put("from_y", value.fromPosition.y)
                put("to_x", value.toPosition.x)
                put("to_y", value.toPosition.y)
                put("swipe_duration", value.swipeDurationMs)
                put("is_repeat_infinite", value.isRepeatInfinite)
                put("pause_duration", 500)
            }
            
            is DumbAction.DumbPause -> buildJsonObject {
                put("name", value.name)
                put("type", "pause")
                put("pause_duration", value.pauseDurationMs)
            }
            
            is DumbAction.DumbAll -> buildJsonObject {
                put("name", value.name)
                put("type", "click") // Default type for DumbAll
                put("repeat_count", value.repeatCount)
                put("repeat_delay", value.repeatDelayMs)
                put("press_duration", value.pressDurationMs)
                put("x", value.position.x)
                put("y", value.position.y)
                put("from_x", value.fromPosition.x)
                put("from_y", value.fromPosition.y)
                put("to_x", value.toPosition.x)
                put("to_y", value.toPosition.y)
                put("swipe_duration", value.swipeDurationMs)
                put("is_repeat_infinite", value.isRepeatInfinite)
                put("pause_duration", value.pauseDurationMs)
            }

            is DumbAction.DumbApi -> buildJsonObject {
                put("name", value.name)
                put("type", "api")
                put("url_value", value.urlValue)
            }

            is DumbAction.DumbTextCopy -> buildJsonObject {
                put("name", value.name)
                put("type", "copy")
                put("text_copy", value.textCopy)
            }

            is DumbAction.DumbLink -> buildJsonObject {
                put("name", value.name)
                put("type", "link")
                put("link_url", value.urlValue)
                put("link_duration", value.linkDurationMs)
            }
        }
        
        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DumbAction {
        throw NotImplementedError("Deserialization is not supported for DumbActionSnakeCaseSerializer")
    }
}
