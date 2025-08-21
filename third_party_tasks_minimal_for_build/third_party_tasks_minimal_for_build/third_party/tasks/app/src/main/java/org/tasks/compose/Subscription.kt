package org.tasks.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.tasks.R
import org.tasks.billing.Sku
import org.tasks.compose.Constants.HALF_KEYLINE
import org.tasks.compose.Constants.KEYLINE_FIRST
import org.tasks.compose.PurchaseText.SubscriptionScreen
import org.tasks.extensions.Context.openUri
import org.tasks.themes.TasksTheme

object PurchaseText {
    private const val POPPER = "\uD83C\uDF89"

    data class CarouselItem(
        val title: Int,
        val icon: Int,
        val description: Int,
        val tint: Boolean = true,
    )

    private val featureList = listOf(
        CarouselItem(
            R.string.tasks_org_account,
            R.drawable.ic_round_icon,
            R.string.upgrade_tasks_org_account_description,
            tint = false
        ),
        CarouselItem(
            R.string.upgrade_more_customization,
            R.drawable.ic_outline_palette_24px,
            R.string.upgrade_more_customization_description
        ),
        CarouselItem(
            R.string.open_source,
            R.drawable.ic_octocat,
            R.string.upgrade_open_source_description
        ),
        CarouselItem(
            R.string.upgrade_desktop_access,
            R.drawable.ic_outline_computer_24px,
            R.string.upgrade_desktop_access_description
        ),
        CarouselItem(
            R.string.davx5,
            R.drawable.ic_davx5_icon_green_bg,
            R.string.davx5_selection_description,
            false
        ),
        CarouselItem(
            R.string.caldav,
            R.drawable.ic_webdav_logo,
            R.string.caldav_selection_description
        ),
        CarouselItem(
            R.string.etesync,
            R.drawable.ic_etesync,
            R.string.etesync_selection_description,
            false
        ),
        CarouselItem(
            R.string.decsync,
            R.drawable.ic_decsync,
            R.string.decsync_selection_description,
            false
        ),
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SubscriptionScreen(
        nameYourPrice: Boolean,
        sliderPosition: Float,
        github: Boolean = false,
        snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
        setPrice: (Float) -> Unit,
        setNameYourPrice: (Boolean) -> Unit,
        subscribe: (Int, Boolean) -> Unit,
        skus: List<Sku>,
        onBack: () -> Unit,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.upgrade_to_pro),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(color = colorResource(R.color.content_background)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GreetingText(R.string.upgrade_blurb_1)
                GreetingText(R.string.upgrade_blurb_2)
                Spacer(Modifier.height(KEYLINE_FIRST))
                val pagerState = rememberPagerState {
                    featureList.size
                }
                HorizontalPager(
                    state = pagerState // Optional: to control the pager's state
                ) { index ->
                    val item = featureList[index]
                    PagerItem(item, nameYourPrice && index == 0)
                }
                Row(
                    Modifier
                        .wrapContentHeight()
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(16.dp)
                        )
                    }
                }
                if (github) {
                    SponsorButton()
                } else {
                    GooglePlayButtons(
                        nameYourPrice = nameYourPrice,
                        sliderPosition = sliderPosition,
                        pagerState = pagerState,
                        setNameYourPrice = setNameYourPrice,
                        setPrice = setPrice,
                        subscribe = subscribe,
                        skus = skus,
                    )
                }
            }
        }
    }

    @Composable
    fun SponsorButton() {
        val context = LocalContext.current
        OutlinedButton(
            onClick = { context.openUri(R.string.url_sponsor) },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            modifier = Modifier.padding(KEYLINE_FIRST, 0.dp, KEYLINE_FIRST, KEYLINE_FIRST)
        ) {
            Row {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_favorite_border_24px),
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.github_sponsor),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @Composable
    fun GreetingText(resId: Int) {
        Text(
            modifier = Modifier.padding(KEYLINE_FIRST, KEYLINE_FIRST, KEYLINE_FIRST, 0.dp),
            text = stringResource(resId),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }

    @Composable
    fun GooglePlayButtons(
        nameYourPrice: Boolean,
        sliderPosition: Float,
        pagerState: PagerState,
        setNameYourPrice: (Boolean) -> Unit,
        setPrice: (Float) -> Unit,
        subscribe: (Int, Boolean) -> Unit,
        skus: List<Sku>,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalDivider(modifier = Modifier.padding(vertical = KEYLINE_FIRST))
            if (nameYourPrice) {
                NameYourPrice(
                    sliderPosition = sliderPosition,
                    setPrice = setPrice,
                    subscribe = subscribe,
                    skus = skus,
                )
            } else {
                TasksAccount(
                    skus = skus,
                    subscribe = subscribe
                )
            }
            Spacer(Modifier.height(KEYLINE_FIRST))
            val scope = rememberCoroutineScope()
            OutlinedButton(
                onClick = {
                    setNameYourPrice(!nameYourPrice)
                    scope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    text = stringResource(R.string.more_options),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = stringResource(R.string.pro_free_trial),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth(.75f)
                    .padding(KEYLINE_FIRST),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    fun PagerItem(
        feature: CarouselItem,
        disabled: Boolean = false,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(250.dp)
                    .height(150.dp)
                    .padding(HALF_KEYLINE),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(feature.icon),
                    contentDescription = null,
                    modifier = Modifier.requiredSize(72.dp),
                    alignment = Alignment.Center,
                    colorFilter = if (feature.tint) {
                        ColorFilter.tint(colorResource(R.color.icon_tint_with_alpha))
                    } else {
                        null
                    }
                )
                Text(
                    text = stringResource(feature.title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp, 4.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.25.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(if (disabled) R.string.account_not_included else feature.description),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (disabled) Color.Red else MaterialTheme.colorScheme.onBackground,
                    style = TextStyle(
                        fontWeight = if (disabled) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        letterSpacing = 0.4.sp
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    @Composable
    fun TasksAccount(
        skus: List<Sku>,
        subscribe: (Int, Boolean) -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KEYLINE_FIRST, 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                PurchaseButton(
                    price = remember(skus) {
                        skus.find { it.productId == "annual_30" }?.price ?: "$30"
                    },
                    popperText = "${stringResource(R.string.save_percent, 16)} $POPPER",
                    onClick = { subscribe(30, false) },
                )
                Spacer(Modifier.width(KEYLINE_FIRST))
                PurchaseButton(
                    price = remember (skus) {
                        skus.find { it.productId == "monthly_03" }?.price ?: "$3"
                    },
                    monthly = true,
                    onClick = { subscribe(3, true) },
                )
            }
        }
    }

    @Composable
    fun PurchaseButton(
        price: String,
        monthly: Boolean = false,
        popperText: String = "",
        onClick: () -> Unit,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { onClick() },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(
                    text = stringResource(
                        if (monthly) R.string.price_per_month_with_currency else R.string.price_per_year_with_currency,
                        price
                    ),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = popperText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    @Composable
    fun NameYourPrice(
        sliderPosition: Float,
        setPrice: (Float) -> Unit,
        subscribe: (Int, Boolean) -> Unit,
        skus: List<Sku>,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                Slider(
                    modifier = Modifier.padding(KEYLINE_FIRST, 0.dp, KEYLINE_FIRST, HALF_KEYLINE),
                    value = sliderPosition,
                    onValueChange = { setPrice(it) },
                    valueRange = 1f..25f,
                    steps = 25,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = colorResource(R.color.text_tertiary),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val price = sliderPosition.toInt()
                PurchaseButton(
                    price = remember (skus, price) {
                        skus
                            .find { it.productId == "annual_${price.toString().padStart(2, '0')}" }
                            ?.price
                            ?: "$$price"
                    },
                    popperText = if (sliderPosition.toInt() >= 7)
                        "${stringResource(R.string.above_average, 16)} $POPPER"
                    else
                        "",
                    onClick = { subscribe(sliderPosition.toInt(), false) },
                )
                if (sliderPosition.toInt() < 3) {
                    Spacer(Modifier.width(KEYLINE_FIRST))
                    PurchaseButton(
                        price = remember (skus, price) {
                            skus
                                .find { it.productId == "monthly_${price.toString().padStart(2, '0')}" }
                                ?.price
                                ?: "$$price"
                        },
                        monthly = true,
                        popperText = "${stringResource(R.string.above_average)} $POPPER",
                        onClick = { subscribe(price, true) },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseDialogPreview() {
    TasksTheme {
        SubscriptionScreen(
            subscribe = { _, _ -> },
            onBack = {},
            nameYourPrice = false,
            sliderPosition = 1f,
            setPrice = {},
            setNameYourPrice = {},
            skus = emptyList(),
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun NameYourPricePreview() {
    TasksTheme {
        SubscriptionScreen(
            subscribe = { _, _ -> },
            onBack = {},
            nameYourPrice = true,
            sliderPosition = 4f,
            setPrice = {},
            setNameYourPrice = {},
            skus = emptyList(),
        )
    }
}
