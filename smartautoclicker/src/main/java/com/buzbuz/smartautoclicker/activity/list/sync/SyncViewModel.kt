package com.buzbuz.smartautoclicker.activity.list.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.activity.list.domain.SyncRepository
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

@HiltViewModel
class SyncViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: SyncRepository
): ViewModel() {
    private val stateSyncUI: MutableStateFlow<BaseSyncStateUI> = MutableStateFlow(BaseSyncStateUI(loading = false, status = StatusSyncStateUI.READY))
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
    private val url: MutableStateFlow<String?> = MutableStateFlow(sharedPreferences.getLastSyncUrl(context))

    val getStateUI: Flow<BaseSyncStateUI> = stateSyncUI

    val getLastUrl: String? = sharedPreferences.getLastSyncUrl(context)
    fun setUrl(urlName: String){
        url.value = urlName
    }

    fun saveLastUrl(isLoading: Boolean) {
        stateSyncUI.value = BaseSyncStateUI(loading = isLoading, status = StatusSyncStateUI.READY)
//        Log.d("url", "Url : ${url.value}")
        sharedPreferences.edit().putSyncUrlConfig(url.value ?: context.resources.getString(R.string.default_sync_url_server)).apply()
    }

    fun createScenarioSync(){
        stateSyncUI.value = BaseSyncStateUI(loading = true, status = StatusSyncStateUI.UPLOADING)
        viewModelScope.launch {
            val scenarios = repository.createScenarioSync()
            withContext(Dispatchers.IO) {
                Log.d("sync", "Scenario : $scenarios")
                val status = repository.sendUrl(scenarios, "${url.value}/sync")
                if(status){
                    stateSyncUI.value = BaseSyncStateUI(loading = false, status = StatusSyncStateUI.COMPLETE)
                } else {
                    stateSyncUI.value = BaseSyncStateUI(loading = false, status = StatusSyncStateUI.FAILED)
                }
            }

        }
    }
}

data class BaseSyncStateUI (
    val loading: Boolean,
    val status: StatusSyncStateUI
)

enum class StatusSyncStateUI {
    READY, UPLOADING, COMPLETE, FAILED
}