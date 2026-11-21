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
        primary = colors.navy,
        onPrimary = colors.white,
        secondary = colors.primaryBlue,
        onSecondary = colors.white,
        tertiary = colors.orange,
        background = colors.cream,
        onBackground = colors.ink,
        surface = colors.white,
        onSurface = colors.ink,
        onSurfaceVariant = colors.muted,
        outline = colors.outline,
        error = colors.danger,
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
