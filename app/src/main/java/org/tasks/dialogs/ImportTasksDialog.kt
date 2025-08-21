package org.tasks.dialogs

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.backup.TasksJsonImporter
import org.tasks.backup.TasksJsonImporter.ImportResult
import javax.inject.Inject

@AndroidEntryPoint
class ImportTasksDialog : DialogFragment() {
    @Inject lateinit var jsonImporter: TasksJsonImporter
    @Inject lateinit var dialogBuilder: DialogBuilder
    @Inject lateinit var context: Activity

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val arguments = requireArguments()
        val data = arguments.getParcelable<Uri>(EXTRA_URI)
        val progressDialog = dialogBuilder.newProgressDialog().apply {
            setProgressStyle(ProgressDialog.STYLE_SPINNER)
            setCancelable(false)
            isIndeterminate = true
        }

        progressDialog.show()
        isCancelable = false
        lifecycleScope.launch {
            val result = withContext(NonCancellable) {
                jsonImporter.importTasks(requireActivity(), data, progressDialog)
            }
            if (progressDialog.isShowing) {
                progressDialog.dismiss()
            }
            showSummary(result)
        }
        return progressDialog
    }

    private fun showSummary(result: ImportResult) {
        val r = requireContext().resources
        dialogBuilder
                .newDialog(R.string.import_summary_title)
                .setMessage(
                        r.getString(
                                R.string.import_summary_message,
                                "",
                                r.getQuantityString(R.plurals.Ntasks, result.taskCount, result.taskCount),
                                r.getQuantityString(
                                        R.plurals.Ntasks,
                                        result.importCount,
                                        result.importCount),
                                r.getQuantityString(R.plurals.Ntasks, result.skipCount, result.skipCount),
                                r.getQuantityString(R.plurals.Ntasks, 0, 0)))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    companion object {
        private const val EXTRA_URI = "extra_uri"

        fun newImportTasksDialog(data: Uri): ImportTasksDialog {
            val importTasksDialog = ImportTasksDialog()
            val args = Bundle()
            args.putParcelable(EXTRA_URI, data)
            importTasksDialog.arguments = args
            return importTasksDialog
        }
    }
}