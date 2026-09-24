package com.nhowe.ember.ui.calendar

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhowe.ember.domain.model.DayState
import com.nhowe.ember.ui.theme.ember

/** One day on the month grid: a ring whose fill is the day's score, colored by its state. */
@Composable
fun DayCell(
    day: Int,
    state: DayState?,
    score: Double?,
    isToday: Boolean,
    plannedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    starred: Boolean = false,
) {
    val colors = MaterialTheme.ember
    val scheme = MaterialTheme.colorScheme
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )
    val ringColor = when (state) {
        DayState.PERFECT -> colors.perfect
        DayState.HIT -> colors.hit
        DayState.MISS -> colors.miss
        DayState.FROZEN -> colors.frozen
        DayState.PENDING -> scheme.primary
        DayState.REST, DayState.FUTURE, null -> scheme.outlineVariant
    }
    val textColor = when (state) {
        DayState.PERFECT -> colors.perfect
        DayState.FUTURE, null -> scheme.onSurfaceVariant.copy(alpha = 0.6f)
        DayState.REST -> scheme.onSurfaceVariant
        else -> scheme.onSurface
    }

    Box(
        modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2 + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            when (state) {
                DayState.PERFECT -> {
                    drawCircle(colors.perfect.copy(alpha = 0.16f))
                    drawArc(colors.perfect, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                DayState.HIT -> {
                    drawArc(colors.hit.copy(alpha = 0.22f), -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    drawArc(colors.hit, -90f, (360f * (score ?: 0.0)).toFloat(), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                DayState.MISS -> {
                    drawCircle(colors.miss.copy(alpha = 0.10f))
                    drawArc(colors.miss.copy(alpha = 0.25f), -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    val s = (score ?: 0.0).toFloat()
                    if (s > 0f) drawArc(colors.miss, -90f, 360f * s, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                DayState.FROZEN -> {
                    drawCircle(colors.frozen.copy(alpha = 0.18f))
                    drawArc(colors.frozen, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                }
                DayState.PENDING -> {
                    drawArc(scheme.primary.copy(alpha = 0.25f + 0.35f * pulse), -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    val s = (score ?: 0.0).toFloat()
                    if (s > 0f) drawArc(scheme.primary, -90f, 360f * s, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                DayState.REST -> drawArc(ringColor.copy(alpha = 0.5f), -90f, 360f, false, topLeft, arcSize, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))))
                DayState.FUTURE, null -> {
                    if (starred) {
                        drawCircle(colors.perfect.copy(alpha = 0.10f))
                        drawArc(colors.perfect, -90f, 360f, false, topLeft, arcSize, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                    } else if (plannedCount > 0) {
                        drawArc(ringColor, -90f, 360f, false, topLeft, arcSize, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f))))
                    }
                }
            }
            if (isToday) {
                drawCircle(scheme.primary, radius = 2.5.dp.toPx(), center = Offset(size.width / 2, size.height - 4.dp.toPx()))
            }
        }
        Text(
            "$day",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = if (isToday || state == DayState.PERFECT) FontWeight.Bold else FontWeight.Medium,
        )
        if (starred) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = "Big goal this day",
                tint = colors.perfect,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 1.dp).size(13.dp),
            )
        }
    }
}

@Suppress("unused")
private val unusedColor = Color.Unspecified
