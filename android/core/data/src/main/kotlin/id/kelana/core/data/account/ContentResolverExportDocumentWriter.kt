package id.kelana.core.data.account

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/** Writes through the ContentResolver into the document the user created (CreateDocument). No storage permission. */
class ContentResolverExportDocumentWriter @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExportDocumentWriter {

    override suspend fun write(destination: String, content: String) {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(destination)
            val resolver = context.contentResolver
            // "wt" truncates when the user picked an existing file; some providers only accept "w".
            val stream = try {
                resolver.openOutputStream(uri, "wt")
            } catch (_: FileNotFoundException) {
                resolver.openOutputStream(uri, "w")
            } catch (_: IllegalArgumentException) {
                resolver.openOutputStream(uri, "w")
            } ?: throw IOException("no output stream")
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        }
    }
}
