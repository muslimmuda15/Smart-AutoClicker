package com.buzbuz.smartautoclicker.activity.list.upload

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.activity.list.domain.UploadRepository
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getLastUploadUrl
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putUploadUrlConfig
import com.buzbuz.smartautoclicker.sendError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UploadRepository
): ViewModel() {
    private val stateUploadUI: MutableStateFlow<BaseUploadStateUI> = MutableStateFlow(BaseUploadStateUI(loading = false, status = StatusUploadStateUI.READY))
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
    private val url: MutableStateFlow<String?> = MutableStateFlow(sharedPreferences.getLastUploadUrl(context))

    val getStateUI: Flow<BaseUploadStateUI> = stateUploadUI

    val getLastUrl: String? = sharedPreferences.getLastUploadUrl(context)
    fun setUrl(urlName: String){
        url.value = urlName
    }

    fun saveLastUrl(isLoading: Boolean) {
        stateUploadUI.value = BaseUploadStateUI(loading = isLoading, status = StatusUploadStateUI.READY)
//        Log.d("url", "Url : ${url.value}")
        sharedPreferences.edit().putUploadUrlConfig(url.value ?: context.resources.getString(R.string.default_upload_url_server)).apply()
    }

    fun createScenarioUpload(
        smartScenarios: List<Long>,
        dumbScenarios: List<Long>,
    ){
        stateUploadUI.value = BaseUploadStateUI(loading = true, status = StatusUploadStateUI.UPLOADING)
        viewModelScope.launch {
            val scenarios = repository.createScenarioUpload(smartScenarios, dumbScenarios)
            withContext(Dispatchers.IO) {
                val status = repository.sendUrl(scenarios, url.value)
                if(status){
                    stateUploadUI.value = BaseUploadStateUI(loading = false, status = StatusUploadStateUI.COMPLETE)
                } else {
                    stateUploadUI.value = BaseUploadStateUI(loading = false, status = StatusUploadStateUI.FAILED)
                }
            }
        }
    }
}

data class BaseUploadStateUI (
    val loading: Boolean,
    val status: StatusUploadStateUI
)

enum class StatusUploadStateUI {
    READY, UPLOADING, COMPLETE, FAILED
}