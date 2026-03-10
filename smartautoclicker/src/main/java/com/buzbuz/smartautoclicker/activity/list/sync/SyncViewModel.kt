package com.buzbuz.smartautoclicker.activity.list.sync

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.activity.list.domain.SyncRepository
import com.buzbuz.smartautoclicker.activity.list.domain.SyncResult
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getLastSyncUrl
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putSyncUrlConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.min
import androidx.core.content.edit
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getDeviceName
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putSyncDeviceNameConfig
import kotlin.text.startsWith

@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SyncRepository
): ViewModel() {
    private val stateSyncUI: MutableStateFlow<BaseSyncStateUI> = MutableStateFlow(BaseSyncStateUI(loading = false, status = StatusSyncStateUI.READY))
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
    private val url: MutableStateFlow<String?> = MutableStateFlow(sharedPreferences.getLastSyncUrl(context))
    private val deviceName: MutableStateFlow<String?> = MutableStateFlow(sharedPreferences.getDeviceName(context))

    val getStateUI: Flow<BaseSyncStateUI> = stateSyncUI

    val getLastUrl: String? = sharedPreferences.getLastSyncUrl(context)
    fun setUrl(urlName: String){
        val fixedUrl = if (!urlName.startsWith("http://") && !urlName.startsWith("https://")) {
            val prefix = if (urlName.startsWith("localhost") || urlName.startsWith("192.168.")) "http" else "https"
            "$prefix://$urlName"
        } else urlName
        url.value = fixedUrl
    }

    val getDeviceName: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
        sharedPreferences.getDeviceName(context) ?: Settings.Global.getString(
            context.contentResolver, Settings.Global.DEVICE_NAME.replace(" ", "")
        )
    } else {
        ""
    }

    fun setDeviceName(deviceName: String) {
        this.deviceName.value = deviceName
    }

    fun saveLastUrl(isLoading: Boolean) {
        stateSyncUI.value = BaseSyncStateUI(loading = isLoading, status = StatusSyncStateUI.READY)
//        Log.d("url", "Url : ${url.value}")
        val rawUrl = url.value ?: context.resources.getString(R.string.default_sync_url_server)
        val rawDeviceName = deviceName.value ?: ""
        sharedPreferences.edit {
            putSyncUrlConfig(rawUrl)
        }
        sharedPreferences.edit { putSyncDeviceNameConfig(rawDeviceName) }
    }

    fun createScenarioSync(username: String){
        stateSyncUI.value = BaseSyncStateUI(loading = true, status = StatusSyncStateUI.UPLOADING)
        viewModelScope.launch {
//            val scenarios = repository.createScenarioSync()
            withContext(Dispatchers.IO) {
//                Log.d("sync", "Scenario Req : $scenarios")
                val deviceIdData = sharedPreferences.getString("device_id", null)
                val result = repository.sendUrl(deviceIdData, username, url.value)
                if(result.isSuccess){
                    sharedPreferences.edit { putString("device_id", result.deviceId) }
                    stateSyncUI.value = BaseSyncStateUI(loading = false, status = StatusSyncStateUI.COMPLETE, message = result.message)
                } else {
                    stateSyncUI.value = BaseSyncStateUI(loading = false, status = StatusSyncStateUI.FAILED, message = result.error)
                }
            }

        }
    }
}

data class BaseSyncStateUI (
    val loading: Boolean,
    val status: StatusSyncStateUI,
    val message: String? = null
)

enum class StatusSyncStateUI {
    READY, UPLOADING, COMPLETE, FAILED
}