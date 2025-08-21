package org.tasks.preferences.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceCategory
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tasks.BuildConfig
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.auth.SignInActivity
import org.tasks.auth.SignInActivity.Platform
import org.tasks.billing.Inventory
import org.tasks.billing.Purchase
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.extensions.Context.openUri
import org.tasks.extensions.Context.toast
import org.tasks.jobs.WorkManager
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDay
import org.tasks.preferences.IconPreference
import org.tasks.preferences.fragments.MainSettingsFragment.Companion.REQUEST_TASKS_ORG
import org.tasks.utility.copyToClipboard
import javax.inject.Inject

@AndroidEntryPoint
class TasksAccount : BaseAccountPreference() {

    @Inject lateinit var inventory: Inventory
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var workManager: WorkManager

    private val viewModel: TasksAccountViewModel by viewModels()

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch {
                refreshUi(account)
            }
        }
    }

    override fun getPreferenceXml() = R.xml.preferences_tasks

    private fun clearPurchaseError(purchase: Purchase?) {
        if (purchase?.isTasksSubscription == true && account.error.isPaymentRequired()) {
            lifecycleScope.launch {
                caldavDao.update(account.copy(error = null))
            }
        }
    }

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        super.setupPreferences(savedInstanceState)

        inventory.subscription.observe(this) { clearPurchaseError(it) }

        if (savedInstanceState == null) {
            viewModel.refreshPasswords(account)
        }

        findPreference(R.string.local_lists).setOnPreferenceClickListener {
            workManager.migrateLocalTasks(account)
            context?.toast(R.string.migrating_tasks)
            false
        }

        findPreference(R.string.generate_new_password).setOnPreferenceChangeListener { _, description ->
            viewModel.requestNewPassword(account, description as String)
            false
        }

        openUrl(R.string.app_passwords_more_info, R.string.url_app_passwords)
    }

    override suspend fun removeAccount() {
        super.removeAccount()
        // TODO: try to delete session from caldav.tasks.org
        inventory.updateTasksAccount()
    }

    override fun onResume() {
        super.onResume()
        viewModel.appPasswords.observe(this) { passwords ->
            passwords?.let {
                runBlocking {
                    refreshPasswords(passwords)
                }
            }
        }
        viewModel.newPassword.observe(this) {
            it?.let {
                val view = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_app_password, null)
                setupTextField(view, R.id.url_layout, R.string.url, getString(R.string.tasks_caldav_url))
                setupTextField(view, R.id.user_layout, R.string.user, it.username)
                setupTextField(view, R.id.password_layout, R.string.password, it.password)
                dialogBuilder.newDialog()
                        .setView(view)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            viewModel.clearNewPassword()
                            viewModel.refreshPasswords(account)
                        }
                        .setCancelable(false)
                        .setNeutralButton(R.string.help) { _, _ ->
                            context?.openUri(R.string.url_app_passwords)
                        }
                        .show()
            }
        }
        localBroadcastManager.registerRefreshListReceiver(refreshReceiver)
    }

    private fun setupTextField(v: View, layout: Int, labelRes: Int, value: String?) {
        with(v.findViewById<TextInputLayout>(layout)) {
            editText?.setText(value)
            if (value != null) {
                setEndIconOnClickListener {
                    copyToClipboard(requireContext(), labelRes, value)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        localBroadcastManager.unregisterReceiver(refreshReceiver)
    }

    private val isGithub: Boolean
        get() = account.username?.startsWith("github") == true

    override suspend fun refreshUi(account: CaldavAccount) {
        (findPreference(R.string.sign_in_with_google) as IconPreference).apply {
            if (account.error.isNullOrBlank()) {
                isVisible = false
                return@apply
            }
            isVisible = true
            when {
                account.isPaymentRequired() -> {
                    val subscription = inventory.subscription.value
                    if (isGithub) {
                        title = null
                        setSummary(R.string.insufficient_sponsorship)
                        @Suppress("KotlinConstantConditions")
                        if (BuildConfig.FLAVOR == "googleplay") {
                            onPreferenceClickListener = null
                        } else {
                            setOnPreferenceClickListener {
                                context.openUri(R.string.url_sponsor)
                                false
                            }
                        }
                    } else {
                        setOnPreferenceClickListener { showPurchaseDialog() }
                        if (subscription == null || subscription.isTasksSubscription) {
                            setTitle(R.string.button_subscribe)
                            setSummary(R.string.your_subscription_expired)
                        } else {
                            setTitle(R.string.manage_subscription)
                            setSummary(R.string.insufficient_subscription)
                        }
                    }
                }
                account.isLoggedOut() -> {
                    setTitle(if (isGithub) {
                        R.string.sign_in_with_github
                    } else {
                        R.string.sign_in_with_google
                    })
                    setSummary(R.string.authentication_required)
                    setOnPreferenceClickListener {
                        activity?.startActivityForResult(
                                Intent(activity, SignInActivity::class.java)
                                        .putExtra(
                                            SignInActivity.EXTRA_SELECT_SERVICE,
                                            if (isGithub) Platform.GITHUB else Platform.GOOGLE
                                        ),
                                REQUEST_TASKS_ORG)
                        false
                    }
                }
                else -> {
                    this.title = null
                    this.summary = account.error
                    this.onPreferenceClickListener = null
                }
            }
            iconVisible = true
        }

        lifecycleScope.launch {
            val localAccount = caldavDao.getAccounts(CaldavAccount.TYPE_LOCAL).firstOrNull()
            val listCount = localAccount?.uuid?.let { caldavDao.listCount(it) } ?: 0
            val quantityString = resources.getQuantityString(R.plurals.list_count, listCount, listCount)
            findPreference(R.string.migrate).isVisible = listCount > 0
            findPreference(R.string.local_lists).summary =
                    getString(R.string.migrate_count, quantityString)
        }
    }

    private suspend fun refreshPasswords(passwords: List<TasksAccountViewModel.AppPassword>) {
        findPreference(R.string.app_passwords_more_info).isVisible = passwords.isEmpty()
        val category = findPreference(R.string.app_passwords) as PreferenceCategory
        category.removeAll()
        passwords.forEach {
            val description = it.description ?: getString(R.string.app_password)
            category.addPreference(IconPreference(requireContext()).apply {
                layoutResource = R.layout.preference_icon
                iconVisible = true
                drawable = context.getDrawable(R.drawable.ic_outline_delete_24px)
                tint = ContextCompat.getColor(requireContext(), R.color.icon_tint_with_alpha)
                title = description
                iconClickListener = View.OnClickListener { _ ->
                    dialogBuilder.newDialog()
                            .setTitle(R.string.delete_tag_confirmation, description)
                            .setMessage(R.string.app_password_delete_confirmation)
                            .setPositiveButton(R.string.ok) { _, _ ->
                                viewModel.deletePassword(account, it.id)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()

                }
                summary = """
                ${getString(R.string.app_password_created_at, formatString(it.createdAt))}
                ${getString(R.string.app_password_last_access, formatString(it.lastAccess) ?: getString(R.string.last_backup_never))}
            """.trimIndent()
            })
        }
    }

    private suspend fun formatString(date: Long?): String? = date?.let {
        getRelativeDay(
            date,
            DateStyle.FULL,
            lowercase = true
        )
    }

    companion object {
        fun newTasksAccountPreference(account: CaldavAccount): Fragment {
            val fragment = TasksAccount()
            fragment.arguments = Bundle().apply {
                putParcelable(EXTRA_ACCOUNT, account)
            }
            return fragment
        }
    }
}