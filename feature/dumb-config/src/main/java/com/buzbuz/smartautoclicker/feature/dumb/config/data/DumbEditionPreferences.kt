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
package com.buzbuz.smartautoclicker.feature.dumb.config.data

import android.content.Context
import android.content.SharedPreferences


/** @return the shared preferences for the default configuration. */
internal fun Context.getDumbConfigPreferences(): SharedPreferences =
    getSharedPreferences(
        DUMB_CONFIG_PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

/** @return the default duration for a click press. */
internal fun SharedPreferences.getClickPressDurationConfig(default: Long) : Long =
    getLong(PREF_LAST_CLICK_PRESS_DURATION, default)

/** Save a new default duration for the click press. */
internal fun SharedPreferences.Editor.putClickPressDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_CLICK_PRESS_DURATION, durationMs)

/** @return the default repeat count for a click. */
internal fun SharedPreferences.getClickRepeatCountConfig(default: Int) : Int =
    getInt(PREF_LAST_CLICK_REPEAT_COUNT, default)

/** Save a new default repeat count for the clicks. */
internal fun SharedPreferences.Editor.putClickRepeatCountConfig(count: Int) : SharedPreferences.Editor =
    putInt(PREF_LAST_CLICK_REPEAT_COUNT, count)

/** @return the default repeat delay for a click. */
internal fun SharedPreferences.getClickRepeatDelayConfig(default: Long) : Long =
    getLong(PREF_LAST_CLICK_REPEAT_DELAY, default)

/** Save a new default repeat delay for the clicks. */
internal fun SharedPreferences.Editor.putClickRepeatDelayConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_CLICK_REPEAT_DELAY, durationMs)


/** @return the default duration for a swipe. */
internal fun SharedPreferences.getSwipeDurationConfig(default: Long) : Long =
    getLong(PREF_LAST_SWIPE_DURATION, default)

/** Save a new default duration for the swipes. */
internal fun SharedPreferences.Editor.putSwipeDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_SWIPE_DURATION, durationMs)

/** @return the default repeat count for a swipe. */
internal fun SharedPreferences.getSwipeRepeatCountConfig(default: Int) : Int =
    getInt(PREF_LAST_SWIPE_REPEAT_COUNT, default)

/** Save a new default repeat count for the swipes. */
internal fun SharedPreferences.Editor.putSwipeRepeatCountConfig(count: Int) : SharedPreferences.Editor =
    putInt(PREF_LAST_SWIPE_REPEAT_COUNT, count)

/** @return the default repeat delay for a swipe. */
internal fun SharedPreferences.getSwipeRepeatDelayConfig(default: Long) : Long =
    getLong(PREF_LAST_SWIPE_REPEAT_DELAY, default)

/** Save a new default repeat delay for the swipes. */
internal fun SharedPreferences.Editor.putSwipeRepeatDelayConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_SWIPE_REPEAT_DELAY, durationMs)


/** @return the default duration for a pause. */
internal fun SharedPreferences.getPauseDurationConfig(default: Long) : Long =
    getLong(PREF_LAST_PAUSE_DURATION, default)

internal fun SharedPreferences.getLinkDurationConfig(default: Long) : Long =
    getLong(PREF_LAST_LINK_DURATION, default)

internal fun SharedPreferences.getLinkUrlConfig(default: String) : String = getString(PREF_LAST_LINK_URL, default)!!

/** Save a new default duration for the pause. */
internal fun SharedPreferences.Editor.putPauseDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_PAUSE_DURATION, durationMs)

internal fun SharedPreferences.Editor.putLinkDurationConfig(durationMs: Long) : SharedPreferences.Editor =
    putLong(PREF_LAST_LINK_DURATION, durationMs)

internal fun SharedPreferences.Editor.putLinkUrlConfig(url: String) : SharedPreferences.Editor =
    putString(PREF_LAST_LINK_URL, url)

internal fun SharedPreferences.Editor.putURLConfig(url: String): SharedPreferences.Editor =
    putString(PREF_LAST_URL, url)

internal fun SharedPreferences.Editor.putTextConfig(text: String): SharedPreferences.Editor =
    putString(PREF_LAST_TEXT, text)

/** Name of the preference file. */
private const val DUMB_CONFIG_PREFERENCES_NAME = "DumbConfigPreferences"
/** User last click press duration key in the SharedPreferences. */
private const val PREF_LAST_CLICK_PRESS_DURATION = "Last_Click_Press_Duration"
/** User last click repeat count key in the SharedPreferences. */
private const val PREF_LAST_CLICK_REPEAT_COUNT = "Last_Click_Repeat_Count"
/** User last click repeat delay key in the SharedPreferences. */
private const val PREF_LAST_CLICK_REPEAT_DELAY = "Last_Click_Repeat_Delay"
/** User last swipe press duration key in the SharedPreferences. */
private const val PREF_LAST_SWIPE_DURATION = "Last_Swipe_Duration"
/** User last swipe repeat count key in the SharedPreferences. */
private const val PREF_LAST_SWIPE_REPEAT_COUNT = "Last_Swipe_Repeat_Count"
/** User last swipe repeat delay key in the SharedPreferences. */
private const val PREF_LAST_SWIPE_REPEAT_DELAY = "Last_Swipe_Repeat_Delay"
/** User last pause press duration key in the SharedPreferences. */
private const val PREF_LAST_PAUSE_DURATION = "Last_Pause_Duration"
/** User change url key in SharedPreferences */
private const val PREF_LAST_TEXT = "Last_Text"
private const val PREF_LAST_URL = "Last_Url"
private const val PREF_LAST_LINK_DURATION = "Last_Link_Duration"
private const val PREF_LAST_LINK_URL = "Last_Link_Url"