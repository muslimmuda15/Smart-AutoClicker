package com.buzbuz.smartautoclicker.activity.list.sync

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
import com.buzbuz.smartautoclicker.databinding.DialogSyncBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SyncDialog: DialogFragment() {
    private lateinit var viewBinding: DialogSyncBinding
    private val viewModel: SyncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogSyncBinding.inflate(layoutInflater)

        viewBinding.apply {
            fieldUrlName.apply {
                setLabel(R.string.input_field_label_url)
                setText(viewModel.getLastUrl)
                setOnTextChangedListener { viewModel.setUrl(it.toString()) }
            }
            buttonSync.setOnClickListener {
                Log.d("sync", "Dumb scenario")
                viewModel.createScenarioSync()
                viewModel.saveLastUrl(true)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_sync_backup)
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

                    when(state.status) {
                        StatusSyncStateUI.READY -> {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                        }
                        StatusSyncStateUI.UPLOADING -> {
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

                            viewBinding.fieldUrlName.textField.isEnabled = false
                            viewBinding.buttonSync.isEnabled = false
                        }
                        StatusSyncStateUI.COMPLETE -> {
                            viewBinding.inputLayout.visibility = View.GONE
                            viewBinding.buttonSync.visibility = View.GONE
                            viewBinding.layoutCompatWarning.visibility = View.GONE
                            viewBinding.iconStatus.apply {
                                setImageResource(R.drawable.img_success)
                                drawable.setTint(Color.GREEN)
                            }
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        }
                        StatusSyncStateUI.FAILED -> {
                            viewBinding.inputLayout.visibility = View.GONE
                            viewBinding.buttonSync.visibility = View.GONE
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
        private const val FRAGMENT_ARG_KEY_SCENARIO_LIST_SYNC = ":backup:fragment_args_key_scenario_list_sync"
        private const val FRAGMENT_ARG_KEY_DUMB_SCENARIO_LIST_SYNC = ":backup:fragment_args_key_dumb_scenario_list_sync"

        fun newInstance(): SyncDialog {
            Log.d("sync", "Show sync dialog")
            return SyncDialog()
        }
    }
}