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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log

import androidx.annotation.DrawableRes

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Repeatable
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.dumb.config.R

/**
 * Action item.
 * @param icon the icon for the action.
 * @param name the name of the action.
 * @param detailsText the details for the action.
 * @param action the action represented by this item.
 */
data class DumbActionDetails(
    @DrawableRes val icon: Int,
    val name: String,
    val detailsText: String,
    val repeatCountText: String?,
    val haveError: Boolean,
    val action: DumbAction,
)

/** @return the [DumbActionDetails] corresponding to this action. */
fun DumbAction.toDumbActionDetails(
    context: Context,
    withPositions: Boolean = true,
    inError: Boolean = !isValid(),
): DumbActionDetails =
    when (this) {
        is DumbAction.DumbClick -> toClickDetails(context, withPositions, inError)
        is DumbAction.DumbSwipe -> toSwipeDetails(context, withPositions, inError)
        is DumbAction.DumbPause -> toPauseDetails(context, withPositions, inError)
        is DumbAction.DumbApi -> toApiDetails(context, inError)
        is DumbAction.DumbTextCopy -> toTextCopyDetails(context, inError)
        is DumbAction.DumbLink -> toLinkDetails(context, inError)
        else -> throw IllegalArgumentException("Not yet supported in dumb action details")
    }

private fun DumbAction.DumbClick.toClickDetails(
    context: Context,
    withPositions: Boolean,
    inError: Boolean
): DumbActionDetails =
    DumbActionDetails(
        icon = R.drawable.ic_click,
        name = name,
        detailsText = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            withPositions -> context.getString(
                R.string.item_desc_dumb_click_details,
                formatDuration(pressDurationMs), position.x, position.y,
            )

            else -> context.getString(
                R.string.item_desc_dumb_action_duration,
                formatDuration(pressDurationMs),
            )
        },
        repeatCountText = getRepeatDisplayText(context),
        haveError = inError,
        action = this,
    )

private fun DumbAction.DumbSwipe.toSwipeDetails(
    context: Context,
    withPositions: Boolean,
    inError: Boolean
): DumbActionDetails =
    DumbActionDetails(
        icon = R.drawable.ic_swipe,
        name = name,
        detailsText = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            withPositions -> context.getString(
                R.string.item_desc_dumb_swipe_details,
                formatDuration(swipeDurationMs), fromPosition.x, fromPosition.y, toPosition.x, toPosition.y
            )

            else -> context.getString(
                R.string.item_desc_dumb_action_duration,
                formatDuration(swipeDurationMs),
            )
        },
        repeatCountText = getRepeatDisplayText(context),
        haveError = inError,
        action = this,
    )

private fun DumbAction.DumbApi.toApiDetails(context: Context, inError: Boolean): DumbActionDetails {
    Log.d(TAG, "API LIST : ${this}")
    return DumbActionDetails(
        icon = R.drawable.ic_api,
        name = name,
        detailsText = if (inError) {
            context.getString(R.string.item_error_action_invalid_generic)
        } else "Setup API : $urlValue",
        repeatCountText = null,
        haveError = inError,
        action = this,
    )
}

private fun DumbAction.DumbTextCopy.toTextCopyDetails(context: Context, inError: Boolean): DumbActionDetails {
    Log.d(TAG, "TEXT LIST : ${this}")
    return DumbActionDetails(
        icon = R.drawable.ic_text_fields,
        name = name,
        detailsText = if (inError) {
            context.getString(R.string.item_error_action_invalid_generic)
        } else "Copied Text : $textCopy",
        repeatCountText = null,
        haveError = inError,
        action = this,
    )
}

private fun DumbAction.DumbPause.toPauseDetails(
    context: Context,
    withPositions: Boolean,
    inError: Boolean
): DumbActionDetails =
    DumbActionDetails(
        icon = R.drawable.ic_wait,
        name = name,
        detailsText = when {
            inError -> context.getString(R.string.item_error_action_invalid_generic)
            withPositions -> context.getString(
                R.string.item_desc_dumb_pause_details,
                formatDuration(pauseDurationMs)
            )

            else -> context.getString(
                R.string.item_desc_dumb_action_duration,
                formatDuration(pauseDurationMs),
            )
        },
        repeatCountText = null,
        haveError = inError,
        action = this,
    )

private fun DumbAction.DumbLink.toLinkDetails(
    context: Context,
    inError: Boolean
): DumbActionDetails =
    DumbActionDetails(
        icon = R.drawable.ic_dataset_linked,
        name = name,
        detailsText = if (inError) {
            context.getString(R.string.item_error_action_invalid_generic)
        } else {
            context.getString(
                R.string.item_desc_dumb_pause_details,
                formatDuration(linkDurationMs)
            )
        },
        repeatCountText = null,
        haveError = inError,
        action = this,
    )

private fun Repeatable.getRepeatDisplayText(context: Context): String =
    if (isRepeatInfinite) context.getString(R.string.item_desc_dumb_repeat_infinite)
    else context.getString(R.string.item_desc_dumb_repeat_count, repeatCount)