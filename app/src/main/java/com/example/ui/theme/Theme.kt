package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AtelierBrandLightScheme = lightColorScheme(
    primary = SwissRed,
    onPrimary = PureSurface,
    primaryContainer = SwissRedContainer,
    onPrimaryContainer = OnSwissRedContainer,
    secondary = AtelierCharcoal,
    onSecondary = PureSurface,
    secondaryContainer = Slate200,
    onSecondaryContainer = AtelierCharcoal,
    tertiary = PccAmber,
    onTertiary = PureSurface,
    tertiaryContainer = PccAmberBg,
    onTertiaryContainer = AtelierCharcoal,
    background = BenchBackground,
    onBackground = AtelierCharcoal,
    surface = PureSurface,
    onSurface = AtelierCharcoal,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate300,
    outlineVariant = Slate200,
    error = PccDanger,
    onError = PureSurface,
    errorContainer = PccDangerBg,
    onErrorContainer = OnSwissRedContainer
)

val SwissShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun AtelierGastronomiaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AtelierBrandLightScheme,
        typography = Typography,
        shapes = SwissShapes,
        content = content
    )
}
