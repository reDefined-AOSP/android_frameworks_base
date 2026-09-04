package com.android.systemui.qs.panels.ui.compose.infinitegrid

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.systemui.qs.panels.ui.viewmodel.TileUiState
import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.statusbar.pipeline.shared.ui.viewmodel.InternetTileViewModel
import com.android.systemui.statusbar.pipeline.shared.ui.model.InternetTileModel
import com.android.systemui.res.R
import com.android.compose.ui.graphics.painter.rememberDrawablePainter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InternetFeaturedTileContent(
    uiState: TileUiState,
    colors: TileColors,
    viewModel: InternetTileViewModel,
    modifier: Modifier = Modifier,
) {
    val wifiModel by viewModel.wifiIconFlow.collectAsStateWithLifecycle(initialValue = InternetTileViewModel.NOT_CONNECTED_NETWORKS_UNAVAILABLE)
    val mobileModel by viewModel.mobileIconFlow.collectAsStateWithLifecycle(initialValue = InternetTileViewModel.NOT_CONNECTED_NETWORKS_UNAVAILABLE)

    val pagerState = rememberPagerState(pageCount = { 2 })

    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        when (page) {
            0 -> {
                InternetPageContent(
                    model = wifiModel,
                    colors = colors,
                    defaultIconId = R.drawable.ic_qs_no_internet_unavailable
                )
            }
            1 -> {
                InternetPageContent(
                    model = mobileModel,
                    colors = colors,
                    defaultIconId = R.drawable.ic_qs_no_internet_unavailable
                )
            }
        }
    }
}

@Composable
private fun InternetPageContent(
    model: InternetTileModel,
    colors: TileColors,
    defaultIconId: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        val context = LocalContext.current
        val drawable = remember(model, context) {
            if (model.icon is QSTileImpl.ResourceIcon) {
                context.getDrawable((model.icon as QSTileImpl.ResourceIcon).resId)
            } else if (model.icon != null) {
                model.icon?.getDrawable(context)
            } else if (model.iconId != null) {
                context.getDrawable(model.iconId!!)
            } else {
                context.getDrawable(defaultIconId)
            }
        }

        Image(
            painter = rememberDrawablePainter(drawable),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.icon),
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(36.dp)
        )

        val label = when (model) {
            is InternetTileModel.Active -> model.secondaryTitle ?: ""
            is InternetTileModel.Inactive -> model.secondaryTitle ?: ""
            else -> ""
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = label.toString(),
                color = colors.label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.End
            )
        }
    }
}
