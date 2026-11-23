package id.kelana.core.navigation

import kotlinx.serialization.Serializable

/**
 * Bagikan postcard (7.2): share the card just written on Tulis postcard ([PostcardRoute]). The card content is
 * kept in memory by `:feature:postcard`, so this route has no arguments.
 */
@Serializable data object BagikanPostcardRoute
