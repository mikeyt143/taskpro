package org.tasks.compose.drawer

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.PermIdentity
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import org.tasks.compose.components.Chevron
import org.tasks.compose.components.SearchBar
import org.tasks.compose.components.TasksIcon
import org.tasks.kmp.formatNumber
import org.tasks.kmp.org.tasks.compose.rememberImeState
import tasks.kmp.generated.resources.Res
import tasks.kmp.generated.resources.help_and_feedback
import tasks.kmp.generated.resources.search
import tasks.kmp.generated.resources.subscribe
import java.util.Locale
import kotlin.math.roundToInt

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private val SEARCH_BAR_BOTTOM_PADDING = androidx.compose.material3.OutlinedTextFieldTopPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListDrawer(
    arrangement: Arrangement.Vertical,
    filters: ImmutableList<DrawerItem>,
    onClick: (DrawerItem) -> Unit,
    onAddClick: (DrawerItem.Header) -> Unit,
    onErrorClick: () -> Unit,
    searchBar: @Composable RowScope.() -> Unit,
) {
    val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()
    var bottomBarHeight by remember { mutableStateOf(0.dp) }
    Scaffold(
        modifier = Modifier
            .imePadding()
            .nestedScroll(bottomAppBarScrollBehavior.nestedScrollConnection),
        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val safeConstraints = constraints.copy(
                            minHeight = constraints.minHeight.coerceAtLeast(0),
                            maxHeight = constraints.maxHeight.coerceAtLeast(0)
                        )
                        val placeable = measurable.measure(safeConstraints)
                        bottomAppBarScrollBehavior.state.heightOffsetLimit =
                            -placeable.height.toFloat()
                        val height =
                            placeable.height + bottomAppBarScrollBehavior.state.heightOffset
                        bottomBarHeight = height.toDp()
                        layout(placeable.width, height.roundToInt().coerceAtLeast(0)) {
                            placeable.place(0, 0)
                        }
                    },
                containerColor = MaterialTheme.colorScheme.surface,
                scrollBehavior = bottomAppBarScrollBehavior
            ) {
                searchBar()
            }
        },
    ) { contentPadding ->
        val keyboardOpen = rememberImeState().value
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = if (keyboardOpen) {
                contentPadding
            } else {
                val systemBarPadding = WindowInsets.systemBars.asPaddingValues()
                val direction = LocalLayoutDirection.current
                remember(contentPadding, systemBarPadding, bottomBarHeight) {
                    PaddingValues(
                        start = contentPadding.calculateStartPadding(direction),
                        top = contentPadding.calculateTopPadding(),
                        end = contentPadding.calculateEndPadding(direction),
                        bottom = maxOf(
                            systemBarPadding.calculateBottomPadding(),
                            contentPadding.calculateBottomPadding() + bottomBarHeight
                        )
                    )
                }
            },
            verticalArrangement = arrangement,
        ) {
            items(items = filters, key = { it.key() }) {
                when (it) {
                    is DrawerItem.Filter -> FilterItem(
                        item = it,
                        onClick = { onClick(it) }
                    )

                    is DrawerItem.Header -> HeaderItem(
                        item = it,
                        canAdd = it.canAdd,
                        toggleCollapsed = { onClick(it) },
                        onAddClick = { onAddClick(it) },
                        onErrorClick = onErrorClick,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FilterItem(
    modifier: Modifier = Modifier,
    item: DrawerItem.Filter,
    onClick: () -> Unit,
) {
    MenuRow(
        modifier = modifier
            .background(
                if (item.selected)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)
                else
                    Color.Transparent
            )
            .clickable(onClick = onClick),
        onClick = onClick,
    ) {
        TasksIcon(
            label = item.icon,
            tint = when (item.color) {
                0 -> MaterialTheme.colorScheme.onSurface
                else -> Color(color = item.color)
            }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        if (item.shareCount > 0) {
            Icon(
                imageVector = when (item.shareCount) {
                    1 -> Icons.Outlined.PermIdentity
                    else -> Icons.Outlined.PeopleOutline
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (item.count > 0) {
                Text(
                    text = formatNumber(item.count),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
internal fun HeaderItem(
    modifier: Modifier = Modifier,
    item: DrawerItem.Header,
    canAdd: Boolean,
    toggleCollapsed: () -> Unit,
    onAddClick: () -> Unit,
    onErrorClick: () -> Unit,
) {
    Column(
        modifier = modifier,
    ) {
        Divider(modifier = Modifier.fillMaxWidth())
        MenuRow(
            padding = PaddingValues(start = 16.dp),
            onClick = toggleCollapsed,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = item.title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = toggleCollapsed) {
                Chevron(item.collapsed)
            }
            if (canAdd) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (item.hasError) {
                IconButton(onClick = onErrorClick) {
                    Icon(
                        imageVector = Icons.Outlined.SyncProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuRow(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(48.dp)
            .padding(padding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun RowScope.MenuSearchBar(
    begForMoney: Boolean,
    onDrawerAction: (DrawerAction) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val density = LocalDensity.current
    var hasFocus by remember { mutableStateOf(false) }
    SearchBar(
        modifier = Modifier
            .onFocusChanged { hasFocus = it.hasFocus }
            .padding(
                start = 8.dp,
                end = if (hasFocus) 8.dp else 0.dp,
                bottom = with(density) { SEARCH_BAR_BOTTOM_PADDING.toDp() }
            )
            .weight(1f)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        text = query,
        onTextChange = { onQueryChange(it) },
        placeHolder = stringResource(Res.string.search),
        onCloseClicked = { onQueryChange("") },
        onSearchClicked = {
            // TODO: close keyboard
        },
    )
    if (!hasFocus) {
        if (begForMoney) {
            IconButton(onClick = { onDrawerAction(DrawerAction.PURCHASE) }) {
                Icon(
                    imageVector = Icons.Outlined.AttachMoney,
                    contentDescription = stringResource(Res.string.subscribe),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        IconButton(onClick = { onDrawerAction(DrawerAction.HELP_AND_FEEDBACK) }) {
            // Cancel the mirroring of the help icon when the locale is Hebrew.
            val modifier =
                if (Locale.getDefault().language == Locale.forLanguageTag("he").language) {
                    Modifier.scale(scaleX = -1f, scaleY = 1f)
                } else {
                    Modifier
                }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = stringResource(Res.string.help_and_feedback),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = modifier,
            )
        }
    }
}
