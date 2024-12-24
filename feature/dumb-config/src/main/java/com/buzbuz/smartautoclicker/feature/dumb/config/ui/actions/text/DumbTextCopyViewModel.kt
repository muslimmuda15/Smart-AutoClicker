package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.text

import android.content.Context
import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.feature.dumb.config.data.getDumbConfigPreferences
import com.buzbuz.smartautoclicker.feature.dumb.config.data.putTextConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class DumbTextCopyViewModel @Inject constructor() : ViewModel() {
    private val _editedDumbTextCopy: MutableStateFlow<DumbAction.DumbTextCopy?> = MutableStateFlow(null)
    private val editedDumbTextCopy: Flow<DumbAction.DumbTextCopy> = _editedDumbTextCopy.filterNotNull()

    fun setEditedDumbCopy(copy: DumbAction.DumbTextCopy) {
        _editedDumbTextCopy.value = copy.copy()
    }

    fun setName(newName: String) {
        _editedDumbTextCopy.value = _editedDumbTextCopy.value?.copy(name = newName)
    }
    fun setText(newText: String){
        _editedDumbTextCopy.value = _editedDumbTextCopy.value?.copy(textCopy = newText)
    }

    fun getEditedDumbCopy(): DumbAction.DumbTextCopy? =
        _editedDumbTextCopy.value

    fun saveLastConfig(context: Context) {
        _editedDumbTextCopy.value?.let { copy ->
            context.getDumbConfigPreferences()
                .edit()
                .putTextConfig(copy.textCopy)
                .apply()
        }
    }

    val name: Flow<String> = editedDumbTextCopy
        .map { it.name }
        .take(1)

    val text: Flow<String> = editedDumbTextCopy
        .map { it.textCopy }
        .take(1)
}