package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.api

import android.content.ContentValues.TAG
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.DialogConfigDumbActionApiBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class DumbApiDialog(
    private val dumbApi: DumbAction.DumbApi,
    private val onConfirmClicked: (DumbAction.DumbApi) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbApi) -> Unit,
    private val onDismissClicked: () -> Unit,
): OverlayDialog(R.style.AppTheme) {
    private val viewModel: DumbApiViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbApiViewModel() },
    )
    private lateinit var viewBinding: DialogConfigDumbActionApiBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbApi(dumbApi)

        viewBinding = DialogConfigDumbActionApiBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText("Change API")

                buttonDismiss.setDebouncedOnClickListener { onDismissButtonClicked()}
                buttonSave.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onSaveButtonClicked() }
                }
                buttonDelete.apply {
                    visibility = View.VISIBLE
                    setDebouncedOnClickListener { onDeleteButtonClicked() }
                }
            }

            urlName.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(urlName.textField)

            urlValue.apply {
                setLabel(R.string.input_field_label_url)
                setOnTextChangedListener { viewModel.setUrl(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(255)
                )
            }
            hideSoftInputOnFocusLoss(urlValue.textField)
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        Log.d(TAG,"DIALOG CREATED")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                launch { viewModel.isValidDumbPause.collect(::updateSaveButton) }
                launch { viewModel.name.collect(viewBinding.urlName::setText) }
                launch { viewModel.url.collect(viewBinding.urlValue::setText) }
//                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
            }
        }
    }

    private fun onDismissButtonClicked() {
        Log.d(TAG,"DIALOG DISMISS")
        onDismissClicked()
        back()
    }


    private fun onDeleteButtonClicked() {
        Log.d(TAG, "PREPARE DELETE : " + viewModel.getEditedDumbApi())
        viewModel.getEditedDumbApi()?.let { editedAction ->
            Log.d(TAG,"DIALOG DELETE : ")
            onDeleteClicked(editedAction)
            back()
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.getEditedDumbApi()?.let { editedDumbClick ->
            Log.d(TAG, "Edited Dumb Api $editedDumbClick" )
            viewModel.saveLastConfig(context)
            onConfirmClicked(editedDumbClick)
            back()
        }
    }
}