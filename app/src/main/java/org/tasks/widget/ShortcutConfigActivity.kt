package org.tasks.widget

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.compose.FilterSelectionActivity.Companion.launch
import org.tasks.compose.FilterSelectionActivity.Companion.registerForFilterPickerResult
import org.tasks.data.UUIDHelper
import org.tasks.databinding.ActivityWidgetShortcutLayoutBinding
import org.tasks.dialogs.ColorPalettePicker
import org.tasks.dialogs.ColorPalettePicker.Companion.newColorPalette
import org.tasks.dialogs.ColorPickerAdapter.Palette
import org.tasks.filters.Filter
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.intents.TaskIntents
import org.tasks.preferences.DefaultFilterProvider
import org.tasks.themes.DrawableUtil
import org.tasks.themes.ThemeColor
import javax.inject.Inject

@AndroidEntryPoint
class ShortcutConfigActivity : ThemedInjectingAppCompatActivity(), ColorPalettePicker.ColorPickedCallback {
    @Inject lateinit var defaultFilterProvider: DefaultFilterProvider

    private lateinit var toolbar: Toolbar
    private lateinit var shortcutList: TextInputEditText
    private lateinit var shortcutName: TextInputEditText
    private lateinit var colorIcon: TextView
    private lateinit var clear: View

    private var selectedFilter: Filter? = null
    private var selectedTheme = 0
    private val listPickerResult = registerForFilterPickerResult {
        if (selectedFilter != null && selectedFilter!!.title == getShortcutName()) {
            shortcutName.text = null
        }
        selectedFilter = it
        updateFilterAndTheme()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        val binding = ActivityWidgetShortcutLayoutBinding.inflate(layoutInflater)
        binding.let {
            toolbar = it.toolbar.toolbar
            shortcutList = it.body.shortcutList.apply {
                setOnClickListener { showListPicker() }
                setOnFocusChangeListener { _, hasFocus -> onListFocusChange(hasFocus) }
            }
            shortcutName = it.body.shortcutName
            colorIcon = it.body.color.color
            clear = it.body.color.clear.clear
            it.body.color.colorRow.setOnClickListener { showThemePicker() }
        }
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            binding.body.root.updatePadding(bottom = systemBars.bottom)
            insets
        }

        toolbar.setTitle(R.string.FSA_label)
        toolbar.navigationIcon = getDrawable(R.drawable.ic_outline_save_24px)
        toolbar.setNavigationOnClickListener { save() }
        if (savedInstanceState == null) {
            selectedFilter = defaultFilterProvider.startupFilter
            selectedTheme = 7
        } else {
            selectedFilter = savedInstanceState.getParcelable(EXTRA_FILTER)
            selectedTheme = savedInstanceState.getInt(EXTRA_THEME)
        }
        updateFilterAndTheme()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_FILTER, selectedFilter)
        outState.putInt(EXTRA_THEME, selectedTheme)
    }

    private fun onListFocusChange(focused: Boolean) {
        if (focused) {
            shortcutList.clearFocus()
            showListPicker()
        }
    }

    private fun showListPicker() {
        listPickerResult.launch(context = this, selectedFilter = selectedFilter)
    }

    private fun showThemePicker() {
        newColorPalette(null, 0, Palette.LAUNCHERS)
                .show(supportFragmentManager, FRAG_TAG_COLOR_PICKER)
    }

    private fun updateFilterAndTheme() {
        if (isNullOrEmpty(getShortcutName()) && selectedFilter != null) {
            shortcutName.setText(selectedFilter!!.title)
        }
        if (selectedFilter != null) {
            shortcutList.setText(selectedFilter!!.title)
        }
        updateTheme()
    }

    private fun updateTheme() {
        clear.visibility = View.GONE
        val color = ThemeColor.getLauncherColor(this, themeIndex)
        DrawableUtil.setLeftDrawable(this, colorIcon, R.drawable.color_picker)
        DrawableUtil.setTint(DrawableUtil.getLeftDrawable(colorIcon), color.primaryColor)
        color.applyToNavigationBar(this)
    }

    private val themeIndex: Int
        get() = if (selectedTheme >= 0 && selectedTheme < ThemeColor.LAUNCHER_COLORS.size) selectedTheme else 7

    private fun getShortcutName(): String = shortcutName.text.toString().trim { it <= ' ' }

    private fun save() {
        val filterId = defaultFilterProvider.getFilterPreferenceValue(selectedFilter!!)
        ShortcutManagerCompat.requestPinShortcut(
            this,
            ShortcutInfoCompat.Builder(this, UUIDHelper.newUUID())
                .setShortLabel(
                    getShortcutName().takeIf { it.isNotBlank() } ?: getString(R.string.app_name)
                )
                .setIntent(TaskIntents.getTaskListByIdIntent(this, filterId))
                .setIcon(IconCompat.createWithResource(this, ThemeColor.ICONS[themeIndex]))
                .build(),
            null,
        )
        finish()
    }

    override fun onColorPicked(index: Int) {
        selectedTheme = index
        updateTheme()
    }

    companion object {
        private const val EXTRA_FILTER = "extra_filter"
        private const val EXTRA_THEME = "extra_theme"
        private const val FRAG_TAG_COLOR_PICKER = "frag_tag_color_picker"
    }
}