/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.files

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tasks.R
import org.tasks.Strings
import org.tasks.compose.edit.AttachmentRow
import org.tasks.data.dao.TaskAttachmentDao
import org.tasks.data.entity.TaskAttachment
import org.tasks.dialogs.AddAttachmentDialog
import org.tasks.extensions.Context.takePersistableUriPermission
import org.tasks.files.FileHelper
import org.tasks.preferences.Preferences
import org.tasks.ui.TaskEditControlFragment
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FilesControlSet : TaskEditControlFragment() {
    @Inject lateinit var taskAttachmentDao: TaskAttachmentDao
    @Inject lateinit var preferences: Preferences

    @Composable
    override fun Content() {
        val viewState = viewModel.viewState.collectAsStateWithLifecycle().value
        LaunchedEffect(Unit) {
            if (viewState.task.hasTransitory(TaskAttachment.KEY)) {
                for (uri in (viewState.task.getTransitory<ArrayList<Uri>>(TaskAttachment.KEY))!!) {
                    newAttachment(uri)
                }
            }
        }
        val context = LocalContext.current
        AttachmentRow(
            attachments = viewState.attachments,
            openAttachment = {
                Timber.d("Clicked open $it")
                FileHelper.startActionView(
                    context,
                    if (Strings.isNullOrEmpty(it.uri)) null else Uri.parse(it.uri)
                )
            },
            deleteAttachment = {
                Timber.d("Clicked delete $it")
                viewModel.setAttachments(viewState.attachments - it)
            },
            addAttachment = {
                Timber.d("Add attachment clicked")
                AddAttachmentDialog.newAddAttachmentDialog(this@FilesControlSet)
                    .show(parentFragmentManager, FRAG_TAG_ADD_ATTACHMENT_DIALOG)
            },
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AddAttachmentDialog.REQUEST_CAMERA || requestCode == AddAttachmentDialog.REQUEST_AUDIO) {
            if (resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    val uri = data!!.data
                    copyToAttachmentDirectory(uri)
                    FileHelper.delete(requireContext(), uri)
                }
            }
        } else if (requestCode == AddAttachmentDialog.REQUEST_STORAGE || requestCode == AddAttachmentDialog.REQUEST_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    val clip = data!!.clipData
                    if (clip != null) {
                        for (i in 0 until clip.itemCount) {
                            val item = clip.getItemAt(i)
                            requireContext().takePersistableUriPermission(item.uri)
                            copyToAttachmentDirectory(item.uri)
                        }
                    } else {
                        requireContext().takePersistableUriPermission(data.data!!)
                        copyToAttachmentDirectory(data.data)
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private suspend fun copyToAttachmentDirectory(input: Uri?) {
        val destination = preferences.attachmentsDirectory ?: return
        newAttachment(
            if (FileHelper.isInTree(requireContext(), destination, input!!)) {
                Timber.d("$input already exists in $destination")
                input
            } else {
                Timber.d("Copying $input to $destination")
                FileHelper.copyToUri(requireContext(), destination, input)
            }
        )
    }

    private suspend fun newAttachment(output: Uri) {
        val attachment = TaskAttachment(
            uri = output.toString(),
            name = FileHelper.getFilename(requireContext(), output)!!,
        )
        withContext(Dispatchers.IO) {
            taskAttachmentDao.insert(attachment)
            viewModel.setAttachments(
                viewModel.viewState.value.attachments +
                        (taskAttachmentDao.getAttachment(attachment.remoteId) ?: return@withContext))
        }
    }

    companion object {
        val TAG = R.string.TEA_ctrl_files_pref
        private const val FRAG_TAG_ADD_ATTACHMENT_DIALOG = "frag_tag_add_attachment_dialog"
    }
}
