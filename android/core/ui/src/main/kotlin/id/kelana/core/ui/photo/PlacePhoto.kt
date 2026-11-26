package id.kelana.core.ui.photo

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * Real photo of a place on a card, cropped to fill. [fallback] (the category illustration) is always drawn
 * underneath, so it shows when [thumbUrl] is null, while the image loads, and if it fails. In previews and
 * inspection mode no request is made. No credit text here: the place gallery shows the attribution.
 */
@Composable
fun PlacePhoto(
    thumbUrl: String?,
    fallback: @Composable () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        fallback()
        if (thumbUrl != null && !LocalInspectionMode.current) {
            val request = ImageRequest.Builder(LocalContext.current).data(thumbUrl).crossfade(true).build()
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** Place id to cover thumbnail URL for the current screen tree. Empty by default (previews, tests). */
val LocalPlaceCovers = compositionLocalOf<Map<String, String>> { emptyMap() }

/** Loads the covers once through [PlaceCoversViewModel] and provides them to [content] via [LocalPlaceCovers]. */
@Composable
fun ProvidePlaceCovers(content: @Composable () -> Unit) {
    val viewModel: PlaceCoversViewModel = hiltViewModel()
    val covers by viewModel.covers.collectAsState()
    CompositionLocalProvider(LocalPlaceCovers provides covers, content = content)
}
