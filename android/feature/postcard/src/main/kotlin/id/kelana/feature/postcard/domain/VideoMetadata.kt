package id.kelana.feature.postcard.domain

/** Reads facts about a picked video on the device. */
interface VideoMetadataReader {
    /** Length in whole seconds (rounded up, at least 1), or null when [source] (a content Uri as text) cannot be read. */
    suspend fun durationSeconds(source: String): Int?
}

/** "0:42" for the duration badge. */
fun formatVideoDuration(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** Milliseconds from MediaMetadataRetriever to whole seconds, rounded up; null for a missing or non-positive value. */
fun secondsFromMillis(millis: Long?): Int? =
    if (millis == null || millis <= 0L) null else ((millis + 999L) / 1000L).toInt()
