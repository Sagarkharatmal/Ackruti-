package com.ssk.chhupanchhupai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.ssk.chhupanchhupai.data.ResultPlayer
import com.ssk.chhupanchhupai.data.Results
import com.ssk.chhupanchhupai.data.Role
import com.ssk.chhupanchhupai.data.Status
import com.ssk.chhupanchhupai.ui.formatClock
import com.ssk.chhupanchhupai.ui.formatDistance
import java.io.File
import kotlin.math.cos
import kotlin.math.max

/**
 * A 1080x1920 "story" image of one player's round: their route, headline stat
 * and the event / sponsor, ready for Instagram, WhatsApp status, etc.
 */
object ShareCard {
    private const val W = 1080
    private const val H = 1920
    private val SAFFRON = 0xFFFF9933.toInt()
    private val MARIGOLD = 0xFFFFC145.toInt()
    private val TEAL = 0xFF2EC4B6.toInt()
    private val RED = 0xFFFF4D6D.toInt()
    private val WHITE = 0xFFF4F4FA.toInt()
    private val MUTED = 0xFFB4B7D6.toInt()

    fun headline(me: ResultPlayer, everyone: List<ResultPlayer>): Pair<String, String> = when {
        me.role == Role.SEEKER -> "${me.catches} CATCH${if (me.catches == 1) "" else "ES"}" to "as a seeker"
        me.status == Status.CAUGHT -> {
            val by = everyone.firstOrNull { it.id == me.caughtBy }?.name
            "CAUGHT AFTER ${formatClock(me.survivedMs ?: 0)}" to (if (by != null) "by $by" else "as a hider")
        }
        else -> "SURVIVED ${formatClock(me.survivedMs ?: 0)}" to "never found!"
    }

    fun render(context: Context, me: ResultPlayer, results: Results, eventTitle: String?, sponsor: String?): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bitmap)
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFF0E1026.toInt(), 0xFF2A1B5C.toInt(), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), bg)

        val bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        fun text(size: Float, color: Int, face: Typeface = bold) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = face
        }

        c.drawText("CHHUPAN", 80f, 190f, text(110f, SAFFRON))
        c.drawText("CHHUPAI", 80f, 310f, text(110f, MARIGOLD))
        val roleColor = if (me.role == Role.SEEKER) RED else TEAL
        c.drawText(if (me.role == Role.SEEKER) "SEEKER" else "HIDER", 84f, 400f, text(48f, roleColor))
        val winner = when (results.winner) {
            Role.SEEKER -> "Seekers won the round"
            Role.HIDER -> "Hiders won the round"
            else -> "Round over"
        }
        c.drawText(winner, 84f, 460f, text(40f, MUTED, Typeface.DEFAULT))

        // Route map: the player's trail scaled into a rounded panel.
        val panel = RectF(80f, 520f, W - 80f, 1320f)
        c.drawRoundRect(panel, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF })
        val pts = me.trail.filter { it.size >= 2 }
        if (pts.size >= 2) {
            val lat0 = pts.map { it[0] }.average()
            val k = cos(Math.toRadians(lat0))
            val xs = pts.map { it[1] * k }
            val ys = pts.map { it[0] }
            val minX = xs.min()
            val maxX = xs.max()
            val minY = ys.min()
            val maxY = ys.max()
            val inset = 70f
            val bw = panel.width() - inset * 2
            val bh = panel.height() - inset * 2
            val span = max(max(maxX - minX, maxY - minY), 1e-6)
            val scale = minOf(bw / span, bh / span)
            val offX = panel.left + inset + (bw - ((maxX - minX) * scale).toFloat()) / 2
            val offY = panel.top + inset + (bh - ((maxY - minY) * scale).toFloat()) / 2
            val path = Path()
            pts.indices.forEach { i ->
                val x = offX + ((xs[i] - minX) * scale).toFloat()
                val y = offY + ((maxY - ys[i]) * scale).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 14f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = roleColor
            })
            val endX = offX + ((xs.last() - minX) * scale).toFloat()
            val endY = offY + ((maxY - ys.last()) * scale).toFloat()
            c.drawCircle(endX, endY, 26f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE })
            c.drawCircle(endX, endY, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = roleColor })
        } else {
            val p = text(44f, MUTED, Typeface.DEFAULT).apply { textAlign = Paint.Align.CENTER }
            c.drawText("No route recorded", panel.centerX(), panel.centerY(), p)
        }

        val (big, small) = headline(me, results.players)
        c.drawText(big, 80f, 1450f, text(if (big.length > 18) 76f else 92f, WHITE))
        c.drawText(small, 84f, 1520f, text(46f, MUTED, Typeface.DEFAULT))
        c.drawText("${formatDistance(me.distanceM)} moved · ${me.name}", 84f, 1600f, text(46f, SAFFRON))

        val footer = text(38f, MUTED, Typeface.DEFAULT)
        var y = 1720f
        if (!eventTitle.isNullOrBlank()) {
            c.drawText(eventTitle, 84f, y, text(42f, WHITE))
            y += 56f
        }
        if (!sponsor.isNullOrBlank()) {
            c.drawText("Presented by $sponsor", 84f, y, footer)
            y += 52f
        }
        c.drawText("Play real-life hide & seek · Chhupan Chhupai", 84f, max(y, 1850f), footer)
        return bitmap
    }

    /** Saves the card and opens the Android share sheet. */
    fun share(context: Context, bitmap: Bitmap) {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "chhupan-chhupai-round.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My Chhupan Chhupai round! 🙈")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share your round").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
