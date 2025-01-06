package com.buzbuz.smartautoclicker.activity.list.upload

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setLabel
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setText
import com.buzbuz.smartautoclicker.databinding.DialogUploadBinding
import com.buzbuz.smartautoclicker.feature.backup.ui.BackupDialogUiState
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getLastUploadUrl
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class UploadDialog: DialogFragment() {
    private lateinit var viewBinding: DialogUploadBinding

    private val viewModel: UploadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding = DialogUploadBinding.inflate(layoutInflater)

        viewBinding.apply {
            fieldUrlName.apply {
                setLabel(R.string.input_field_label_url)
                setText(
                    this@UploadDialog.context?.getEventConfigPreferences()
                        ?.getLastUploadUrl(this@UploadDialog.requireContext())
                )
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title_upload_backup)
            .setView(viewBinding.root)
            .setNegativeButton(R.string.close, null)
            .create()

        return dialog
    }

    companion object {
        fun newInstance(): UploadDialog {
            Log.d("upload", "Show upload dialog")
            return UploadDialog()
        }
    }
}