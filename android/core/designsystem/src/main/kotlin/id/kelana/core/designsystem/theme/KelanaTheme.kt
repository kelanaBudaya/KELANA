package id.kelana.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/** Accessor for Kelana tokens inside a [KelanaTheme]. */
object KelanaTheme {
    val colors: KelanaColors
        @Composable get() = LocalKelanaColors.current
}

@Composable
fun KelanaTheme(content: @Composable () -> Unit) {
    val colors = KelanaColors()
    val scheme = lightColorScheme(
        primary = colors.primary,
        onPrimary = colors.white,
        primaryContainer = colors.skyText,
        onPrimaryContainer = colors.navy,
        secondary = colors.navy,
        onSecondary = colors.white,
        tertiary = colors.accentOrange,
        onTertiary = colors.white,
        background = colors.background,
        onBackground = colors.ink,
        surface = colors.surface,
        onSurface = colors.ink,
        surfaceVariant = colors.backgroundWarm,
        onSurfaceVariant = colors.muted,
        outline = colors.outline,
        error = colors.danger,
        onError = colors.white,
    )
    CompositionLocalProvider(LocalKelanaColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = KelanaTypography,
            shapes = KelanaMaterialShapes,
            content = content,
        )
    }
}
