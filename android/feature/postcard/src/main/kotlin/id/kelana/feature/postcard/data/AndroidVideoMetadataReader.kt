package id.kelana.feature.postcard.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kelana.feature.postcard.domain.VideoMetadataReader
import id.kelana.feature.postcard.domain.secondsFromMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Reads the duration with [MediaMetadataRetriever] from the picked content Uri (no storage permission needed). */
class AndroidVideoMetadataReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoMetadataReader {
    override suspend fun durationSeconds(source: String): Int? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(source))
            secondsFromMillis(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // nothing left to free
            }
        }
    }
}
