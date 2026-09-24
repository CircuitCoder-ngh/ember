package com.nhowe.ember.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nhowe.ember.ui.theme.EmberOrange
import com.nhowe.ember.ui.theme.Gold
import com.nhowe.ember.ui.theme.ember

/** The hero ring. Progress animates with a soft spring; a faint halo sits under the arc. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 18.dp,
    colors: List<Color> = listOf(EmberOrange, Gold, EmberOrange),
    trackColor: Color = MaterialTheme.ember.ringTrack,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = if (animate) spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow) else spring(stiffness = Spring.StiffnessHigh),
        label = "ring",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            if (animated > 0.001f) {
                val brush = Brush.sweepGradient(colors)
                rotate(-90f) {
                    // halo
                    drawArc(
                        brush = brush,
                        startAngle = 0f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke * 1.9f, cap = StrokeCap.Round),
                        alpha = 0.16f,
                    )
                    drawArc(
                        brush = brush,
                        startAngle = 0f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        content()
    }
}
