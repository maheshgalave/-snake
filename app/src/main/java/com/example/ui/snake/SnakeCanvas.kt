package com.example.ui.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.*
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun SnakeCanvas(
    gameState: SnakeGameState,
    onDirectionChange: (Direction) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var totalDx = 0f
                var totalDy = 0f
                detectDragGestures(
                    onDragStart = {
                        totalDx = 0f
                        totalDy = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDx += dragAmount.x
                        totalDy += dragAmount.y

                        val threshold = 35f
                        if (abs(totalDx) > threshold || abs(totalDy) > threshold) {
                            if (abs(totalDx) > abs(totalDy)) {
                                if (totalDx > 0) onDirectionChange(Direction.RIGHT)
                                else onDirectionChange(Direction.LEFT)
                            } else {
                                if (totalDy > 0) onDirectionChange(Direction.DOWN)
                                else onDirectionChange(Direction.UP)
                            }
                            totalDx = 0f
                            totalDy = 0f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = tapOffset.x - cx
                    val dy = tapOffset.y - cy

                    if (abs(dx) > abs(dy)) {
                        if (dx > 0) onDirectionChange(Direction.RIGHT)
                        else onDirectionChange(Direction.LEFT)
                    } else {
                        if (dy > 0) onDirectionChange(Direction.DOWN)
                        else onDirectionChange(Direction.UP)
                    }
                }
            }
    ) {
        val cellW = size.width / gameState.gridWidth
        val cellH = size.height / gameState.gridHeight

        // Apply screen shake
        val shakeX = if (gameState.screenShake > 0f) (Math.random().toFloat() - 0.5f) * gameState.screenShake else 0f
        val shakeY = if (gameState.screenShake > 0f) (Math.random().toFloat() - 0.5f) * gameState.screenShake else 0f

        // Draw background grid lines
        val gridColor = Color(gameState.theme.gridColor)
        for (col in 0..gameState.gridWidth) {
            val x = col * cellW + shakeX
            drawLine(
                color = gridColor.copy(alpha = 0.35f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
        }
        for (row in 0..gameState.gridHeight) {
            val y = row * cellH + shakeY
            drawLine(
                color = gridColor.copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        // Draw Obstacles (in Maze Mode)
        gameState.obstacles.forEach { obs ->
            val ox = obs.x * cellW + shakeX
            val oy = obs.y * cellH + shakeY
            drawRoundRect(
                color = Color(0xFFFF5252).copy(alpha = 0.85f),
                topLeft = Offset(ox + 2f, oy + 2f),
                size = Size(cellW - 4f, cellH - 4f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0xFFFF8A80),
                topLeft = Offset(ox + 4f, oy + 4f),
                size = Size(cellW - 8f, cellH - 8f),
                style = Stroke(width = 2f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Draw Food
        drawFoodItem(gameState.food, cellW, cellH, shakeX, shakeY)

        // Draw Bonus Food (if active)
        gameState.bonusFood?.let { bonus ->
            drawFoodItem(bonus, cellW, cellH, shakeX, shakeY)
        }

        // Draw Snake
        drawSnakeBody(gameState, cellW, cellH, shakeX, shakeY)

        // Draw Particles
        gameState.particles.forEach { p ->
            val px = p.x * cellW + shakeX
            val py = p.y * cellH + shakeY
            drawCircle(
                color = Color(p.color).copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(px, py)
            )
        }
    }
}

private fun DrawScope.drawFoodItem(
    food: FoodItem,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float
) {
    val fx = food.point.x * cellW + cellW / 2f + shakeX
    val fy = food.point.y * cellH + cellH / 2f + shakeY
    val radius = (minOf(cellW, cellH) * 0.42f)
    val color = Color(food.type.colorHex)

    // Outer glow ring
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = radius * 1.45f,
        center = Offset(fx, fy)
    )

    // Main food core
    drawCircle(
        color = color,
        radius = radius,
        center = Offset(fx, fy)
    )

    // Inner highlight / shine
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = radius * 0.32f,
        center = Offset(fx - radius * 0.28f, fy - radius * 0.28f)
    )
}

private fun DrawScope.drawSnakeBody(
    gameState: SnakeGameState,
    cellW: Float,
    cellH: Float,
    shakeX: Float,
    shakeY: Float
) {
    val snake = gameState.snake
    if (snake.isEmpty()) return

    val skin = gameState.skin
    val headColor = Color(skin.headColor)
    val bodyColor = Color(skin.bodyColor)

    // Draw Body Segments (from tail to head)
    for (i in (snake.size - 1) downTo 1) {
        val segment = snake[i]
        val sx = segment.x * cellW + cellW / 2f + shakeX
        val sy = segment.y * cellH + cellH / 2f + shakeY

        // Interpolate color from tail to head
        val fraction = i.toFloat() / snake.size
        val segColor = lerpColor(headColor, bodyColor, fraction)
        val segRadius = (minOf(cellW, cellH) * 0.44f) * (0.85f + 0.15f * (1f - fraction))

        // Glow
        drawCircle(
            color = Color(skin.glowColor).copy(alpha = 0.2f),
            radius = segRadius * 1.35f,
            center = Offset(sx, sy)
        )

        // Body cell
        drawCircle(
            color = segColor,
            radius = segRadius,
            center = Offset(sx, sy)
        )

        // Inner cyber core
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = segRadius * 0.3f,
            center = Offset(sx, sy)
        )
    }

    // Draw Head
    val head = snake.first()
    val hx = head.x * cellW + cellW / 2f + shakeX
    val hy = head.y * cellH + cellH / 2f + shakeY
    val headRadius = minOf(cellW, cellH) * 0.48f

    // Head outer glow
    drawCircle(
        color = headColor.copy(alpha = 0.45f),
        radius = headRadius * 1.55f,
        center = Offset(hx, hy)
    )

    // Head base
    drawCircle(
        color = headColor,
        radius = headRadius,
        center = Offset(hx, hy)
    )

    // Draw cyber eye slits based on current moving direction
    val dir = gameState.currentDirection
    val eyeOffsetForward = headRadius * 0.4f
    val eyeOffsetSide = headRadius * 0.35f

    val (eye1, eye2) = when (dir) {
        Direction.UP -> Pair(
            Offset(hx - eyeOffsetSide, hy - eyeOffsetForward),
            Offset(hx + eyeOffsetSide, hy - eyeOffsetForward)
        )
        Direction.DOWN -> Pair(
            Offset(hx - eyeOffsetSide, hy + eyeOffsetForward),
            Offset(hx + eyeOffsetSide, hy + eyeOffsetForward)
        )
        Direction.LEFT -> Pair(
            Offset(hx - eyeOffsetForward, hy - eyeOffsetSide),
            Offset(hx - eyeOffsetForward, hy + eyeOffsetSide)
        )
        Direction.RIGHT -> Pair(
            Offset(hx + eyeOffsetForward, hy - eyeOffsetSide),
            Offset(hx + eyeOffsetForward, hy + eyeOffsetSide)
        )
    }

    val eyeRadius = headRadius * 0.22f
    drawCircle(color = Color.Black, radius = eyeRadius, center = eye1)
    drawCircle(color = Color.Black, radius = eyeRadius, center = eye2)
    drawCircle(color = Color.White, radius = eyeRadius * 0.5f, center = eye1)
    drawCircle(color = Color.White, radius = eyeRadius * 0.5f, center = eye2)
}

private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
    val r = c1.red + (c2.red - c1.red) * t
    val g = c1.green + (c2.green - c1.green) * t
    val b = c1.blue + (c2.blue - c1.blue) * t
    return Color(r, g, b, 1f)
}
