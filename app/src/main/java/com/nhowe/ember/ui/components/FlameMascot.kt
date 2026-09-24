package com.nhowe.ember.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.DayState
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.ui.theme.EmberOrange
import com.nhowe.ember.ui.theme.Gold
import com.nhowe.ember.ui.theme.Ice
import kotlin.math.PI
import kotlin.math.sin

enum class FlameState { SLEEPING, EMBER, WARM, BLAZING, FROZEN }

fun flameStateFor(snapshot: EngineSnapshot): FlameState {
    val yesterday = snapshot.dayStates[snapshot.today.minusDays(1)]
    return when {
        yesterday == DayState.FROZEN -> FlameState.FROZEN
        !snapshot.todayPlan.hasGoals && snapshot.streak.current == 0 -> FlameState.SLEEPING
        snapshot.streak.current >= 7 -> FlameState.BLAZING
        snapshot.streak.current >= 1 -> FlameState.WARM
        else -> FlameState.EMBER
    }
}

/** A Canvas-drawn flame that breathes. Bigger and livelier as the streak grows. */
@Composable
fun FlameMascot(state: FlameState, modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val transition = rememberInfiniteTransition(label = "flame")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(if (state == FlameState.BLAZING) 900 else 1500, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val (outer, inner, core) = when (state) {
        FlameState.FROZEN -> Triple(Ice, Color(0xFFBAE6FD), Color.White)
        FlameState.SLEEPING -> Triple(Color(0xFF6B4A3A), Color(0xFF8C5A44), Color(0xFFB07A5E))
        FlameState.EMBER -> Triple(EmberOrange.copy(alpha = 0.75f), Gold.copy(alpha = 0.8f), Color(0xFFFFF1C9))
        else -> Triple(EmberOrange, Gold, Color(0xFFFFF7DB))
    }
    val scale = when (state) {
        FlameState.SLEEPING -> 0.62f
        FlameState.EMBER -> 0.78f
        FlameState.WARM -> 0.92f
        FlameState.BLAZING -> 1.05f
        FlameState.FROZEN -> 0.9f
    }
    val wobble = if (state == FlameState.SLEEPING) 0.02f else if (state == FlameState.BLAZING) 0.09f else 0.05f

    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val baseY = h * 0.92f
        val sway = sin(phase) * wobble
        val flick = 1f + sin(phase * 2f + 1f) * wobble * 0.6f

        fun flame(width: Float, height: Float, color: Color, lean: Float) {
            val tipX = cx + lean * width
            val tipY = baseY - height * flick
            val path = Path().apply {
                moveTo(tipX, tipY)
                cubicTo(tipX + width * 0.55f, tipY + height * 0.30f, cx + width * 0.62f, baseY - height * 0.42f, cx + width * 0.55f, baseY - height * 0.22f)
                cubicTo(cx + width * 0.5f, baseY - height * 0.05f, cx + width * 0.35f, baseY, cx, baseY)
                cubicTo(cx - width * 0.35f, baseY, cx - width * 0.5f, baseY - height * 0.05f, cx - width * 0.55f, baseY - height * 0.22f)
                cubicTo(cx - width * 0.62f, baseY - height * 0.42f, tipX - width * 0.55f, tipY + height * 0.30f, tipX, tipY)
                close()
            }
            drawPath(path, color)
        }

        val bodyW = w * 0.38f * scale
        val bodyH = h * 0.82f * scale
        flame(bodyW, bodyH, outer, sway)
        flame(bodyW * 0.62f, bodyH * 0.62f, inner, sway * 1.4f)
        flame(bodyW * 0.32f, bodyH * 0.34f, core, sway * 1.8f)

        if (state == FlameState.BLAZING) {
            repeat(4) { i ->
                val t = ((phase / (2 * PI).toFloat()) + i * 0.25f) % 1f
                val sx = cx + sin(t * 9f + i) * bodyW * 0.7f
                val sy = baseY - bodyH * 0.55f - t * bodyH * 0.9f
                drawCircle(Gold.copy(alpha = (1f - t) * 0.9f), radius = (3.5f - t * 2.5f).dp.toPx(), center = Offset(sx, sy))
            }
        }
        if (state == FlameState.SLEEPING) {
            // a couple of drifting "z"s as dots so the flame reads as asleep
            repeat(2) { i ->
                val t = ((phase / (2 * PI).toFloat()) + i * 0.5f) % 1f
                drawCircle(Color(0xFF9AA0B5).copy(alpha = (1f - t) * 0.6f), radius = (2f + i).dp.toPx(), center = Offset(cx + bodyW * 0.9f + t * 10f, baseY - bodyH - t * 30f))
            }
        }
    }
}
