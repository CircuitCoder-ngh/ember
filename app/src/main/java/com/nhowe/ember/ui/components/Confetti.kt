package com.nhowe.ember.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.nhowe.ember.ui.theme.GoalPalette
import kotlin.random.Random

/** Change [id] to fire a new burst. [count] particles rain from the top third of the box. */
data class ConfettiTrigger(val id: Long, val count: Int = 120, val originY: Float = 0.35f)

private class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var rot: Float, val vr: Float, val color: Color, val size: Float, val round: Boolean, var life: Float,
)

@Composable
fun ConfettiOverlay(trigger: ConfettiTrigger?, modifier: Modifier = Modifier) {
    val particles = remember { ArrayList<Particle>() }
    var frame by remember { mutableIntStateOf(0) }
    var box by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(trigger) {
        val t = trigger ?: return@LaunchedEffect
        if (box == IntSize.Zero) return@LaunchedEffect
        val rnd = Random(t.id)
        val w = box.width.toFloat()
        val h = box.height.toFloat()
        repeat(t.count) {
            particles += Particle(
                x = w / 2f + (rnd.nextFloat() - 0.5f) * w * 0.5f,
                y = h * t.originY,
                vx = (rnd.nextFloat() - 0.5f) * w * 0.045f,
                vy = -(rnd.nextFloat() * h * 0.028f + h * 0.008f),
                rot = rnd.nextFloat() * 360f,
                vr = (rnd.nextFloat() - 0.5f) * 24f,
                color = GoalPalette[rnd.nextInt(GoalPalette.size)],
                size = w * (0.012f + rnd.nextFloat() * 0.014f),
                round = rnd.nextInt(3) == 0,
                life = 2.2f + rnd.nextFloat(),
            )
        }
        var last = withFrameNanos { it }
        while (particles.isNotEmpty()) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                last = now
                val gravity = h * 0.0016f
                val it = particles.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.vy += gravity * dt * 60f
                    p.vx *= 0.985f
                    p.x += p.vx * dt * 60f
                    p.y += p.vy * dt * 60f
                    p.rot += p.vr * dt * 60f
                    p.life -= dt
                    if (p.life <= 0f || p.y > h + p.size * 2) it.remove()
                }
                frame++
            }
        }
    }

    Canvas(modifier.fillMaxSize().onSizeChanged { box = it }) {
        @Suppress("UNUSED_EXPRESSION") frame
        for (p in particles) {
            val alpha = p.life.coerceIn(0f, 1f)
            rotate(p.rot, pivot = Offset(p.x, p.y)) {
                if (p.round) drawCircle(p.color, radius = p.size / 2f, center = Offset(p.x, p.y), alpha = alpha)
                else drawRect(p.color, topLeft = Offset(p.x - p.size / 2f, p.y - p.size * 0.3f), size = Size(p.size, p.size * 0.6f), alpha = alpha)
            }
        }
    }
}
