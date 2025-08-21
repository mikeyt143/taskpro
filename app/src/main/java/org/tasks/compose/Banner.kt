package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.BuildConfig
import org.tasks.R
import org.tasks.TasksApplication
import org.tasks.compose.components.AnimatedBanner
import org.tasks.compose.components.Banner
import org.tasks.themes.TasksTheme

@Composable
fun NotificationsDisabledBanner(
    settings: () -> Unit,
    dismiss: () -> Unit,
) {
    Banner(
        title = stringResource(id = R.string.enable_reminders),
        body = stringResource(id = R.string.enable_reminders_description),
        dismissText = stringResource(id = R.string.dismiss),
        onDismiss = dismiss,
        action = stringResource(id = R.string.TLA_menu_settings),
        onAction = settings,
    )
}

@Composable
fun AlarmsDisabledBanner(
    settings: () -> Unit,
    dismiss: () -> Unit,
) {
    Banner(
        title = stringResource(id = R.string.enable_alarms),
        body = stringResource(id = R.string.enable_alarms_description),
        dismissText = stringResource(id = R.string.dismiss),
        onDismiss = dismiss,
        action = stringResource(id = R.string.TLA_menu_settings),
        onAction = settings,
    )

}

@Composable
fun SubscriptionNagBanner(
    subscribe: () -> Unit,
    dismiss: () -> Unit,
) {
    Banner(
        title = stringResource(id = R.string.enjoying_tasks),
        body = stringResource(id = if (TasksApplication.IS_GENERIC) {
            R.string.donate_nag
        } else {
            R.string.support_development_subscribe
        }),
        dismissText = stringResource(id = R.string.donate_maybe_later),
        onDismiss = dismiss,
        action = stringResource(id = if (TasksApplication.IS_GENERIC) {
            R.string.donate_today
        } else {
            R.string.button_subscribe
        }),
        onAction = subscribe
    )
}

@Composable
fun QuietHoursBanner(
    showSettings: () -> Unit,
    dismiss: () -> Unit,
) {
    Banner(
        title = stringResource(R.string.quiet_hours_in_effect),
        body = stringResource(R.string.quiet_hours_summary),
        dismissText = stringResource(id = R.string.dismiss),
        onDismiss = dismiss,
        action = stringResource(id = R.string.TLA_menu_settings),
        onAction = showSettings,
    )
}

@Composable
fun SyncWarningGoogleTasks(
    moreInfo: () -> Unit,
    dismiss: () -> Unit,
) {
    Banner(
        title = stringResource(R.string.sync_warning_google_tasks_title),
        body = stringResource(R.string.sync_warning_google_tasks),
        dismissText = stringResource(id = R.string.dismiss),
        onDismiss = dismiss,
        action = stringResource(id = R.string.button_learn_more),
        onAction = moreInfo,
    )
}

@Composable
fun SyncWarningMicrosoft(
    moreInfo: () -> Unit,
    dismiss: () -> Unit,
) {
    Banner(
        title = stringResource(R.string.sync_warning_microsoft_title),
        body = stringResource(R.string.sync_warning_microsoft),
        dismissText = stringResource(id = R.string.dismiss),
        onDismiss = dismiss,
        action = stringResource(id = R.string.button_learn_more),
        onAction = moreInfo,
    )
}

@Composable
fun AppUpdatedBanner(
    whatsNew: () -> Unit,
    dismiss: () -> Unit,
) {
    Banner(
        title = stringResource(id = R.string.banner_app_updated_title),
        body = stringResource(id = R.string.banner_app_updated_description, BuildConfig.VERSION_NAME),
        dismissText = stringResource(id = R.string.dismiss),
        onDismiss = dismiss,
        action = stringResource(id = R.string.whats_new),
        onAction = whatsNew
    )
}

@Composable
fun BeastModeBanner(
    visible: Boolean,
    showSettings: () -> Unit,
    dismiss: () -> Unit,
) {
    AnimatedBanner(
        visible = visible,
        title = stringResource(id = R.string.hint_customize_edit_title),
        body = stringResource(id = R.string.hint_customize_edit_body),
        dismissText = stringResource(id = R.string.dismiss),
        onDismiss = dismiss,
        action = stringResource(id = R.string.TLA_menu_settings),
        onAction = showSettings,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotificationsDisabledPreview() = TasksTheme {
    NotificationsDisabledBanner(settings = {}, dismiss = {})
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BeastModePreview() = TasksTheme {
    BeastModeBanner(visible = true, showSettings = {}, dismiss = {})
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SubscriptionNagPreview() = TasksTheme {
    SubscriptionNagBanner(subscribe = {}, dismiss = {})
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuietHoursPreview() = TasksTheme {
    QuietHoursBanner(showSettings = {}, dismiss = {})
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MicrosoftWarningPreview() = TasksTheme {
    SyncWarningMicrosoft(moreInfo = {}, dismiss = {})
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GoogleTasksWarningPreview() = TasksTheme {
    SyncWarningGoogleTasks(moreInfo = {}, dismiss = {})
}

@Preview(showBackground = true)
@Preview(showBackground = true, backgroundColor = 0x202124, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppUpdatedBannerPreview() = TasksTheme {
    AppUpdatedBanner(whatsNew = {}, dismiss = {})
}
