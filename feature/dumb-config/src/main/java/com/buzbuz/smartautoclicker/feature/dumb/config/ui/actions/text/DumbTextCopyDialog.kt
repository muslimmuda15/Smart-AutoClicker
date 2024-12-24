package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.text

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
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.DialogConfigDumbActionTextCopyBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class DumbTextCopyDialog(
    private val dumbTextCopy: DumbAction.DumbTextCopy,
    private val onConfirmClicked: (DumbAction.DumbTextCopy) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbTextCopy) -> Unit,
    private val onDismissClicked: () -> Unit,
): OverlayDialog(R.style.AppTheme) {
    private val viewModel: DumbTextCopyViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbTextCopyViewModel() },
    )
    private lateinit var viewBinding: DialogConfigDumbActionTextCopyBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbCopy(dumbTextCopy)

        viewBinding = DialogConfigDumbActionTextCopyBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText("Copy Text")

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

            textCopyName.apply {
                setLabel(R.string.input_field_label_name)
                setOnTextChangedListener { viewModel.setName(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(textCopyName.textField)

            textCopyValue.apply {
                setLabel(R.string.input_field_label_text_copy)
                setOnTextChangedListener { viewModel.setText(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(255)
                )
            }
            hideSoftInputOnFocusLoss(textCopyValue.textField)
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        Log.d(TAG,"DIALOG CREATED")
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                launch { viewModel.isValidDumbPause.collect(::updateSaveButton) }
                launch { viewModel.name.collect(viewBinding.textCopyName::setText) }
                launch { viewModel.text.collect(viewBinding.textCopyValue::setText) }
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
        Log.d(TAG, "PREPARE DELETE : " + viewModel.getEditedDumbCopy())
        viewModel.getEditedDumbCopy()?.let { editedAction ->
            Log.d(TAG,"DIALOG DELETE : ")
            onDeleteClicked(editedAction)
            back()
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.getEditedDumbCopy()?.let { editedDumbClick ->
            Log.d(TAG, "Edited Dumb Api $editedDumbClick" )
            viewModel.saveLastConfig(context)
            onConfirmClicked(editedDumbClick)
            back()
        }
    }
}