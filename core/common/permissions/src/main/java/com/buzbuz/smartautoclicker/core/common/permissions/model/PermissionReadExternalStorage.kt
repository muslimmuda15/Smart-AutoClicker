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
package com.buzbuz.smartautoclicker.core.common.permissions.model

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionReadExternalStorage(
    private val optional: Boolean = false,
) : Permission.Dangerous(optional), Permission.ForApiRange {

    override val fromApiLvl: Int
        get() = Build.VERSION_CODES.M

    override val toApiLvl: Int
        get() = Build.VERSION_CODES.TIRAMISU

    override val permissionString: String
        get() = Manifest.permission.READ_EXTERNAL_STORAGE

    override val fallbackSettingsIntent: Intent
        get() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", "com.buzbuz.smartautoclicker", null)
        }

    override fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == 
            PackageManager.PERMISSION_GRANTED
}
