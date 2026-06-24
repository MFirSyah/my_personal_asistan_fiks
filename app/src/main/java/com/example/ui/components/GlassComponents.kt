package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Bento Grid design theme colors
val GlassBackground = Color(0xFF020617) // Slate 950 base
val GlassWhiteAlpha = Color(0xFFFFFFFF).copy(alpha = 0.05f)
val GlassWhiteBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f)

val BentoIndigo = Color(0xFF4F46E5) // Indigo 600
val BentoIndigoLight = Color(0xFF93C5FD) // Indigo 300 / Light blue
val BentoEmerald = Color(0xFF10B981) // Emerald 500
val BentoEmeraldLight = Color(0xFF34D399) // Emerald 400
val BentoRose = Color(0xFFE11D48) // Rose 600
val BentoRoseLight = Color(0xFFFB7185) // Rose 400
val BentoCyan = Color(0xFF67E8F9) // Cyan 300

// Compatibility mapping for existing variables
val VividViolet = BentoIndigo
val VividIndigo = Color(0xFF3730A3)
val VividRose = BentoRose

@Composable
fun GlassmorphicBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassBackground) // Slate 950 base
    ) {
        // 1. Top-Left Indigo light spot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BentoIndigo.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        // 2. Middle-Right Emerald light spot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BentoEmerald.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 1400f
                    )
                )
        )

        // 3. Bottom-Left/Center Rose light spot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BentoRose.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 1600f
                    )
                )
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(GlassWhiteAlpha)
            .border(1.dp, GlassWhiteBorder, RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}

fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        while (i < len) {
            // Check for bullet points at line starts or after newlines
            if (text[i] == '*' && i + 1 < len && text[i + 1] == ' ' && (i == 0 || text[i - 1] == '\n')) {
                append("• ")
                i += 2
            } else if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                // Try to find matching "**"
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(i + 2, end))
                    pop()
                    i = end + 2
                } else {
                    append("**")
                    i += 2
                }
            } else if (text[i] == '*') {
                // Try to find matching "*"
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append("*")
                    i += 1
                }
            } else {
                append(text[i])
                i += 1
            }
        }
    }
}
