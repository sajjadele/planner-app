package com.example.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BrandedLoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AscendingStepsVLogo(
            modifier = Modifier.size(132.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = "ویژن پلنر",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(30.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        )
    }
}

@Composable
private fun AscendingStepsVLogo(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = w * 0.085f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        // Left arm of the V.
        drawLine(
            color,
            Offset(w * 0.16f, h * 0.18f),
            Offset(w * 0.5f, h * 0.82f),
            strokeWidth = stroke.width,
            cap = stroke.cap
        )
        // Right arm as ascending staircase leading toward the guide star.
        val steps = listOf(
            Offset(w * 0.5f, h * 0.82f),
            Offset(w * 0.59f, h * 0.82f),
            Offset(w * 0.59f, h * 0.70f),
            Offset(w * 0.68f, h * 0.70f),
            Offset(w * 0.68f, h * 0.58f),
            Offset(w * 0.77f, h * 0.58f),
            Offset(w * 0.77f, h * 0.46f),
            Offset(w * 0.86f, h * 0.46f),
            Offset(w * 0.86f, h * 0.34f)
        )
        val path = Path().apply {
            moveTo(steps.first().x, steps.first().y)
            steps.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color, style = stroke)
        // Guide star crowning the steps.
        drawStar(center = Offset(w * 0.86f, h * 0.20f), radius = w * 0.13f, color = color)
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color, points: Int = 5) {
    val inner = radius * 0.45f
    val path = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else inner
        val angle = Math.PI * i / points - Math.PI / 2
        val x = center.x + r * cos(angle).toFloat()
        val y = center.y + r * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
