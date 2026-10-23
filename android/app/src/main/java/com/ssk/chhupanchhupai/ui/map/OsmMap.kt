package com.ssk.chhupanchhupai.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import kotlin.math.max

/** Roughly the middle of India; used until we know where the player is. */
val FALLBACK_CENTER = GeoPoint(20.5937, 78.9629)

/**
 * An OpenStreetMap view (osmdroid) wrapped for Compose. The caller gets the
 * MapView in [onReady] and drives overlays with [MapLayer].
 */
@Composable
fun OsmMap(
    modifier: Modifier = Modifier,
    center: GeoPoint = FALLBACK_CENTER,
    zoom: Double = 16.0,
    onReady: (MapView) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            controller.setZoom(zoom)
            controller.setCenter(center)
            overlays.add(CopyrightOverlay(context))
        }
    }
    val readyCallback by rememberUpdatedState(onReady)
    LaunchedEffect(mapView) { readyCallback(mapView) }
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

/** A replaceable set of overlays on a map (everything except the copyright line). */
class MapLayer(private val map: MapView) {
    private val current = mutableListOf<Overlay>()

    fun replace(overlays: List<Overlay>) {
        map.overlays.removeAll(current)
        current.clear()
        current.addAll(overlays)
        map.overlays.addAll(overlays)
        map.invalidate()
    }
}

/** Polygon/Polyline that ignore taps (no empty info bubbles). */
private class QuietPolygon(map: MapView) : Polygon(map) {
    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean = false
}

private class QuietPolyline(map: MapView) : Polyline(map) {
    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean = false
}

fun zoneCircle(map: MapView, lat: Double, lng: Double, radiusM: Double, color: Color, fillAlpha: Float = 0.12f): Overlay =
    QuietPolygon(map).apply {
        setPoints(Polygon.pointsAsCircle(GeoPoint(lat, lng), radiusM))
        fillPaint.color = color.copy(alpha = fillAlpha).toArgb()
        outlinePaint.color = color.toArgb()
        outlinePaint.strokeWidth = 4f * map.resources.displayMetrics.density
        outlinePaint.isAntiAlias = true
    }

fun trailLine(map: MapView, points: List<GeoPoint>, color: Color): Overlay =
    QuietPolyline(map).apply {
        setPoints(points)
        outlinePaint.color = color.toArgb()
        outlinePaint.strokeWidth = 5f * map.resources.displayMetrics.density
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.isAntiAlias = true
    }

/** A coloured dot with an optional name tag underneath. */
fun dotMarker(
    map: MapView,
    point: GeoPoint,
    color: Color,
    label: String? = null,
    alpha: Float = 1f,
    small: Boolean = false,
): Overlay {
    val (drawable, anchorY) = dotBitmap(map.context, color, label, alpha, small)
    return Marker(map).apply {
        position = point
        icon = drawable
        setAnchor(Marker.ANCHOR_CENTER, anchorY)
        setInfoWindow(null)
        setOnMarkerClickListener { _, _ -> true }
    }
}

private fun dotBitmap(context: Context, color: Color, label: String?, alpha: Float, small: Boolean): Pair<BitmapDrawable, Float> {
    val d = context.resources.displayMetrics.density
    val radius = (if (small) 6f else 10f) * d
    val stroke = (if (small) 2f else 3f) * d
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textSize = 12f * d
        isFakeBoldText = true
    }
    val textWidth = label?.let { textPaint.measureText(it) } ?: 0f
    val pillPad = 6f * d
    val pillHeight = if (label != null) 18f * d else 0f
    val width = max(radius * 2 + stroke * 2, textWidth + pillPad * 2).toInt() + 2
    val dotBox = radius * 2 + stroke * 2
    val height = (dotBox + if (label != null) pillHeight + 2f * d else 0f).toInt() + 2
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = width / 2f
    val cy = dotBox / 2f
    val a = (alpha * 255).toInt()
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb(); this.alpha = a }
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        this.alpha = a
        style = Paint.Style.STROKE
        strokeWidth = stroke
    }
    canvas.drawCircle(cx, cy, radius, fill)
    canvas.drawCircle(cx, cy, radius, ring)
    if (label != null) {
        val top = dotBox + 2f * d
        val pill = RectF(cx - textWidth / 2 - pillPad, top, cx + textWidth / 2 + pillPad, top + pillHeight)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.argb((0.8f * a).toInt(), 14, 16, 38)
        }
        canvas.drawRoundRect(pill, pillHeight / 2, pillHeight / 2, pillPaint)
        textPaint.alpha = a
        val baseline = pill.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(label, cx - textWidth / 2, baseline, textPaint)
    }
    return BitmapDrawable(context.resources, bitmap) to (cy / height)
}

/** Zooms to show every point (after the map has been laid out). */
fun MapView.fitPoints(points: List<GeoPoint>, paddingDp: Int = 48) {
    if (points.isEmpty()) return
    val run: () -> Unit = {
        if (points.size == 1) {
            controller.setZoom(17.0)
            controller.setCenter(points[0])
        } else {
            val box = BoundingBox.fromGeoPoints(points)
            zoomToBoundingBox(box.increaseByScale(1.15f), false, (paddingDp * resources.displayMetrics.density).toInt())
        }
    }
    if (isLayoutOccurred) run() else addOnFirstLayoutListener { _, _, _, _, _ -> run() }
}
