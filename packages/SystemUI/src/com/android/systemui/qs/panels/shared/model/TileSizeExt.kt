package com.android.systemui.qs.panels.shared.model

import com.android.systemui.qs.panels.shared.model.TileSize

// Extend SizedTile to understand 2D TileSize, defaulting to 1x1 or 2x1 based on width
fun <T> SizedTile<T>.toTileSize(): TileSize {
    // If it's a Media Tile, it should be 2x2. We will add that specific check in Phase 3.
    // For now, Alarm and Home Controls are just 2x1 (Large), we'll upgrade them to 2x2 later.
    return if (width == 1) TileSize.SMALL else TileSize.LARGE
}
