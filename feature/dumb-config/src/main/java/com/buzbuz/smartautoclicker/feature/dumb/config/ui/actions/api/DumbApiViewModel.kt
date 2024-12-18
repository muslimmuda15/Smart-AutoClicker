package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.api

import android.content.Context
import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putURLConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class DumbApiViewModel @Inject constructor() : ViewModel() {
    private val _editedDumbApi: MutableStateFlow<DumbAction.DumbApi?> = MutableStateFlow(null)
    private val editedDumbApi: Flow<DumbAction.DumbApi> = _editedDumbApi.filterNotNull()

    fun setEditedDumbApi(api: DumbAction.DumbApi) {
        _editedDumbApi.value = api.copy()
    }

    fun setName(newName: String) {
        _editedDumbApi.value = _editedDumbApi.value?.copy(name = newName)
    }
    fun setUrl(newUrl: String){
        _editedDumbApi.value = _editedDumbApi.value?.copy(urlValue = newUrl)
    }

    fun getEditedDumbApi(): DumbAction.DumbApi? =
        _editedDumbApi.value

    fun saveLastConfig(context: Context) {
        _editedDumbApi.value?.let { api ->
            context.getDumbConfigPreferences()
                .edit()
                .putURLConfig(api.urlValue)
                .apply()
        }
    }

    val name: Flow<String> = editedDumbApi
        .map { it.name }
        .take(1)

    val url: Flow<String> = editedDumbApi
        .map { it.urlValue }
        .take(1)
}