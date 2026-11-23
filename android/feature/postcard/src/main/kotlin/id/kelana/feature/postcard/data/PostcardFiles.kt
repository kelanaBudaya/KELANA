package id.kelana.feature.postcard.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Where the feature keeps its temporary files. Both folders are under `cacheDir` and are shared through [PostcardFileProvider]. */
internal object PostcardFiles {
    const val CAMERA_FILE = "capture.jpg"
    private const val SHARE_DIR = "postcards"
    private const val CAMERA_DIR = "postcard-camera"
    private const val MAX_AGE_MS = 60 * 60 * 1000L

    fun authority(context: Context): String = context.packageName + ".postcard.fileprovider"

    fun shareDir(context: Context): File = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }

    fun cameraDir(context: Context): File = File(context.cacheDir, CAMERA_DIR).apply { mkdirs() }

    /** Fresh target file for the camera app. */
    fun newCameraUri(context: Context): Uri {
        val file = File(cameraDir(context), CAMERA_FILE).apply { delete() }
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    /** The camera target, without touching the file. */
    fun cameraUri(context: Context): Uri =
        FileProvider.getUriForFile(context, authority(context), File(cameraDir(context), CAMERA_FILE))

    fun uriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(context, authority(context), file)

    /** Removes rendered pictures older than an hour, so the cache does not grow. */
    fun cleanOldShares(context: Context, now: Long = System.currentTimeMillis()) {
        shareDir(context).listFiles()?.forEach { if (now - it.lastModified() > MAX_AGE_MS) it.delete() }
    }
}

/** Own subclass so the manifest entry cannot collide with another FileProvider in the app. */
class PostcardFileProvider : FileProvider()
