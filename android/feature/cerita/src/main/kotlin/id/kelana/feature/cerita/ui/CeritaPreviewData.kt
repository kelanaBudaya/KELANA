package id.kelana.feature.cerita.ui

import id.kelana.core.model.PostcardStory
import id.kelana.core.model.VideoStory
import java.time.Instant
import java.time.LocalDate

/**
 * SAMPLE CONTENT FOR @Preview AND UNIT TESTS ONLY. These are not real visitors, places' reviews or photos.
 * Nothing in the production code path references this object: the app binds an empty story repository.
 */
internal object CeritaPreviewData {
    val postcard = PostcardStory(
        id = "sample-postcard",
        placeId = "sample-place",
        placeName = "Contoh tempat",
        authorName = "Contoh pengunjung",
        createdAt = Instant.parse("2026-11-20T10:00:00Z"),
        stampCode = "YK-00",
        message = "Contoh pesan untuk pratinjau.",
        rating = 4,
        visitedOn = LocalDate.of(2026, 11, 20),
    )

    val video = VideoStory(
        id = "sample-video",
        placeId = "sample-place",
        placeName = "Contoh tempat",
        authorName = "Contoh pengunjung",
        createdAt = Instant.parse("2026-11-20T11:00:00Z"),
        stampCode = "YK-00",
        caption = "Contoh keterangan video untuk pratinjau.",
    )

    val stories = listOf(
        video,
        postcard,
        postcard.copy(id = "sample-postcard-2", rating = null, message = null, createdAt = Instant.parse("2026-11-19T10:00:00Z")),
        video.copy(id = "sample-video-2", caption = null, authorName = "", createdAt = Instant.parse("2026-11-18T10:00:00Z")),
    )
}
