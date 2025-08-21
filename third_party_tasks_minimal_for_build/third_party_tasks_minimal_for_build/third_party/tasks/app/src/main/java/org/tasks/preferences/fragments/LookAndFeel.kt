package org.tasks.preferences.fragments

import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.color.DynamicColors
import com.todoroo.andlib.utility.AndroidUtilities.atLeastTiramisu
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.billing.Inventory
import org.tasks.billing.PurchaseActivity
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForFilterPickerResult
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter
import org.tasks.dialogs.ColorWheelPicker
import org.tasks.dialogs.ThemePickerDialog
import org.tasks.dialogs.ThemePickerDialog.Companion.newThemePickerDialog
import org.tasks.injection.InjectingPreferenceFragment
import org.tasks.locale.LocalePickerDialog
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.preferences.Preferences
import org.tasks.themes.ThemeBase
import org.tasks.themes.ThemeBase.DEFAULT_BASE_THEME
import org.tasks.themes.ThemeBase.EXTRA_THEME_OVERRIDE
import org.tasks.themes.ThemeColor
import org.tasks.themes.ThemeColor.getLauncherColor
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LookAndFeel : InjectingPreferenceFragment() {

    @Inject lateinit var themeBase: ThemeBase
    @Inject lateinit var themeColor: ThemeColor
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var locale: Locale

    private val listPickerLauncher = registerForFilterPickerResult {
        defaultFilterProvider.setDefaultOpenFilter(it)
        findPreference(R.string.p_default_open_filter).summary = it.title
        localBroadcastManager.broadcastRefresh()
    }

    override fun getPreferenceXml() = R.xml.preferences_look_and_feel

    override suspend fun setupPreferences(savedInstanceState: Bundle?) {
        val themePref = findPreference(R.string.p_theme)
        val themeNames = resources.getStringArray(R.array.base_theme_names)
        themePref.summary = themeNames[themeBase.index]
        themePref.setOnPreferenceClickListener {
            newThemePickerDialog(this, REQUEST_THEME_PICKER, themeBase.index)
                .show(parentFragmentManager, FRAG_TAG_THEME_PICKER)
            false
        }

        val defaultList = findPreference(R.string.p_default_open_filter)
        val filter = defaultFilterProvider.getDefaultOpenFilter()
        defaultList.summary = filter.title
        defaultList.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            lifecycleScope.launch {
                listPickerLauncher.launch(
                    context = requireContext(),
                    selectedFilter = defaultFilterProvider.getDefaultOpenFilter(),
                )
            }
            true
        }

        val languagePreference = findPreference(R.string.p_language)
        languagePreference.summary = locale.getDisplayName(locale)
        languagePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (atLeastTiramisu()) {
                startActivity(
                    Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                        .setData(Uri.fromParts("package", requireContext().packageName, null))
                )
            } else {
                val dialog = LocalePickerDialog.newLocalePickerDialog()
                dialog.setTargetFragment(this, REQUEST_LOCALE)
                dialog.show(parentFragmentManager, FRAG_TAG_LOCALE_PICKER)
            }
            false
        }

        openUrl(R.string.translations, R.string.url_translations)
        val themeColor = findPreference(R.string.p_theme_color)
        val dynamicColor = findPreference(R.string.p_dynamic_color) as SwitchPreferenceCompat
        themeColor.isVisible = !dynamicColor.isChecked
        dynamicColor.setOnPreferenceChangeListener { _, newValue ->
            themeColor.isVisible = !(newValue as Boolean)
            true
        }
        requires(DynamicColors.isDynamicColorAvailable(), R.string.p_dynamic_color)
    }

    override fun onResume() {
        super.onResume()

        setupColorPreference(
            R.string.p_theme_color,
            themeColor.pickerColor,
            ColorPickerAdapter.Palette.COLORS,
            REQUEST_COLOR_PICKER
        )
        updateLauncherPreference()

        if (DynamicColors.isDynamicColorAvailable()) {
            (findPreference(R.string.p_dynamic_color) as SwitchPreferenceCompat).apply {
                if (inventory.hasPro) {
                    summary = null
                    isEnabled = true
                } else {
                    summary = getString(R.string.requires_pro_subscription)
                    isEnabled = false
                    isChecked = false
                }
            }
        }
    }

    private fun updateLauncherPreference() {
        val launcher = getLauncherColor(context, preferences.getInt(R.string.p_theme_launcher, 7))
        setupColorPreference(
            R.string.p_theme_launcher,
            launcher.pickerColor,
            ColorPickerAdapter.Palette.LAUNCHERS,
            REQUEST_LAUNCHER_PICKER
        )
    }

    private fun setBaseTheme(index: Int) {
        activity?.intent?.removeExtra(EXTRA_THEME_OVERRIDE)
        preferences.setInt(R.string.p_theme, index)
        if (themeBase.index != index) {
            Handler().post {
                ThemeBase(index).setDefaultNightMode()
                recreate()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_PURCHASE -> {
                val index = if (inventory.hasPro) {
                    data?.getIntExtra(ThemePickerDialog.EXTRA_SELECTED, DEFAULT_BASE_THEME)
                            ?: themeBase.index
                } else preferences.themeBase
                setBaseTheme(index)
            }
            REQUEST_THEME_PICKER -> {
                val index = data?.getIntExtra(ThemePickerDialog.EXTRA_SELECTED, DEFAULT_BASE_THEME)
                        ?: preferences.themeBase
                if (resultCode == RESULT_OK) {
                    if (inventory.purchasedThemes() || ThemeBase(index).isFree) {
                        setBaseTheme(index)
                    } else {
                        startActivityForResult(
                            Intent(context, PurchaseActivity::class.java),
                            REQUEST_PURCHASE
                        )
                    }
                } else {
                    setBaseTheme(index)
                }
            }
            REQUEST_COLOR_PICKER -> {
                if (resultCode == RESULT_OK) {
                    val color = data?.getIntExtra(
                            ColorWheelPicker.EXTRA_SELECTED,
                            themeColor.primaryColor
                    ) ?: themeColor.primaryColor

                    if (preferences.defaultThemeColor != color) {
                        preferences.setInt(R.string.p_theme_color, color)
                        recreate()
                    }
                }
            }
            REQUEST_LAUNCHER_PICKER -> {
                if (resultCode == RESULT_OK) {
                    val index = data!!.getIntExtra(ColorPalettePicker.EXTRA_SELECTED, 0)
                    setLauncherIcon(index)
                    preferences.setInt(R.string.p_theme_launcher, index)
                    updateLauncherPreference()
                }
            }
            REQUEST_LOCALE -> {
                if (resultCode == RESULT_OK) {
                    val languageTag = data!!.getStringExtra(LocalePickerDialog.EXTRA_LOCALE)
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(languageTag)
                    )
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun setLauncherIcon(index: Int) {
        val packageManager: PackageManager? = context?.packageManager
        for (i in ThemeColor.LAUNCHERS.indices) {
            val componentName = ComponentName(
                requireContext(),
                "com.todoroo.astrid.activity.TaskListActivity" + ThemeColor.LAUNCHERS[i]
            )
            packageManager?.setComponentEnabledSetting(
                componentName,
                if (index == i) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun setupColorPreference(
        @StringRes prefId: Int,
        color: Int,
        palette: ColorPickerAdapter.Palette,
        requestCode: Int
    ) {
        tintColorPreference(prefId, color)
        findPreference(prefId).setOnPreferenceClickListener {
            newColorPalette(this, requestCode, color, palette)
                .show(parentFragmentManager, FRAG_TAG_COLOR_PICKER)
            false
        }
    }

    companion object {
        private const val REQUEST_THEME_PICKER = 10001
        private const val REQUEST_COLOR_PICKER = 10002
        private const val REQUEST_LAUNCHER_PICKER = 10004
        private const val REQUEST_LOCALE = 10006
        private const val REQUEST_PURCHASE = 10007
        private const val FRAG_TAG_LOCALE_PICKER = "frag_tag_locale_picker"
        private const val FRAG_TAG_THEME_PICKER = "frag_tag_theme_picker"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
    }
}