package com.buzbuz.smartautoclicker.activity.list.upload

import android.app.Dialog
import android.os.Bundle
import android.util.Log
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
            textFileSelection.setOnClickListener {
                Log.d("upload", "Dumb scenario is : $exportDumbScenarios")
                viewModel.createScenarioUpload(exportSmartScenarios, exportDumbScenarios)
                viewModel.saveLastUrl(true)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_upload_backup)
            .setView(viewBinding.root)
            .setNegativeButton(R.string.close) { dialogFragment, _ ->
                viewModel.saveLastUrl(false)
                dialogFragment.dismiss()
            }
            .create()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.getUrl.collect { url ->
//                    viewBinding.fieldUrlName.setText(url)
//                }
                viewModel.getLoading.collect { isLoading ->
                    viewBinding.fieldUrlName.textField.isEnabled = !isLoading
                    viewBinding.textFileSelection.isEnabled = !isLoading
                    viewBinding.textFileSelection.text = getString(
                        if(isLoading)
                            R.string.loading
                        else
                            R.string.item_title_upload_scenario
                    )
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = !isLoading
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