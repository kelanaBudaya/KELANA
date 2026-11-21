package id.kelana.feature.cerita.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.kelana.core.designsystem.component.EmptyState
import id.kelana.core.designsystem.component.KelanaBackTopBar
import id.kelana.core.designsystem.theme.KelanaTheme
import id.kelana.feature.cerita.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

/** "20 Nov 2026" (id-ID). */
internal fun formatStoryDate(date: LocalDate): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", IdLocale).format(date)

/** The author's display name, or a generic label when the profile has none. Never an invented name. */
@Composable
internal fun authorLabel(authorName: String): String =
    authorName.ifBlank { stringResource(R.string.cerita_author_fallback) }

/** Soft tint behind a photo placeholder, picked from the story id so it stays stable across recompositions. */
@Composable
internal fun placeholderTint(storyId: String): Color {
    val c = KelanaTheme.colors
    val palette = listOf(c.skyText, c.peach, c.creamYellow, c.successSurface)
    return palette[Math.floorMod(storyId.hashCode(), palette.size)]
}

/** [rating] stars out of 5 (1 to 5). Draws nothing for null or out-of-range values. */
@Composable
internal fun StarRow(rating: Int?, starSize: Int, modifier: Modifier = Modifier) {
    if (rating == null || rating !in 1..5) return
    val description = stringResource(R.string.cerita_rating_desc, rating)
    Row(modifier.semantics { contentDescription = description }) {
        repeat(5) { index ->
            Image(
                painter = painterResource(R.drawable.cerita_ic_star),
                contentDescription = null,
                modifier = Modifier.size(starSize.dp),
                colorFilter = if (index < rating) null else ColorFilter.tint(KelanaTheme.colors.divider),
            )
        }
    }
}

@Composable
internal fun LoadingBox(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cerita_loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = KelanaTheme.colors.primary)
    }
}

@Composable
internal fun GuestState(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.cerita_guest_title),
        body = stringResource(R.string.cerita_guest_body),
        modifier = modifier,
        actionLabel = stringResource(R.string.cerita_guest_action),
        onAction = onSignIn,
    )
}

@Composable
internal fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.cerita_error_title),
        body = stringResource(R.string.cerita_error_body),
        modifier = modifier,
        actionLabel = stringResource(R.string.cerita_retry),
        onAction = onRetry,
    )
}

/**
 * Detail screens share these non-story states. Returns true when [state] was one of them and has been drawn,
 * false when the caller should draw the story itself.
 */
@Composable
internal fun showStoryDetailFallback(
    state: StoryDetailUiState,
    title: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
): Boolean {
    val isFallback = state is StoryDetailUiState.Loading || state is StoryDetailUiState.Guest ||
        state is StoryDetailUiState.NotFound || state is StoryDetailUiState.Error
    if (!isFallback) return false
    Column(Modifier.fillMaxSize().background(KelanaTheme.colors.background)) {
        KelanaBackTopBar(title = title, onBack = onBack)
        when (state) {
            StoryDetailUiState.Loading -> LoadingBox()
            StoryDetailUiState.Guest -> GuestState(onSignIn, Modifier.padding(top = 24.dp))
            StoryDetailUiState.Error -> ErrorState(onRetry, Modifier.padding(top = 24.dp))
            else -> EmptyState(
                title = stringResource(R.string.cerita_not_found_title),
                body = stringResource(R.string.cerita_not_found_body),
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
    return true
}
