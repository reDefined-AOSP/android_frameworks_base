/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.systemui.qs.panels.domain.interactor

import com.android.internal.logging.UiEventLogger
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.log.LogBuffer
import com.android.systemui.log.core.LogLevel
import com.android.systemui.qs.QSEditEvent
import com.android.systemui.qs.panels.data.repository.DefaultLargeTilesRepository
import com.android.systemui.qs.panels.shared.model.PanelsLog
import com.android.systemui.qs.pipeline.domain.interactor.CurrentTilesInteractor
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.qs.pipeline.shared.metricSpec
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** Interactor for retrieving the list of [TileSpec] to be displayed as icons and resizing icons. */
@SysUISingleton
class IconTilesInteractor
@Inject
constructor(
    private val repo: DefaultLargeTilesRepository,
    private val currentTilesInteractor: CurrentTilesInteractor,
    private val preferencesInteractor: QSPreferencesInteractor,
    private val uiEventLogger: UiEventLogger,
    @PanelsLog private val logBuffer: LogBuffer,
    @Background private val scope: CoroutineScope,
) {

    val largeTilesSpecs =
        preferencesInteractor.largeTilesSpecs
            .onEach { logChange("Large", it) }
            .stateIn(scope, SharingStarted.Eagerly, repo.defaultLargeTiles)

    val featuredTilesSpecs =
        preferencesInteractor.featuredTilesSpecs
            .onEach { logChange("Featured", it) }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val FEATURED_WHITELIST = setOf(
        "internet"
    )

    fun isIconTile(spec: TileSpec): Boolean = !largeTilesSpecs.value.contains(spec) && !featuredTilesSpecs.value.contains(spec)
    fun isLargeTile(spec: TileSpec): Boolean = largeTilesSpecs.value.contains(spec)
    fun isFeaturedTile(spec: TileSpec): Boolean = featuredTilesSpecs.value.contains(spec)

    /** Set the large tiles to be [specs] */
    fun setLargeTiles(specs: Set<TileSpec>) {
        preferencesInteractor.setLargeTilesSpecs(specs)
    }

    /** Remove [specs] from the current set of large tiles */
    fun removeLargeTiles(specs: Set<TileSpec>) {
        preferencesInteractor.removeLargeTilesSpecs(specs)
    }

    fun resetToDefault() {
        preferencesInteractor.setLargeTilesSpecs(repo.defaultLargeTiles)
        preferencesInteractor.setFeaturedTilesSpecs(emptySet())
    }

    fun cycleSize(spec: TileSpec) {
        if (!isCurrent(spec)) return
        val isSmall = isIconTile(spec)
        val isLarge = isLargeTile(spec)
        val isFeatured = isFeaturedTile(spec)
        val canBeFeatured = FEATURED_WHITELIST.contains(spec.spec)
        if (isSmall) {
            preferencesInteractor.setLargeTilesSpecs(largeTilesSpecs.value + spec)
        } else if (isLarge) {
            if (canBeFeatured) {
                preferencesInteractor.setLargeTilesSpecs(largeTilesSpecs.value - spec)
                preferencesInteractor.setFeaturedTilesSpecs(featuredTilesSpecs.value + spec)
            } else {
                preferencesInteractor.setLargeTilesSpecs(largeTilesSpecs.value - spec)
            }
        } else if (isFeatured) {
            preferencesInteractor.setFeaturedTilesSpecs(featuredTilesSpecs.value - spec)
        }
    }

    fun resize(spec: TileSpec, toIcon: Boolean) {
        if (!isCurrent(spec)) {
            return
        }

        val isSmall = isIconTile(spec)
        val isLarge = isLargeTile(spec)
        val isFeatured = isFeaturedTile(spec)
        val canBeFeatured = FEATURED_WHITELIST.contains(spec.spec)

        if (toIcon && !isSmall) {
            // Shrink from Featured -> Large, or Large -> Small
            if (isFeatured) {
                preferencesInteractor.setFeaturedTilesSpecs(featuredTilesSpecs.value - spec)
                preferencesInteractor.setLargeTilesSpecs(largeTilesSpecs.value + spec)
            } else if (isLarge) {
                preferencesInteractor.setLargeTilesSpecs(largeTilesSpecs.value - spec)
            }
            uiEventLogger.log(
                /* event= */ QSEditEvent.QS_EDIT_RESIZE_SMALL,
                /* uid= */ 0,
                /* packageName= */ spec.metricSpec,
            )
        } else if (!toIcon && (isSmall || isLarge) && !isFeatured) {
            // Expand from Small -> Large, or Large -> Featured (if whitelisted)
            if (isSmall) {
                preferencesInteractor.setLargeTilesSpecs(largeTilesSpecs.value + spec)
            } else if (isLarge) {
                if (canBeFeatured) {
                    preferencesInteractor.setLargeTilesSpecs(largeTilesSpecs.value - spec)
                    preferencesInteractor.setFeaturedTilesSpecs(featuredTilesSpecs.value + spec)
                } else {
                    // Normal tiles cycle back to Small if they try to grow past Large
                    preferencesInteractor.setLargeTilesSpecs(largeTilesSpecs.value - spec)
                }
            }
            uiEventLogger.log(
                /* event= */ QSEditEvent.QS_EDIT_RESIZE_LARGE,
                /* uid= */ 0,
                /* packageName= */ spec.metricSpec,
            )
        }
    }

    private fun isCurrent(spec: TileSpec): Boolean {
        return currentTilesInteractor.currentTilesSpecs.contains(spec)
    }

    private fun logChange(type: String, specs: Set<TileSpec>) {
        logBuffer.log(
            LOG_BUFFER_LARGE_TILES_SPECS_CHANGE_TAG,
            LogLevel.DEBUG,
            { str1 = specs.toString(); str2 = type },
            { "$str2 tiles change: $str1" },
        )
    }

    private companion object {
        const val LOG_BUFFER_LARGE_TILES_SPECS_CHANGE_TAG = "LargeTilesSpecsChange"
    }
}
