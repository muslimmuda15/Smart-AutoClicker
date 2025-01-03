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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.link

import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.setSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.timeUnitDropdownItems
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonEnabledState
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setError
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.AppTypeDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.appTypeDropDownItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.getSelectedItem
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.toAppTypeDropDown
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.getText
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setInputType
import com.buzbuz.smartautoclicker.core.ui.utils.MinMaxInputFilter
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.DialogConfigDumbActionLinkBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class DumbLinkDialog(
    private val dumbLink: DumbAction.DumbLink,
    private val onConfirmClicked: (DumbAction.DumbLink) -> Unit,
    private val onDeleteClicked: (DumbAction.DumbLink) -> Unit,
    private val onDismissClicked: () -> Unit,
) : OverlayDialog(R.style.AppTheme) {
    val waPattern = Regex("^[1-9]\\d{6,14}\$")

    /** The view model for this dialog. */
    private val viewModel: DumbLinkViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbLinkViewModel() },
    )
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConfigDumbActionLinkBinding

    override fun onCreateView(): ViewGroup {
        viewModel.setEditedDumbLink(dumbLink)

        viewBinding = DialogConfigDumbActionLinkBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.item_title_dumb_link)

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

//            editNameLayout.apply {
//                setLabel(R.string.input_field_label_name)
//                setOnTextChangedListener { viewModel.setName(it.toString()) }
//                textField.filters = arrayOf<InputFilter>(
//                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
//                )
//            }
            editNameLayout.setItems(
                label = context.getString(R.string.input_field_label_name),
                items = appTypeDropDownItem,
                onItemSelected = ::onItemSelected,
            )
            hideSoftInputOnFocusLoss(editNameLayout.textField)

            editLinkNumberLayout.apply {
                setLabel(R.string.item_title_dumb_action_number)
                setOnTextChangedListener {
                    viewModel.setLinkNumber(it.toString())
                    if(viewBinding.editNameLayout.getSelectedItem().toAppTypeDropDown() == AppTypeDropDownItem.Whatsapp) {
                        validateWhatsappNumber()
                    }
                }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(editLinkNumberLayout.textField)

            editLinkDescriptionLayout.apply {
                setLabel(R.string.item_title_dumb_action_description)
                setOnTextChangedListener { viewModel.setLinkDescription(it.toString()) }
                textField.filters = arrayOf<InputFilter>(
                    InputFilter.LengthFilter(context.resources.getInteger(R.integer.name_max_length))
                )
            }
            hideSoftInputOnFocusLoss(editLinkDescriptionLayout.textField)

            editLinkDurationLayout.apply {
                textField.filters = arrayOf(MinMaxInputFilter(min = 1))
                setLabel(R.string.input_field_label_link_duration)
                setOnTextChangedListener {
                    viewModel.setLinkDurationMs(if (it.isNotEmpty()) it.toString().toLong() else 0)
                }
            }
            hideSoftInputOnFocusLoss(editLinkDurationLayout.textField)

            timeUnitField.setItems(
                label = context.getString(R.string.dropdown_label_time_unit),
                items = timeUnitDropdownItems,
                onItemSelected = viewModel::setTimeUnit,
            )
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isValidDumbLink.collect(::updateSaveButton) }
                launch { viewModel.appType.collect(viewBinding.editNameLayout::setSelectedItem) }
                launch { viewModel.number.collect(::updateDumbLinkNumber) }
                launch { viewModel.description.collect(viewBinding.editLinkDescriptionLayout::setText) }
//                launch { viewModel.nameError.collect(viewBinding.editNameLayout::setError)}
                launch { viewModel.linkDuration.collect(::updateDumbLinkDuration) }
                launch { viewModel.linkDurationError.collect(viewBinding.editLinkDurationLayout::setError)}
                launch { viewModel.selectedUnitItem.collect(viewBinding.timeUnitField::setSelectedItem) }
            }
        }
    }

    private fun onSaveButtonClicked() {
        viewModel.getEditedDumbLink()?.let { editedDumbClick ->
            viewModel.saveLastConfig(context)
            onConfirmClicked(editedDumbClick)
            back()
        }
    }

    private fun onDeleteButtonClicked() {
        viewModel.getEditedDumbLink()?.let { editedAction ->
            onDeleteClicked(editedAction)
            back()
        }
    }

    private fun onDismissButtonClicked() {
        onDismissClicked()
        back()
    }

    private fun validateWhatsappNumber() {
        if(!waPattern.matches(viewBinding.editLinkNumberLayout.getText())){
            viewBinding.editLinkNumberLayout.setError(R.string.message_whatsapp_error, true)
        } else {
            viewBinding.editLinkNumberLayout.setError(false)
        }
    }

    private fun onItemSelected(app: AppTypeDropDownItem){
        viewModel.setAppType(app)
        when(app){
            is AppTypeDropDownItem.Whatsapp -> {
                viewBinding.editLinkNumberLayout.apply {
                    setLabel(R.string.item_title_dumb_action_number)
                    setInputType(InputType.TYPE_CLASS_NUMBER)
                }
                validateWhatsappNumber()
            }
            is AppTypeDropDownItem.Telegram -> {
                viewBinding.editLinkNumberLayout.apply {
                    setLabel(R.string.item_title_dumb_action_user_id)
                    setInputType(InputType.TYPE_CLASS_TEXT)
                }
            }
        }
    }

    private fun updateDumbLinkNumber(number: String){
        viewBinding.editLinkNumberLayout.setText(number,InputType.TYPE_CLASS_NUMBER )
    }

    private fun updateDumbLinkDuration(duration: String) {
        viewBinding.editLinkDurationLayout.setText(duration, InputType.TYPE_CLASS_NUMBER)
    }

    private fun updateSaveButton(isValidCondition: Boolean) {
        viewBinding.layoutTopBar.setButtonEnabledState(DialogNavigationButton.SAVE, isValidCondition)
    }
}