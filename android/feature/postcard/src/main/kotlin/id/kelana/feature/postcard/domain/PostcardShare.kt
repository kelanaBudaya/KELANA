package id.kelana.feature.postcard.domain

import androidx.compose.runtime.Immutable
import id.kelana.core.model.PostcardStyle
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Output size of the shared picture. */
enum class ShareFormat(val widthPx: Int, val heightPx: Int) {
    /** 9:16, Instagram Story. */
    Story(1080, 1920),

    /** 1:1, feed. */
    Feed(1080, 1080),
}

/**
 * Everything the card shows. [photoJpeg] is the already re-encoded photo (no EXIF). [stampedAt] is the stamp
 * date from the stamp list, or null when the stamp is not in the list (the card then hides the dates).
 * [regionName], [regionCode] and [regionShortName] describe the region of the stamped place ("Jawa Tengah",
 * "JTG", "Jateng"), never a fixed region.
 */
@Immutable
class CardContent(
    val placeName: String,
    val capCode: String,
    val message: String,
    val rating: Int?,
    val style: PostcardStyle,
    val stampedAt: Instant?,
    val photoJpeg: ByteArray?,
    val regionName: String,
    val regionCode: String,
    val regionShortName: String,
) {
    /** Header label, in capitals as in the design: "JAWA TENGAH". */
    val regionLabel: String get() = regionName.uppercase(CardLocale)
}

private val CardLocale: Locale = Locale.forLanguageTag("id-ID")

/**
 * Font size that makes text fit: [base] when the text measured at [base] is [measured] wide and fits in
 * [available], otherwise scaled down in proportion, never below [min].
 */
fun fitFontSize(base: Float, measured: Float, available: Float, min: Float): Float =
    if (measured <= available || measured <= 0f) base else maxOf(min, base * available / measured)

/**
 * Hands the card from Tulis to Bagikan. In memory only: the read side of postcards (Cerita) is not built yet,
 * so there is nothing to reload from. After process death Bagikan shows "postcard tidak ditemukan".
 */
@Singleton
class PostcardShareSession @Inject constructor() {
    @Volatile
    var card: CardContent? = null
        private set

    fun put(card: CardContent) {
        this.card = card
    }

    fun clear() {
        card = null
    }
}

/** Decodes, downsizes and re-encodes a picked photo so no EXIF (GPS included) survives. */
interface PhotoProcessor {
    /** [source] is a content Uri as text. Null when the image cannot be read or cannot be made small enough. */
    suspend fun process(source: String): id.kelana.core.model.PostcardPhoto?
}

/** Renders the card to a file and saves or shares it. */
interface CardExporter {
    /** True on API 29+ (MediaStore, no storage permission). On API 26 to 28 the Simpan button is hidden. */
    val canSaveToGallery: Boolean

    /** Renders [card] to `cacheDir/postcards/` and returns its content Uri as text, or null on failure. Cleans old files. */
    suspend fun renderToCache(card: CardContent, format: ShareFormat): String?

    /** Renders and writes to `Pictures/KELANA`. False on failure. */
    suspend fun saveToGallery(card: CardContent, format: ShareFormat): Boolean
}
