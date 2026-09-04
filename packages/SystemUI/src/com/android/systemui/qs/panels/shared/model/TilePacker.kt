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

package com.android.systemui.qs.panels.shared.model

/**
 * A 2D packing engine for QS tiles. Supports 1x1, 2x1, and 2x2 tile sizes.
 *
 * The packer assigns each tile an (x, y) coordinate on a [columns]-wide grid. It scans
 * left-to-right, top-to-bottom to find the first available slot that can fit the tile. Remaining
 * holes are filled with spacers to maintain correct visual alignment.
 */

/** The size of a tile in grid units */
data class TileSize(val colSpan: Int, val rowSpan: Int) {
    companion object {
        val SMALL = TileSize(1, 1) // 1x1 icon
        val LARGE = TileSize(2, 1) // 2x1 pill
        val FEATURED = TileSize(2, 2) // 2x2 widget
    }
}

/** A packed tile with its 2D coordinate and size */
data class PackedTile<T>(
    val tile: T,
    val col: Int,
    val row: Int,
    val size: TileSize,
)

/**
 * Packs a list of sized tiles into pages.
 *
 * @param tiles The tiles to pack. Each tile is a pair of (T, TileSize).
 * @param columns The number of columns (usually 4).
 * @param rowsPerPage The number of rows per page (usually 4).
 * @return A list of pages, where each page is a list of [PackedTile].
 */
fun <T> packTilesIntoPages(
    tiles: List<Pair<T, TileSize>>,
    columns: Int = 4,
    rowsPerPage: Int = 4,
): List<List<PackedTile<T>>> {
    val pages = mutableListOf<List<PackedTile<T>>>()
    var allTiles = tiles.toMutableList()

    while (allTiles.isNotEmpty()) {
        val (packedPage, remaining) = packOnePage(allTiles, columns, rowsPerPage)
        pages.add(packedPage)
        allTiles = remaining.toMutableList()
    }

    if (pages.isEmpty()) pages.add(emptyList())
    return pages
}

/**
 * Packs as many tiles as possible onto a single [rowsPerPage]-row page.
 *
 * @return A pair of (packed tiles for this page, remaining tiles that didn't fit).
 */
private fun <T> packOnePage(
    tiles: List<Pair<T, TileSize>>,
    columns: Int,
    rowsPerPage: Int,
): Pair<List<PackedTile<T>>, List<Pair<T, TileSize>>> {
    // 2D boolean grid: true = occupied
    val grid = Array(rowsPerPage) { BooleanArray(columns) { false } }
    val packedTiles = mutableListOf<PackedTile<T>>()
    val remainingTiles = mutableListOf<Pair<T, TileSize>>()

    for ((tile, size) in tiles) {
        val placed = tryPlace(grid, tile, size, columns, rowsPerPage, packedTiles)
        if (!placed) {
            remainingTiles.add(tile to size)
        }
    }

    return packedTiles to remainingTiles
}

private fun <T> tryPlace(
    grid: Array<BooleanArray>,
    tile: T,
    size: TileSize,
    columns: Int,
    rowsPerPage: Int,
    packedTiles: MutableList<PackedTile<T>>,
): Boolean {
    for (row in 0 until rowsPerPage) {
        for (col in 0 until columns) {
            if (canPlace(grid, col, row, size, columns, rowsPerPage)) {
                occupy(grid, col, row, size)
                packedTiles.add(PackedTile(tile, col, row, size))
                return true
            }
        }
    }
    return false
}

private fun canPlace(
    grid: Array<BooleanArray>,
    col: Int,
    row: Int,
    size: TileSize,
    columns: Int,
    rowsPerPage: Int,
): Boolean {
    if (col + size.colSpan > columns) return false
    if (row + size.rowSpan > rowsPerPage) return false
    for (r in row until row + size.rowSpan) {
        for (c in col until col + size.colSpan) {
            if (grid[r][c]) return false
        }
    }
    return true
}

private fun occupy(
    grid: Array<BooleanArray>,
    col: Int,
    row: Int,
    size: TileSize,
) {
    for (r in row until row + size.rowSpan) {
        for (c in col until col + size.colSpan) {
            grid[r][c] = true
        }
    }
}
