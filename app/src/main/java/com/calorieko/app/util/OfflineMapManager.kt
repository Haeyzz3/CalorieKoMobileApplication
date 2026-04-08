package com.calorieko.app.util

import android.content.Context
import android.util.Log
import com.mapbox.common.Cancelable
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.Style
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.bindgen.Value

/**
 * Manages offline map data for the CalorieKo GPS workout tracker.
 *
 * ── How It Works ──
 * Mapbox v11 offline maps use two components that must both be downloaded:
 *
 * 1. **Style Pack**: The visual style resources (fonts, sprites, style definition).
 *    Downloaded via [OfflineManager.loadStylePack]. Identified by style URI.
 *
 * 2. **Tile Region**: The actual map tile data for a geographic area.
 *    Downloaded via [TileStore.loadTileRegion]. Identified by a region ID.
 *
 * When both are cached, the MapView renders fully offline — it reads tiles
 * from the local TileStore instead of fetching them over the network.
 *
 * ── Usage ──
 * ```kotlin
 * // In a ViewModel or after obtaining user's location:
 * val manager = OfflineMapManager(context)
 * manager.downloadRegion(
 *     centerLat = 14.5995,    // User's latitude
 *     centerLng = 120.9842,   // User's longitude
 *     radiusKm = 5.0,         // 5km radius around the user
 *     onProgress = { completed, total -> /* update UI */ },
 *     onComplete = { success -> /* notify user */ }
 * )
 * ```
 *
 * ── Offline Fallback ──
 * The MapView in LogWorkoutScreen/ActivityDetailsScreen uses the **default TileStore**
 * path. As long as this manager downloads tiles to the same default path, the MapView
 * will seamlessly use cached tiles when offline — no code changes needed in the UI.
 */
object OfflineMapManager {

    private const val TAG = "OfflineMapManager"
    private const val TILE_REGION_ID = "calorieko_workout_region"
    private const val STYLE_PACK_METADATA = "calorieko-style-pack"
    private const val TILE_REGION_METADATA = "calorieko-tile-region"

    // Zoom levels optimized for workout tracking:
    // - Min zoom 10: City-wide overview when zooming out
    // - Max zoom 16: Street-level detail for route tracking
    private const val MIN_ZOOM: Byte = 10
    private const val MAX_ZOOM: Byte = 16

    // All three map styles used by the GPS tracker
    private val STYLES_TO_CACHE = listOf(
        Style.DARK,           // Default GPS tracker style
        Style.MAPBOX_STREETS, // "Standard" option
        Style.OUTDOORS        // "Terrain" option
    )

    private val tileStore: TileStore by lazy { TileStore.create() }
    private val offlineManager: OfflineManager by lazy { OfflineManager() }

    // Track active downloads so they can be cancelled
    private var stylePackCancelables = mutableListOf<Cancelable>()
    private var tilePackCancelable: Cancelable? = null

    /**
     * Downloads map tiles and style packs for a circular region around a center point.
     *
     * This should be called proactively while the user has internet — e.g., after
     * they open the GPS workout screen, or from a settings "Cache Maps" button.
     *
     * The download covers all three map styles (Dark, Standard, Terrain) so the
     * user can switch styles while offline.
     *
     * @param centerLat  Latitude of the center point (e.g., user's current location)
     * @param centerLng  Longitude of the center point
     * @param radiusKm   Radius in kilometers to cache around the center (default: 5km)
     * @param onProgress Callback with (completedResources, totalResources)
     * @param onComplete Callback with success status when all downloads finish
     */
    fun downloadRegion(
        context: Context,
        centerLat: Double,
        centerLng: Double,
        radiusKm: Double = 5.0,
        onProgress: ((completed: Long, total: Long) -> Unit)? = null,
        onComplete: ((success: Boolean) -> Unit)? = null
    ) {
        Log.d(TAG, "Starting offline cache: center=($centerLat, $centerLng), radius=${radiusKm}km")

        // Cancel any active downloads
        cancelDownloads()

        val pixelRatio = context.resources.displayMetrics.density

        // ── 1. Download style packs for all three styles ──
        var stylesCompleted = 0
        val totalStyles = STYLES_TO_CACHE.size

        for (styleUri in STYLES_TO_CACHE) {
            val cancelable = offlineManager.loadStylePack(
                styleUri,
                StylePackLoadOptions.Builder()
                    .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                    .metadata(Value(STYLE_PACK_METADATA))
                    .build(),
                { progress ->
                    Log.d(TAG, "StylePack ($styleUri): ${progress.completedResourceCount}/${progress.requiredResourceCount}")
                },
                { expected ->
                    if (expected.isValue) {
                        Log.d(TAG, "StylePack downloaded: $styleUri")
                    }
                    expected.error?.let {
                        Log.e(TAG, "StylePack error ($styleUri): $it")
                    }
                    stylesCompleted++
                    if (stylesCompleted == totalStyles) {
                        Log.d(TAG, "All $totalStyles style packs downloaded.")
                    }
                }
            )
            stylePackCancelables.add(cancelable)
        }

        // ── 2. Create bounding box polygon from center + radius ──
        val boundingBox = createBoundingBox(centerLat, centerLng, radiusKm)

        // ── 3. Create tileset descriptors for all styles ──
        val descriptors = STYLES_TO_CACHE.map { styleUri ->
            offlineManager.createTilesetDescriptor(
                TilesetDescriptorOptions.Builder()
                    .styleURI(styleUri)
                    .pixelRatio(pixelRatio)
                    .minZoom(MIN_ZOOM)
                    .maxZoom(MAX_ZOOM)
                    .build()
            )
        }

        // ── 4. Download tile region ──
        tilePackCancelable = tileStore.loadTileRegion(
            TILE_REGION_ID,
            TileRegionLoadOptions.Builder()
                .geometry(boundingBox)
                .descriptors(descriptors)
                .metadata(Value(TILE_REGION_METADATA))
                .acceptExpired(true)
                .networkRestriction(NetworkRestriction.NONE)
                .build(),
            { progress ->
                val completed = progress.completedResourceCount
                val total = progress.requiredResourceCount
                Log.d(TAG, "TileRegion: $completed/$total")
                onProgress?.invoke(completed, total)
            }
        ) { expected ->
            if (expected.isValue) {
                expected.value?.let { region ->
                    Log.d(TAG, "══ Tile region download COMPLETE: $region ══")
                    onComplete?.invoke(true)
                }
            }
            expected.error?.let { error ->
                Log.e(TAG, "Tile region download FAILED: $error")
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Cancels all active download operations.
     */
    fun cancelDownloads() {
        stylePackCancelables.forEach { it.cancel() }
        stylePackCancelables.clear()
        tilePackCancelable?.cancel()
        tilePackCancelable = null
        Log.d(TAG, "All active downloads cancelled.")
    }

    /**
     * Removes all downloaded offline data (tile regions + style packs).
     * Useful for a "Clear Cache" setting.
     */
    fun clearCache() {
        tileStore.removeTileRegion(TILE_REGION_ID)
        STYLES_TO_CACHE.forEach { styleUri ->
            offlineManager.removeStylePack(styleUri)
        }
        Log.d(TAG, "Offline cache cleared.")
    }

    /**
     * Checks if a tile region has been downloaded.
     *
     * @param onResult Callback with `true` if the region exists, `false` otherwise
     */
    fun isRegionCached(onResult: (Boolean) -> Unit) {
        tileStore.getAllTileRegions { expected ->
            if (expected.isValue) {
                val exists = expected.value?.any { it.id == TILE_REGION_ID } == true
                onResult(exists)
            } else {
                onResult(false)
            }
        }
    }

    // ── Private Helpers ──

    /**
     * Creates a bounding-box [Polygon] from a center coordinate and a radius.
     *
     * Uses the approximation:
     * - 1° latitude  ≈ 111.32 km
     * - 1° longitude ≈ 111.32 km × cos(latitude)
     *
     * @return A GeoJSON [Polygon] representing the bounding box
     */
    private fun createBoundingBox(centerLat: Double, centerLng: Double, radiusKm: Double): Polygon {
        val latOffset = radiusKm / 111.32
        val lngOffset = radiusKm / (111.32 * Math.cos(Math.toRadians(centerLat)))

        val sw = Point.fromLngLat(centerLng - lngOffset, centerLat - latOffset)
        val se = Point.fromLngLat(centerLng + lngOffset, centerLat - latOffset)
        val ne = Point.fromLngLat(centerLng + lngOffset, centerLat + latOffset)
        val nw = Point.fromLngLat(centerLng - lngOffset, centerLat + latOffset)

        // GeoJSON Polygon requires the ring to be closed (first == last point)
        return Polygon.fromLngLats(listOf(listOf(sw, se, ne, nw, sw)))
    }
}
