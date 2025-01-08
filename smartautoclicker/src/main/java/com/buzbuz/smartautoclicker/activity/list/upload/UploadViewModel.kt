package com.buzbuz.smartautoclicker.activity.list.upload

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.activity.list.domain.UploadRepository
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getLastUploadUrl
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putClickPressDurationConfig
import com.buzbuz.smartautoclicker.feature.smart.config.utils.putUploadUrlConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UploadRepository
): ViewModel() {
    private val loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()
    private val url: MutableStateFlow<String?> = MutableStateFlow(sharedPreferences.getLastUploadUrl(context))

    val getLoading: Flow<Boolean> = loading

    val getLastUrl: String? = sharedPreferences.getLastUploadUrl(context)
    fun setUrl(urlName: String){
        url.value = urlName
    }

    fun saveLastUrl(isLoading: Boolean) {
        loading.value = isLoading
        Log.d("url", "Url : ${url.value}")
        sharedPreferences.edit().putUploadUrlConfig(url.value ?: context.resources.getString(R.string.default_upload_url_server)).apply()
    }

    fun createScenarioUpload(
        smartScenarios: List<Long>,
        dumbScenarios: List<Long>,
    ){
        viewModelScope.launch {
            repository.createScenarioUpload(smartScenarios, dumbScenarios)
        }
    }
}