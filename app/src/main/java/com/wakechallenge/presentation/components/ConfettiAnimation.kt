package com.wakechallenge.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ConfettiParticle(
    val id: Int,
    val x: Float,
    val initialY: Float,
    val color: Color,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val fallSpeed: Float,
    val swayAmplitude: Float,
    val swaySpeed: Float,
    val shape: ConfettiShape
)

enum class ConfettiShape { RECTANGLE, CIRCLE, TRIANGLE }

@Composable
fun ConfettiAnimation(
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    colors: List<Color> = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFFF6B35), // Orange
        Color(0xFFFF1744), // Red
        Color(0xFF00E676), // Green
        Color(0xFF2979FF), // Blue
        Color(0xFFAA00FF), // Purple
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFE91E63)  // Pink
    ),
    durationMillis: Int = 4000,
    onAnimationEnd: () -> Unit = {}
) {
    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var isAnimating by remember { mutableStateOf(true) }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    LaunchedEffect(Unit) {
        particles = List(particleCount) { index ->
            ConfettiParticle(
                id = index,
                x = Random.nextFloat(),
                initialY = Random.nextFloat() * -0.5f - 0.1f,
                color = colors.random(),
                size = Random.nextFloat() * 12f + 6f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 720f - 360f,
                fallSpeed = Random.nextFloat() * 0.3f + 0.2f,
                swayAmplitude = Random.nextFloat() * 0.1f + 0.02f,
                swaySpeed = Random.nextFloat() * 5f + 2f,
                shape = ConfettiShape.entries.random()
            )
        }

        delay(durationMillis.toLong())
        isAnimating = false
        onAnimationEnd()
    }

    if (isAnimating) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            particles.forEach { particle ->
                val progress = (time * particle.fallSpeed) % 2f
                val y = particle.initialY + progress

                if (y in -0.2f..1.2f) {
                    val sway = kotlin.math.sin(time * particle.swaySpeed * 0.01f) * particle.swayAmplitude
                    val x = (particle.x + sway).coerceIn(0f, 1f)

                    val currentRotation = particle.rotation + time * particle.rotationSpeed * 0.01f

                    val centerX = x * width
                    val centerY = y * height

                    rotate(
                        degrees = currentRotation,
                        pivot = Offset(centerX, centerY)
                    ) {
                        when (particle.shape) {
                            ConfettiShape.RECTANGLE -> {
                                drawRect(
                                    color = particle.color,
                                    topLeft = Offset(
                                        centerX - particle.size / 2,
                                        centerY - particle.size / 4
                                    ),
                                    size = Size(particle.size, particle.size / 2)
                                )
                            }
                            ConfettiShape.CIRCLE -> {
                                drawCircle(
                                    color = particle.color,
                                    radius = particle.size / 2,
                                    center = Offset(centerX, centerY)
                                )
                            }
                            ConfettiShape.TRIANGLE -> {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(centerX, centerY - particle.size / 2)
                                    lineTo(centerX - particle.size / 2, centerY + particle.size / 2)
                                    lineTo(centerX + particle.size / 2, centerY + particle.size / 2)
                                    close()
                                }
                                drawPath(path, particle.color)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CelebrationOverlay(
    show: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    content()

    if (show) {
        ConfettiAnimation(
            modifier = Modifier.fillMaxSize(),
            onAnimationEnd = onDismiss
        )
    }
}
