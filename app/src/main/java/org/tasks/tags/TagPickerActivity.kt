package org.tasks.tags

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.Strings
import org.tasks.billing.Inventory
import org.tasks.compose.components.TasksIcon
import org.tasks.data.entity.TagData
import org.tasks.extensions.addBackPressedCallback
import org.tasks.filters.TagFilter
import org.tasks.filters.getIcon
import org.tasks.injection.ThemedInjectingAppCompatActivity
import org.tasks.themes.ColorProvider
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme
import org.tasks.themes.Theme
import javax.inject.Inject

@AndroidEntryPoint
class TagPickerActivity : ThemedInjectingAppCompatActivity() {
    @Inject lateinit var theme: Theme
    @Inject lateinit var inventory: Inventory
    @Inject lateinit var colorProvider: ColorProvider


    private val viewModel: TagPickerViewModel by viewModels()
    private var taskIds: ArrayList<Long>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val intent = intent
        taskIds = intent.getSerializableExtra(EXTRA_TASKS) as ArrayList<Long>?
        if (savedInstanceState == null) {
            val selected = intent.getParcelableArrayListExtra<TagData>(EXTRA_SELECTED)
            if ( selected != null ) {
                viewModel.setSelected(
                    selected, intent.getParcelableArrayListExtra<TagData>(EXTRA_PARTIALLY_SELECTED)
                )
            }
        }

        addBackPressedCallback { handleBackPressed() }

        viewModel.search("")

        setContent {
            TasksTheme(
                theme = theme.themeBase.index,
                primary = theme.themeColor.primaryColor,
            ) {
                TagPicker(
                    viewModel,
                    onBackClicked = { handleBackPressed() },
                    getTagIcon = { tagData -> getIcon(tagData) },
                    getTagColor = { tagData -> getColor(tagData) }
                )
            } /* setContent */
        }
    } /* onCreate */

    private fun handleBackPressed() {
        if (Strings.isNullOrEmpty(viewModel.searchText.value)) {
            val data = Intent()
            data.putExtra(EXTRA_TASKS, taskIds)
            data.putParcelableArrayListExtra(EXTRA_PARTIALLY_SELECTED, viewModel.getPartiallySelected())
            data.putParcelableArrayListExtra(EXTRA_SELECTED, viewModel.getSelected())
            setResult(Activity.RESULT_OK, data)
            finish()
        } else {
            viewModel.search("")
        }
    } /* handleBackPressed */

    private fun getColor(tagData: TagData): Color {
        if ((tagData.color ?: 0) != 0) {
            val themeColor = colorProvider.getThemeColor(tagData.color ?: 0, true)
            if (inventory.purchasedThemes() || themeColor.isFree) {
                return Color(themeColor.primaryColor)
            }
        }
        return Color(getColor(R.color.icon_tint_with_alpha))
    }

    private fun getIcon(tagData: TagData): String =
        TagFilter(tagData).getIcon(inventory) ?: TasksIcons.LABEL

    /* Copy of the TagPickerActivity's companion object */
    companion object {
        const val EXTRA_SELECTED = "extra_tags"
        const val EXTRA_PARTIALLY_SELECTED = "extra_partial"
        const val EXTRA_TASKS = "extra_tasks"
    }
}

@Composable
internal fun TagPicker(
    viewModel: TagPickerViewModel,
    onBackClicked: () -> Unit,
    getTagIcon: (TagData) -> String,
    getTagColor: (TagData) -> Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column (modifier = Modifier.padding(horizontal = 12.dp)) {
            Box( modifier = Modifier.fillMaxWidth() ) {
                SearchBar(viewModel, onBackClicked)
            }
            Box (
                modifier = Modifier.weight(1f)
            ) {
                PickerBox(
                    viewModel = viewModel,
                    tags = viewModel.tagsList.observeAsState(initial = emptyList()),
                    getTagIcon = getTagIcon,
                    getTagColor = getTagColor
                )
            }
        }
    }
}

@Composable
internal fun SearchBar(
    viewModel: TagPickerViewModel,
    onBack: () -> Unit
) {
    val searchPattern = remember { viewModel.searchText }
    val invitation = LocalContext.current.getString(R.string.enter_tag_name)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            ImageVector.vectorResource(id = R.drawable.ic_outline_arrow_back_24px),
            "Done",
            modifier = Modifier
                .padding(6.dp)
                .clickable { onBack() },
            tint = MaterialTheme.colorScheme.onSurface,
        )

        TextField(
            value = searchPattern.value,
            onValueChange = { viewModel.search(it) },
            placeholder = {
                Text(
                    text = invitation,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = TextStyle(
                textDirection = TextDirection.Content
            ),
            modifier = Modifier.padding(start = 6.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
            )
        )
    }
} /* SearchBar */

@Composable
internal fun PickerBox (
    viewModel: TagPickerViewModel,
    tags: State<List<TagData>>,
    getTagIcon: (TagData) -> String = { TasksIcons.LABEL },
    getTagColor: (TagData) -> Color = { Color.Gray }
) {
    val onClick: (TagData) -> Unit = {
        viewModel.viewModelScope.launch {
            viewModel.toggle(it, viewModel.getState(it) != ToggleableState.On) }
    }

    val newItem: (String) -> Unit = {
        viewModel.viewModelScope.launch { viewModel.createNew(it); viewModel.search("") }
    }

    LazyColumn {
        if (viewModel.tagToCreate.value != "") {
            item(key = -1) {
                val text = LocalContext.current.getString(R.string.new_tag) + " \"${viewModel.tagToCreate.value}\""
                TagRow(
                    icon = TasksIcons.ADD,
                    iconColor = Color(LocalContext.current.getColor(R.color.icon_tint_with_alpha)),
                    text = text,
                    onClick = { newItem(viewModel.searchText.value) }
                )
            }
        }

        items( tags.value, key = { tag -> tag.id!! } )
        {
            val checked = remember { mutableStateOf ( viewModel.getState(it) ) }
            val clickChecked: () -> Unit = { onClick(it); checked.value = viewModel.getState(it) }
            TagRow(
                icon = getTagIcon(it),
                iconColor = getTagColor(it),
                text = it.name!!,
                onClick = clickChecked
            ) {
                TriStateCheckbox(
                    modifier = Modifier.padding(6.dp),
                    state = checked.value,
                    onClick = clickChecked
                )
            }
        }
    }
} /* PickerBox */

@Composable
internal fun TagRow (
    icon: String,
    iconColor: Color,
    text: String,
    onClick: () -> Unit,
    checkBox: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TasksIcon(
            modifier = Modifier.padding(6.dp),
            label = icon,
            tint = iconColor
        )
        Text(
            text,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        checkBox()
    }
} /* TagRow */

/*
internal fun genTestTags(): List<TagData>
{
    var idcc: Long = 1
    return "alpha beta gamma delta kappa theta alfa1 beta1 gamma1 delta1 kappa1 theta1"
        .split(" ")
        .map { name -> TagData(name).also{ it.id = idcc++ } }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xffffff)
internal fun PickerBoxPreview() {
    val list = remember { mutableStateOf( genTestTags() ) }
    PickerBox(list, getTagColor = { Color.Green })
}
*/
