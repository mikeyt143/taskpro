package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.compose.Constants
import org.tasks.data.dao.LocationDao
import org.tasks.data.entity.Place
import org.tasks.data.mapPosition
import org.tasks.extensions.formatNumber
import org.tasks.filters.Filter
import org.tasks.filters.PlaceFilter
import org.tasks.location.MapFragment
import org.tasks.preferences.Preferences
import org.tasks.themes.TasksIcons
import org.tasks.themes.TasksTheme
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class PlaceSettingsActivity : BaseListSettingsActivity(),
    MapFragment.MapFragmentCallback {

    companion object {
        const val EXTRA_PLACE = "extra_place"
        private const val MIN_RADIUS = 75
        private const val MAX_RADIUS = 1000
        private const val STEP = 25
    }

    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var map: MapFragment
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var place: Place
    override val defaultIcon = TasksIcons.PLACE

    private val sliderPos = mutableFloatStateOf(100f)
    private lateinit var viewHolder: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent?.hasExtra(EXTRA_PLACE) != true) {
            finish()
        }

        val extra: Place? = intent?.getParcelableExtra(EXTRA_PLACE)
        if (extra == null) {
            finish()
            return
        }

        place = extra

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            baseViewModel.setTitle(place.displayName)
            baseViewModel.setColor(place.color)
            baseViewModel.setIcon(place.icon ?: defaultIcon)
        }

        sliderPos.floatValue = (place.radius / STEP * STEP).toFloat()

        setContent {
            TasksTheme {
                BaseSettingsContent {
                    Row(
                        modifier = Modifier
                            .requiredHeight(56.dp)
                            .fillMaxWidth()
                            .padding(horizontal = Constants.KEYLINE_FIRST),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.geofence_radius))
                        Row(horizontalArrangement = Arrangement.End) {
                            Text(getString(
                                R.string.location_radius_meters,
                                locale.formatNumber(sliderPos.floatValue.roundToInt()
                                )))
                        }
                    }
                    Slider(
                        modifier = Modifier.fillMaxWidth(),
                        value = sliderPos.floatValue,
                        valueRange = (MIN_RADIUS.toFloat()..MAX_RADIUS.toFloat()),
                        steps = (MAX_RADIUS - MIN_RADIUS) / STEP - 1,
                        onValueChange = { sliderPos.floatValue = it; updateGeofenceCircle(sliderPos.floatValue) },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                            inactiveTrackColor = colorResource(id = R.color.text_tertiary),
                            activeTickColor = MaterialTheme.colorScheme.secondary,
                            inactiveTickColor = colorResource(id = R.color.text_tertiary),
                            disabledActiveTrackColor = MaterialTheme.colorScheme.secondary,
                            disabledInactiveTrackColor = colorResource(id = R.color.text_tertiary),
                            disabledActiveTickColor = MaterialTheme.colorScheme.secondary,
                            disabledInactiveTickColor = colorResource(id = R.color.text_tertiary)
                        )
                    )
                    val dark = isSystemInDarkTheme()
                    AndroidView(
                        factory = { ctx ->
                            viewHolder = LinearLayout(ctx).apply {
                                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                                id = R.id.map
                            }
                            map.init(
                                this@PlaceSettingsActivity,
                                this@PlaceSettingsActivity,
                                dark,
                                viewHolder
                            )
                            viewHolder
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeight(300.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

    override fun hasChanges() = baseViewModel.title != place.displayName
                    || baseViewModel.color != place.color
                    || baseViewModel.icon != (place.icon ?: TasksIcons.PLACE)

    override suspend fun save() {
        val newName: String = baseViewModel.title

        if (isNullOrEmpty(newName)) {
            baseViewModel.setError(getString(R.string.name_cannot_be_empty))
            return
        }

        place = place.copy(
            name = newName,
            color = baseViewModel.color,
            icon = baseViewModel.icon,
            radius = sliderPos.floatValue.roundToInt(),
        )
        locationDao.update(place)
        localBroadcastManager.broadcastRefresh()
        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_RELOAD)
                        .putExtra(MainActivity.OPEN_FILTER, PlaceFilter(place)))
        finish()
    }

    override val filter: Filter
        get() = PlaceFilter(place)

    override val toolbarTitle: String
        get() = place.address ?: place.displayName

    override suspend fun delete() {
        locationDao.deleteGeofencesByPlace(place.uid!!)
        locationDao.delete(place)
        setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
        localBroadcastManager.broadcastRefreshList()
        finish()
    }

    override fun onMapReady(mapFragment: MapFragment) {
        map = mapFragment
        map.setMarkers(listOf(place))
        map.disableGestures()
        map.movePosition(place.mapPosition, false)
        updateGeofenceCircle(sliderPos.floatValue)
    }

    override fun onPlaceSelected(place: Place) {}

    private fun updateGeofenceCircle(radius: Float) {
        val radius = radius.toDouble()
        val zoom = when (radius) {
            in 0f..300f -> 15f
            in 300f..500f -> 14.5f
            in 500f..700f -> 14.25f
            in 700f..900f -> 14f
            else -> 13.75f
        }
        map.showCircle(radius, place.latitude, place.longitude)
        map.movePosition(
            mapPosition = place.mapPosition.copy(zoom = zoom),
            animate = true,
        )
    }
}