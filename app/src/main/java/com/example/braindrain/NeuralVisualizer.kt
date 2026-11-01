package com.example.braindrain

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────
// Simple Brain State Engine (BrainPal Replication)
// ──────────────────────────────────────────────────────
enum class BrainState(
    val label: String,
    val emoji: String,
    val pillLabel: String,
    val pillEmoji: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val shakeIntensity: Float,
    val pulseSpeed: Int
) {
    OPTIMAL("Healthy Brain", "🧠", "HEALTHY", "🧠", Color(0xFFF472B6), Color(0xFFF472B6), 0f, 2000),       // Pink
    ALERT("Active Brain", "🧠", "ACTIVE", "🧠", Color(0xFFF472B6), Color(0xFFF472B6), 1f, 1800),         // Pink
    CAUTION("Tired Brain", "🧠", "TIRED", "🧠", Color(0xFFFBBF24), Color(0xFFFBBF24), 2f, 1500),         // Yellow
    WARNING("Sick Brain", "🤢", "SICK", "🤢", Color(0xFF4ADE80), Color(0xFF4ADE80), 5f, 1000),           // Green
    CRITICAL("Rotting Brain", "🧟", "ROTTING", "🧟", Color(0xFF9CA3AF), Color(0xFF9CA3AF), 10f, 500),      // Grey
    BLACKOUT("Dead Brain", "💀", "DEAD", "💀", Color(0xFF1F2937), Color(0xFF1F2937), 0f, 2000);            // Dark Grey

    companion object {
        fun fromScrollCount(count: Int): BrainState = when {
            count <= 10  -> OPTIMAL
            count <= 25  -> ALERT
            count <= 50  -> CAUTION
            count <= 100 -> WARNING
            count <= 150 -> CRITICAL
            else         -> BLACKOUT
        }
    }
}

// ──────────────────────────────────────────────────────
// Simple Brain Visualizer
// ──────────────────────────────────────────────────────
@Composable
fun NeuralConsciousnessVisualizer(
    scrollCount: Int,
    dailyLimit: Int,
    modifier: Modifier = Modifier
) {
    val brainState = BrainState.fromScrollCount(scrollCount)

    val animatedColor by animateColorAsState(
        targetValue = brainState.primaryColor,
        animationSpec = tween(1000),
        label = "brainColor"
    )

    // Shake animation for stressed states
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -brainState.shakeIntensity,
        targetValue = brainState.shakeIntensity,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeOffset"
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .offset(x = shakeOffset.dp, y = (shakeOffset * 0.5f).dp)
        ) {
            // Eye blink logic
            var isBlinking by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay((2000..5000).random().toLong())
                    isBlinking = true
                    delay(150)
                    isBlinking = false
                }
            }
            
            // Draw a simple cartoon brain
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val r = size.minDimension / 2 * 0.6f

                // Left hemisphere
                drawCircle(color = animatedColor, radius = r, center = Offset(cx - r * 0.5f, cy))
                // Right hemisphere
                drawCircle(color = animatedColor, radius = r, center = Offset(cx + r * 0.5f, cy))
                // Top lobe
                drawCircle(color = animatedColor, radius = r * 0.9f, center = Offset(cx, cy - r * 0.6f))
                
                // Outlines
                val outlineColor = Color.Black.copy(alpha = 0.3f)
                val strokeWidth = 8f
                drawCircle(color = outlineColor, radius = r, center = Offset(cx - r * 0.5f, cy), style = Stroke(width = strokeWidth))
                drawCircle(color = outlineColor, radius = r, center = Offset(cx + r * 0.5f, cy), style = Stroke(width = strokeWidth))
                drawCircle(color = outlineColor, radius = r * 0.9f, center = Offset(cx, cy - r * 0.6f), style = Stroke(width = strokeWidth))
                
                // Brain folds (simple squiggles)
                drawLine(color = outlineColor, start = Offset(cx - r * 0.7f, cy), end = Offset(cx - r * 0.2f, cy - r * 0.3f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = outlineColor, start = Offset(cx + r * 0.7f, cy), end = Offset(cx + r * 0.2f, cy - r * 0.3f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = outlineColor, start = Offset(cx, cy - r * 1.1f), end = Offset(cx, cy - r * 0.4f), strokeWidth = strokeWidth, cap = StrokeCap.Round)

                // Face / Eyes
                val eyeY = cy + r * 0.2f
                val eyeRadius = r * 0.25f
                val leftEyeX = cx - r * 0.45f
                val rightEyeX = cx + r * 0.45f
                
                if (brainState == BrainState.BLACKOUT) {
                    // X for eyes when dead
                    drawLine(color = outlineColor, start = Offset(leftEyeX - eyeRadius*0.8f, eyeY - eyeRadius*0.8f), end = Offset(leftEyeX + eyeRadius*0.8f, eyeY + eyeRadius*0.8f), strokeWidth = strokeWidth*1.5f, cap = StrokeCap.Round)
                    drawLine(color = outlineColor, start = Offset(leftEyeX + eyeRadius*0.8f, eyeY - eyeRadius*0.8f), end = Offset(leftEyeX - eyeRadius*0.8f, eyeY + eyeRadius*0.8f), strokeWidth = strokeWidth*1.5f, cap = StrokeCap.Round)
                    
                    drawLine(color = outlineColor, start = Offset(rightEyeX - eyeRadius*0.8f, eyeY - eyeRadius*0.8f), end = Offset(rightEyeX + eyeRadius*0.8f, eyeY + eyeRadius*0.8f), strokeWidth = strokeWidth*1.5f, cap = StrokeCap.Round)
                    drawLine(color = outlineColor, start = Offset(rightEyeX + eyeRadius*0.8f, eyeY - eyeRadius*0.8f), end = Offset(rightEyeX - eyeRadius*0.8f, eyeY + eyeRadius*0.8f), strokeWidth = strokeWidth*1.5f, cap = StrokeCap.Round)
                } else if (isBlinking) {
                    // Closed eyes (horizontal lines)
                    drawLine(color = outlineColor, start = Offset(leftEyeX - eyeRadius, eyeY), end = Offset(leftEyeX + eyeRadius, eyeY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    drawLine(color = outlineColor, start = Offset(rightEyeX - eyeRadius, eyeY), end = Offset(rightEyeX + eyeRadius, eyeY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                } else {
                    // Open eyes
                    // Whites
                    drawCircle(color = Color.White, radius = eyeRadius, center = Offset(leftEyeX, eyeY))
                    drawCircle(color = Color.White, radius = eyeRadius, center = Offset(rightEyeX, eyeY))
                    // Outlines
                    drawCircle(color = outlineColor, radius = eyeRadius, center = Offset(leftEyeX, eyeY), style = Stroke(width = strokeWidth))
                    drawCircle(color = outlineColor, radius = eyeRadius, center = Offset(rightEyeX, eyeY), style = Stroke(width = strokeWidth))
                    
                    // Pupils (look slightly inward if tired/sick, normally straight ahead)
                    val pupilRadius = eyeRadius * 0.45f
                    val pupilColor = if (brainState == BrainState.CRITICAL) Color.DarkGray else Color.Black
                    val lookOffsetX = if (brainState == BrainState.WARNING || brainState == BrainState.CRITICAL) eyeRadius*0.3f else 0f
                    val lookOffsetY = if (brainState == BrainState.CAUTION) eyeRadius*0.2f else 0f
                    
                    drawCircle(color = pupilColor, radius = pupilRadius, center = Offset(leftEyeX + lookOffsetX, eyeY + lookOffsetY))
                    drawCircle(color = pupilColor, radius = pupilRadius, center = Offset(rightEyeX - lookOffsetX, eyeY + lookOffsetY))
                    
                    // Cute eye highlight (catchlight)
                    if (brainState != BrainState.CRITICAL) {
                        drawCircle(color = Color.White, radius = eyeRadius * 0.15f, center = Offset(leftEyeX - eyeRadius * 0.15f, eyeY - eyeRadius * 0.15f))
                        drawCircle(color = Color.White, radius = eyeRadius * 0.15f, center = Offset(rightEyeX - eyeRadius * 0.15f, eyeY - eyeRadius * 0.15f))
                    }
                }
                
                // Mouth
                val mouthY = cy + r * 0.7f
                when (brainState) {
                    BrainState.OPTIMAL -> { // Happy smile
                        val mouthPath = Path().apply {
                            moveTo(cx - r*0.3f, mouthY - r*0.1f)
                            quadraticBezierTo(cx, mouthY + r*0.2f, cx + r*0.3f, mouthY - r*0.1f)
                        }
                        drawPath(path = mouthPath, color = outlineColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    }
                    BrainState.ALERT -> { // Small smile
                        val mouthPath = Path().apply {
                            moveTo(cx - r*0.2f, mouthY)
                            quadraticBezierTo(cx, mouthY + r*0.1f, cx + r*0.2f, mouthY)
                        }
                        drawPath(path = mouthPath, color = outlineColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    }
                    BrainState.CAUTION -> { // Neutral straight line
                        drawLine(color = outlineColor, start = Offset(cx - r*0.25f, mouthY), end = Offset(cx + r*0.25f, mouthY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                    }
                    BrainState.WARNING, BrainState.CRITICAL -> { // Sad / Wavy mouth
                        val mouthPath = Path().apply {
                            moveTo(cx - r*0.3f, mouthY + r*0.1f)
                            quadraticBezierTo(cx, mouthY - r*0.15f, cx + r*0.3f, mouthY + r*0.1f)
                        }
                        drawPath(path = mouthPath, color = outlineColor, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    }
                    BrainState.BLACKOUT -> { // Open dead mouth (oval)
                        drawOval(color = outlineColor, topLeft = Offset(cx - r*0.2f, mouthY - r*0.1f), size = Size(r*0.4f, r*0.3f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "$scrollCount",
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "REELS WATCHED",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(animatedColor.copy(alpha = 0.2f))
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = brainState.label.uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = animatedColor,
                letterSpacing = 1.sp
            )
        }
    }
}

// ──────────────────────────────────────────────────────
// Mini Simple Brain Indicator (for overlay pill)
// ──────────────────────────────────────────────────────
@Composable
fun MiniNeuralIndicator(
    scrollCount: Int,
    modifier: Modifier = Modifier
) {
    val brainState = BrainState.fromScrollCount(scrollCount)

    val animatedColor by animateColorAsState(
        targetValue = brainState.primaryColor,
        animationSpec = tween(600),
        label = "miniColor"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(animatedColor)
    ) {
        Text(
            text = brainState.pillEmoji,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 14.sp
        )
    }
}
