package id.kelana.feature.tempat.ui

import id.kelana.core.model.PlacePhoto

internal fun testPhoto(i: Int, placeId: String = "p1") = PlacePhoto(
    id = "ph$i",
    placeId = placeId,
    url = "https://example.invalid/$i.jpg",
    thumbUrl = "https://example.invalid/${i}_t.jpg",
    width = 1600,
    height = 1000,
    caption = null,
    attributionText = "Foto: Contoh · CC BY-SA 4.0 · Wikimedia Commons",
    licenseCode = "CC-BY-SA-4.0",
    licenseUrl = "https://creativecommons.org/licenses/by-sa/4.0/",
    sourcePageUrl = "https://commons.wikimedia.org/wiki/File:Contoh$i.jpg",
)
