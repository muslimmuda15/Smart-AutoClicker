package com.buzbuz.smartautoclicker.activity.list.upload

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.databinding.DialogUploadBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UploadDialog: DialogFragment() {
    private lateinit var viewBinding: DialogUploadBinding
    private val viewModel: UploadViewModel by viewModels()

    private val exportSmartScenarios: List<Long> by lazy {
        arguments?.getLongArray(FRAGMENT_ARG_KEY_SCENARIO_LIST_UPLOAD)?.toList() ?: emptyList()
    }
    private val exportDumbScenarios: List<Long> by lazy {
        arguments?.getLongArray(FRAGMENT_ARG_KEY_DUMB_SCENARIO_LIST_UPLOAD)?.toList() ?: emptyList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogUploadBinding.inflate(layoutInflater)

        viewBinding.apply {
            fieldUrlName.apply {
                setLabel(R.string.input_field_label_url)
                setText(viewModel.getLastUrl)
                setOnTextChangedListener { viewModel.setUrl(it.toString()) }
            }
            buttonUpload.setOnClickListener {
                Log.d("upload", "Dumb scenario is : $exportDumbScenarios")
                viewModel.createScenarioUpload(exportSmartScenarios, exportDumbScenarios)
                viewModel.saveLastUrl(true)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_upload_backup)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok) { dialogFragment, _ ->
                dialogFragment.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialogFragment, _ ->
                viewModel.saveLastUrl(false)
                dialogFragment.dismiss()
            }
            .create()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.getStateUI.collect { state ->
//                    viewBinding.fieldUrlName.textField.isEnabled = !state.loading
//                    viewBinding.buttonUpload.isEnabled = !state.loading
//                    viewBinding.buttonUpload.text = getString(
//                        if(state.loading)
//                            R.string.loading
//                        else
//                            R.string.item_title_upload_scenario
//                    )
//                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !state.loading

                    when(state.status) {
                        StatusUploadStateUI.READY -> {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                        }
                        StatusUploadStateUI.UPLOADING -> {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

                            viewBinding.fieldUrlName.textField.isEnabled = false
                            viewBinding.buttonUpload.isEnabled = false
                        }
                        StatusUploadStateUI.COMPLETE -> {
                            viewBinding.inputLayout.visibility = View.GONE
                            viewBinding.buttonUpload.visibility = View.GONE
                            viewBinding.layoutCompatWarning.visibility = View.GONE
                            viewBinding.iconStatus.apply {
                                setImageResource(R.drawable.img_success)
                                drawable.setTint(Color.GREEN)
                            }
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        }
                        StatusUploadStateUI.FAILED -> {
                            viewBinding.inputLayout.visibility = View.GONE
                            viewBinding.buttonUpload.visibility = View.GONE
                            viewBinding.layoutCompatWarning.visibility = View.GONE
                            viewBinding.iconStatus.apply {
                                setImageResource(R.drawable.img_error)
                                drawable.setTint(Color.RED)
                            }
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        }
                    }
                }
            }
        }

        return dialog
    }

    companion object {
        private const val FRAGMENT_ARG_KEY_SCENARIO_LIST_UPLOAD = ":backup:fragment_args_key_scenario_list_upload"
        private const val FRAGMENT_ARG_KEY_DUMB_SCENARIO_LIST_UPLOAD = ":backup:fragment_args_key_dumb_scenario_list_upload"

        fun newInstance(
            exportSmartScenarios: Collection<Long>? = null,
            exportDumbScenarios: Collection<Long>? = null,
        ): UploadDialog {
            Log.d("upload", "Show upload dialog")
            return UploadDialog().apply {
                arguments = Bundle().apply {
                    exportSmartScenarios?.let {
                        putLongArray(FRAGMENT_ARG_KEY_SCENARIO_LIST_UPLOAD, it.toLongArray())
                    }
                    exportDumbScenarios?.let {
                        putLongArray(FRAGMENT_ARG_KEY_DUMB_SCENARIO_LIST_UPLOAD, it.toLongArray())
                    }
                }
            }
        }
    }
}