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
package com.buzbuz.smartautoclicker.core.dumb.engine

import ToastManager
import android.accessibilityservice.GestureDescription
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Path
import android.net.Uri
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.AndroidExecutor
import com.buzbuz.smartautoclicker.core.base.extensions.buildSingleStroke
import com.buzbuz.smartautoclicker.core.base.extensions.nextIntInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.nextLongInOffset
import com.buzbuz.smartautoclicker.core.base.extensions.safeLineTo
import com.buzbuz.smartautoclicker.core.base.extensions.safeMoveTo
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Repeatable
import com.buzbuz.smartautoclicker.core.dumb.util.isValidUrl
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.AppTypeDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.toAppTypeDropDown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random


internal class DumbActionExecutor(private val context: Context, private val androidExecutor: AndroidExecutor) {

    private val random: Random = Random(System.currentTimeMillis())
    private var randomize: Boolean = false

    var toast = ToastManager()

    private fun showToast(dumbName: String) {
        toast.showToast(context, dumbName)
    }

    suspend fun executeDumbAction(action: DumbAction, randomize: Boolean) {
        this.randomize = randomize
        when (action) {
            is DumbAction.DumbClick -> executeDumbClick(action)
            is DumbAction.DumbSwipe -> executeDumbSwipe(action)
            is DumbAction.DumbPause -> executeDumbPause(action)
            is DumbAction.DumbApi -> executeDumbApi(action)
            is DumbAction.DumbTextCopy -> executeDumbCopy(action)
            is DumbAction.DumbLink -> executeDumbLink(action)
        }
    }

    private suspend fun executeDumbClick(dumbClick: DumbAction.DumbClick) {
        val clickGesture = GestureDescription.Builder().buildSingleStroke(
            path = Path().apply { moveTo(dumbClick.position.x, dumbClick.position.y) },
            durationMs = dumbClick.pressDurationMs.randomizeDurationIfNeeded(),
        )

//        withContext(Dispatchers.Main) {
//            showToast(dumbClick.name)
//        }
        Log.d("action", "Click : ${dumbClick.name}")

        executeRepeatableGesture(clickGesture, dumbClick)
    }

    private suspend fun executeDumbSwipe(dumbSwipe: DumbAction.DumbSwipe) {
        val swipeGesture = GestureDescription.Builder().buildSingleStroke(
            path = Path().apply {
                moveTo(dumbSwipe.fromPosition.x, dumbSwipe.fromPosition.y)
                lineTo(dumbSwipe.toPosition.x, dumbSwipe.toPosition.y)
            },
            durationMs = dumbSwipe.swipeDurationMs.randomizeDurationIfNeeded(),
        )

//        withContext(Dispatchers.Main) {
//            showToast(dumbSwipe.name)
//        }
        Log.d("action", "Swipe : ${dumbSwipe.name}")

        executeRepeatableGesture(swipeGesture, dumbSwipe)
    }

    private suspend fun executeDumbPause(dumbPause: DumbAction.DumbPause) {
//        withContext(Dispatchers.Main) {
//            showToast(dumbPause.name)
//        }
        Log.d("action", "Pause : ${dumbPause.name}")
        delay(dumbPause.pauseDurationMs.randomizeDurationIfNeeded())
    }

    private suspend fun executeDumbApi(dumbApi: DumbAction.DumbApi) {

    }

    private suspend fun executeDumbCopy(dumbCopy: DumbAction.DumbTextCopy){
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(dumbCopy.name, dumbCopy.textCopy)
        clipboard.setPrimaryClip(clip)
//        withContext(Dispatchers.Main) {
//            showToast(dumbCopy.name)
//        }
        Log.d("action", "Copy : ${dumbCopy.name}")
    }

    private suspend fun executeDumbLink(dumbLink: DumbAction.DumbLink) {
        Log.d("action", "Link : ${dumbLink.name}")
        withContext(Dispatchers.Main) {
            if(isValidUrl(dumbLink.urlValue)){
                Log.d("action", "URL is valid")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(dumbLink.urlValue)
                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    showToast("No application can be accessed")
                }
                delay(dumbLink.linkDurationMs.randomizeDurationIfNeeded())
            } else {
                Log.d("action", "URL is not valid")
                when (dumbLink.name.toAppTypeDropDown()) {
                    is AppTypeDropDownItem.Whatsapp -> {
                        if (isAppInstalled("com.whatsapp")) {
                            val formattedNumber = dumbLink.linkNumber
                            val encodedMessage = Uri.encode(dumbLink.linkDescription)

                            // Create the WhatsApp URI
                            val uri =
                                Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=$encodedMessage")

                            // Initialize the Intent
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.whatsapp")
                                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }

                            // Launch the Intent
                            context.startActivity(intent)
                        } else {
                            showToast("Whatsapp not installed")
                            val uri = Uri.parse("market://details?id=com.whatsapp")
                            val goToMarket = Intent(Intent.ACTION_VIEW, uri).apply {
                                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(goToMarket)
                        }
                    }

                    is AppTypeDropDownItem.Telegram -> {
                        if (isAppInstalled("org.telegram.messenger")) {
                            try {
                                val userId = dumbLink.linkNumber
                                val encodedMessage = Uri.encode(dumbLink.linkDescription)
                                val uri = Uri.parse("https://t.me/$userId")

                                val telegramIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("org.telegram.messenger");
                                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(telegramIntent)
                            } catch (e: Exception) {
                                showToast("Telegram not installed")
                                val uri = Uri.parse("market://details?id=org.telegram.messenger")
                                val goToMarket = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(goToMarket)
                                Log.e("telegram", "Error when open telegram", e)
                            }
                        } else {
                            showToast("Telegram not installed")
                            val uri = Uri.parse("market://details?id=com.whatsapp")
                            val goToMarket = Intent(Intent.ACTION_VIEW, uri).apply {
                                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(goToMarket)
                        }
                    }

                    else -> throw IllegalArgumentException("Not yet supported in base smart action link app")
                }
                delay(dumbLink.linkDurationMs.randomizeDurationIfNeeded())
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        val pm: PackageManager = context.packageManager
        var appInstalled = false
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            appInstalled = true
        } catch (e: PackageManager.NameNotFoundException) {
            appInstalled = false
        }
        return appInstalled
    }

    private suspend fun executeRepeatableGesture(gesture: GestureDescription, repeatable: Repeatable) {
        repeatable.repeat {
            withContext(Dispatchers.Main) {
                androidExecutor.executeGesture(gesture)
            }
        }
    }
    private fun Path.moveTo(x: Int, y: Int) {
        if (!randomize) safeMoveTo(x, y)
        else safeMoveTo(
            random.nextIntInOffset(x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
            random.nextIntInOffset(y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        )
    }

    private fun Path.lineTo(x: Int, y: Int) {
        if (!randomize) safeLineTo(x, y)
        else safeLineTo(
            random.nextIntInOffset(x, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
            random.nextIntInOffset(y, RANDOMIZATION_POSITION_MAX_OFFSET_PX),
        )
    }

    private fun Long.randomizeDurationIfNeeded(): Long =
        if (randomize) random.nextLongInOffset(this, RANDOMIZATION_DURATION_MAX_OFFSET_MS)
        else this
}


private const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
private const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5L