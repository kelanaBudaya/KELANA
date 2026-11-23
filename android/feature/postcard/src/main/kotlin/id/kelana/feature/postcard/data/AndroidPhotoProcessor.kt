package id.kelana.feature.postcard.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import id.kelana.core.model.PostcardPhoto
import id.kelana.feature.postcard.domain.JpegMetadata
import id.kelana.feature.postcard.domain.PhotoMath
import id.kelana.feature.postcard.domain.PhotoProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Reads a picked or captured photo, makes it upright, downsizes it to at most 1600 px on the long side and
 * re-encodes it as JPEG (quality about 85, under 5 MB). Re-encoding with [Bitmap.compress] writes no EXIF, so
 * the GPS tag of the original never leaves the device; [JpegMetadata.strip] then removes any remaining
 * metadata segment, and a result that is not a clean JPEG is refused.
 */
class AndroidPhotoProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoProcessor {

    override suspend fun process(source: String): PostcardPhoto? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(source)
        try {
            encode(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        } finally {
            deleteCameraTemp(uri)
        }
    }

    private fun encode(uri: Uri): PostcardPhoto? {
        val resolver = context.contentResolver
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = PhotoMath.sampleSize(bounds.outWidth, bounds.outHeight)
        }
        var bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val (w, h) = PhotoMath.fitWithin(bitmap.width, bitmap.height)
        if (w != bitmap.width || h != bitmap.height) bitmap = Bitmap.createScaledBitmap(bitmap, w, h, true)

        val turn = PhotoMath.orientationOf(orientation)
        if (turn.rotationDegrees != 0 || turn.flipHorizontal) {
            val matrix = Matrix().apply {
                if (turn.rotationDegrees != 0) postRotate(turn.rotationDegrees.toFloat())
                if (turn.flipHorizontal) postScale(-1f, 1f)
            }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        if (bitmap.hasAlpha()) bitmap = onWhite(bitmap) // JPEG has no alpha: avoid a black background

        var quality = START_QUALITY
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= QUALITY_STEP
        } while (bytes.size > PhotoMath.MAX_BYTES && quality >= MIN_QUALITY)
        if (bytes.size > PhotoMath.MAX_BYTES) return null

        val clean = JpegMetadata.strip(bytes) ?: return null
        return PostcardPhoto(clean, "image/jpeg")
    }

    private fun onWhite(source: Bitmap): Bitmap {
        val flat = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        return flat
    }

    /** The camera writes into our own cache folder; the temporary file is not kept. */
    private fun deleteCameraTemp(uri: Uri) {
        if (uri.authority != PostcardFiles.authority(context)) return
        runCatching { File(PostcardFiles.cameraDir(context), PostcardFiles.CAMERA_FILE).delete() }
    }

    private companion object {
        const val START_QUALITY = 85
        const val QUALITY_STEP = 10
        const val MIN_QUALITY = 35
    }
}
