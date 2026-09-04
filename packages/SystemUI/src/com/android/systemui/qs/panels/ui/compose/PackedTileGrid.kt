/*
 * Copyright (C) 2025 reDefined AOSP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs.panels.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.systemui.qs.panels.shared.model.PackedTile
import com.android.systemui.qs.panels.shared.model.TileSize

/**
 * A 2D custom Layout composable that renders tiles on a [columns]-column grid.
 *
 * Each tile is placed at its exact (col, row) coordinate from [PackedTile]. Supports 1x1, 2x1,
 * and 2x2 tiles. Empty cells are automatically left blank — no Spacer needed.
 *
 * @param packedTiles The pre-packed tiles for this page (from [packTilesIntoPages]).
 * @param columns Number of columns (usually 4).
 * @param rowsPerPage Max rows per page (usually 4).
 * @param tileGap The spacing between tiles.
 * @param modifier Modifier for the entire grid container.
 * @param tileContent A composable lambda that draws the content inside each tile cell.
 */
@Composable
fun <T> PackedTileGrid(
    packedTiles: List<PackedTile<T>>,
    columns: Int = 4,
    rowsPerPage: Int = 4,
    tileGap: Dp = 8.dp,
    modifier: Modifier = Modifier,
    tileContent: @Composable (tile: T, size: TileSize) -> Unit,
) {
    val tiles = remember(packedTiles) { packedTiles }

    Layout(
        modifier = modifier,
        content = {
            for (packed in tiles) {
                Box {
                    tileContent(packed.tile, packed.size)
                }
            }
        },
    ) { measurables, constraints ->
        val totalWidth = constraints.maxWidth
        val gapPx = tileGap.roundToPx()

        // Calculate cell width and height from total width, columns, and gaps
        val cellWidth = (totalWidth - gapPx * (columns - 1)) / columns
        val cellHeight = cellWidth // Square cells

        val totalHeight = rowsPerPage * cellHeight + (rowsPerPage - 1) * gapPx

        val placeables = measurables.mapIndexed { index, measurable ->
            val packed = tiles[index]
            val w = packed.size.colSpan * cellWidth + (packed.size.colSpan - 1) * gapPx
            val h = packed.size.rowSpan * cellHeight + (packed.size.rowSpan - 1) * gapPx
            measurable.measure(
                constraints.copy(minWidth = w, maxWidth = w, minHeight = h, maxHeight = h)
            )
        }

        layout(totalWidth, totalHeight) {
            placeables.forEachIndexed { index, placeable ->
                val packed = tiles[index]
                val x = packed.col * (cellWidth + gapPx)
                val y = packed.row * (cellHeight + gapPx)
                placeable.placeRelative(x, y)
            }
        }
    }
}
