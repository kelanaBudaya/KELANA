package id.kelana.feature.postcard.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kelana.feature.postcard.domain.CardContent
import id.kelana.feature.postcard.domain.CardExporter
import id.kelana.feature.postcard.domain.ShareFormat
import id.kelana.feature.postcard.ui.card.cardTextMeasurer
import id.kelana.feature.postcard.ui.card.cardTexts
import id.kelana.feature.postcard.ui.card.decodeCardPhoto
import id.kelana.feature.postcard.ui.card.drawPostcardCardFitWidth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Renders the card with the same drawing as the on-screen preview into a 1080 x 1920 (Story) or 1080 x 1080
 * (Feed) bitmap, writes it as PNG. Sharing goes through the FileProvider (`cacheDir/postcards/`); saving uses
 * MediaStore `Pictures/KELANA` on API 29+ and needs no storage permission. On API 26 to 28 saving is not offered.
 */
class AndroidCardExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : CardExporter {

    override val canSaveToGallery: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override suspend fun renderToCache(card: CardContent, format: ShareFormat): String? = withContext(Dispatchers.Default) {
        try {
            val bitmap = render(card, format)
            PostcardFiles.cleanOldShares(context)
            val file = File(PostcardFiles.shareDir(context), "kelana-postcard-" + System.currentTimeMillis() + ".png")
            file.outputStream().use { bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
            PostcardFiles.uriFor(context, file).toString()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    override suspend fun saveToGallery(card: CardContent, format: ShareFormat): Boolean = withContext(Dispatchers.Default) {
        if (!canSaveToGallery) return@withContext false
        try {
            val bitmap = render(card, format).asAndroidBitmap()
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "kelana-postcard-" + System.currentTimeMillis() + ".png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KELANA")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
            val written = try {
                resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) } ?: false
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
            if (!written) {
                resolver.delete(uri, null, null)
                return@withContext false
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        } catch (_: OutOfMemoryError) {
            false
        }
    }

    private fun render(card: CardContent, format: ShareFormat): ImageBitmap {
        val image = ImageBitmap(format.widthPx, format.heightPx)
        val measurer = cardTextMeasurer(context)
        val texts = cardTexts(context, card)
        val photo = decodeCardPhoto(card.photoJpeg)
        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = Canvas(image),
            size = Size(format.widthPx.toFloat(), format.heightPx.toFloat()),
        ) {
            drawPostcardCardFitWidth(card, texts, photo, measurer, format)
        }
        return image
    }
}
