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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.DayState
import com.nhowe.ember.domain.model.EngineSnapshot
import com.nhowe.ember.domain.model.FlameForm
import com.nhowe.ember.domain.model.FlameSkin
import com.nhowe.ember.ui.theme.Ice
import kotlin.math.PI
import kotlin.math.cos
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

/**
 * The mascot. Its mood ([state]) comes from the streak, its shape ([form]) from the level, and its
 * colours from the chosen [skin]. Frozen and sleeping moods override the skin.
 */
@Composable
fun FlameMascot(
    state: FlameState,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    form: FlameForm = FlameForm.EMBER,
    skin: FlameSkin = FlameSkin.CLASSIC,
) {
    val transition = rememberInfiniteTransition(label = "flame")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(if (state == FlameState.BLAZING) 900 else 1500, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val skinOuter = Color(skin.outer)
    val skinInner = Color(skin.inner)
    val skinCore = Color(skin.core)
    val (outer, inner, core) = when (state) {
        FlameState.FROZEN -> Triple(Ice, Color(0xFFBAE6FD), Color.White)
        FlameState.SLEEPING -> Triple(Color(0xFF6B4A3A), Color(0xFF8C5A44), Color(0xFFB07A5E))
        FlameState.EMBER -> Triple(skinOuter.copy(alpha = 0.75f), skinInner.copy(alpha = 0.8f), skinCore)
        else -> Triple(skinOuter, skinInner, skinCore)
    }
    val moodScale = when (state) {
        FlameState.SLEEPING -> 0.62f
        FlameState.EMBER -> 0.78f
        FlameState.WARM -> 0.92f
        FlameState.BLAZING -> 1.05f
        FlameState.FROZEN -> 0.9f
    }
    val formScale = when (form) {
        FlameForm.SPARK -> 0.8f
        FlameForm.EMBER -> 1f
        FlameForm.FLAME -> 1.04f
        FlameForm.BLAZE -> 1.08f
        FlameForm.INFERNO -> 1.1f
        FlameForm.SUPERNOVA -> 1.12f
    }
    val scale = moodScale * formScale
    val wobble = if (state == FlameState.SLEEPING) 0.02f else if (state == FlameState.BLAZING) 0.09f else 0.05f
    val lively = state != FlameState.SLEEPING && state != FlameState.FROZEN

    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val baseY = h * 0.92f
        val sway = sin(phase) * wobble
        val flick = 1f + sin(phase * 2f + 1f) * wobble * 0.6f
        val bodyW = w * 0.38f * scale
        val bodyH = h * 0.82f * scale

        // Halo for the top forms.
        if (form >= FlameForm.INFERNO && lively) {
            val r = bodyH * (if (form == FlameForm.SUPERNOVA) 0.62f else 0.5f) * (1f + sin(phase) * 0.03f)
            drawCircle(
                Brush.radialGradient(listOf(inner.copy(alpha = if (form == FlameForm.SUPERNOVA) 0.35f else 0.22f), Color.Transparent), center = Offset(cx, baseY - bodyH * 0.42f), radius = r),
                radius = r, center = Offset(cx, baseY - bodyH * 0.42f),
            )
        }
        // Side tongues from FLAME upward.
        if (form >= FlameForm.FLAME) {
            val side = when (form) { FlameForm.FLAME -> 0.42f; FlameForm.BLAZE -> 0.5f; else -> 0.56f }
            flame(cx - bodyW * 0.55f, baseY, bodyW * side, bodyH * side, outer.copy(alpha = 0.85f), sway * 1.3f, flick)
            flame(cx + bodyW * 0.55f, baseY, bodyW * side, bodyH * side, outer.copy(alpha = 0.85f), -sway * 1.3f, flick)
            if (form >= FlameForm.BLAZE) {
                flame(cx - bodyW * 0.55f, baseY, bodyW * side * 0.55f, bodyH * side * 0.55f, inner, sway * 1.6f, flick)
                flame(cx + bodyW * 0.55f, baseY, bodyW * side * 0.55f, bodyH * side * 0.55f, inner, -sway * 1.6f, flick)
            }
        }
        // Main body: spark is a lone tongue, everything else has three layers.
        flame(cx, baseY, bodyW, bodyH, outer, sway, flick)
        if (form != FlameForm.SPARK) flame(cx, baseY, bodyW * 0.62f, bodyH * 0.62f, inner, sway * 1.4f, flick)
        val coreW = if (form == FlameForm.SUPERNOVA) 0.42f else 0.32f
        flame(cx, baseY, bodyW * coreW, bodyH * (coreW + 0.02f), core, sway * 1.8f, flick)

        // Sparks: streak-driven for early forms, always-on and more numerous for later ones.
        val sparks = when {
            state == FlameState.BLAZING && form < FlameForm.BLAZE -> 4
            form == FlameForm.BLAZE -> 5
            form == FlameForm.INFERNO -> 7
            form == FlameForm.SUPERNOVA -> 9
            else -> 0
        }
        if (lively) repeat(sparks) { i ->
            val t = ((phase / (2 * PI).toFloat()) + i.toFloat() / sparks) % 1f
            val sx = cx + sin(t * 9f + i) * bodyW * 0.85f
            val sy = baseY - bodyH * 0.55f - t * bodyH * 0.95f
            drawCircle(inner.copy(alpha = (1f - t) * 0.9f), radius = (3.5f - t * 2.5f).dp.toPx(), center = Offset(sx, sy))
        }
        // Supernova: orbiting motes.
        if (form == FlameForm.SUPERNOVA && lively) repeat(6) { i ->
            val a = phase * 0.7f + i * (2 * PI / 6).toFloat()
            val rx = bodyW * 1.05f; val ry = bodyH * 0.22f
            val p = Offset(cx + cos(a) * rx, baseY - bodyH * 0.35f + sin(a) * ry)
            drawCircle(core.copy(alpha = 0.6f + 0.4f * sin(a)), radius = 2.5.dp.toPx(), center = p)
        }
        if (state == FlameState.SLEEPING) repeat(2) { i ->
            val t = ((phase / (2 * PI).toFloat()) + i * 0.5f) % 1f
            drawCircle(Color(0xFF9AA0B5).copy(alpha = (1f - t) * 0.6f), radius = (2f + i).dp.toPx(), center = Offset(cx + bodyW * 0.9f + t * 10f, baseY - bodyH - t * 30f))
        }
    }
}

private fun DrawScope.flame(cx: Float, baseY: Float, width: Float, height: Float, color: Color, lean: Float, flick: Float) {
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
