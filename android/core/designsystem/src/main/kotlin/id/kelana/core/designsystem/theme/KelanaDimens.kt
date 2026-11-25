package id.kelana.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii. Source: docs/design/artifact/README.md; the list card radius 20 comes from KatMuseum.dc.html. */
object KelanaRadii {
    /** Badge (KatMuseum, DetailIlustratif: `border-radius: 4px`). */
    val badge: Dp = 4.dp

    /** Input and small tile (README: input 12; Jelajah 40 dp tiles use 12). */
    val input: Dp = 12.dp

    /** Card and thumbnail (README: card 16; KatMuseum cover tile 16). */
    val card: Dp = 16.dp

    /** Row card of a list (KatMuseum place rows `border-radius: 20px`). */
    val listCard: Dp = 20.dp

    /** Day tile of the agenda strip (54 x 74, `border-radius: 18px`). */
    val dayTile: Dp = 18.dp

    /** Hero card and bottom sheet (`border-radius: 24px`: Agenda hero, Detail pentas card, Hapus akun sheet). */
    val sheet: Dp = 24.dp

    /** Large image panel (Kenalan1, Masuk: `border-radius: 32px`). */
    val imagePanel: Dp = 32.dp

    /** Chip, primary button and pills (`border-radius: 999px`). */
    val pill: Dp = 999.dp
}

object KelanaSpacing {
    /** Side gutter of card sections and lists (16 in Jelajah, Detail, KatMuseum sections). */
    val gutter: Dp = 16.dp

    /** Side gutter of chip rows, buttons and top bars (20 in Kenalan1, Masuk, KatMuseum chip row). */
    val gutterWide: Dp = 20.dp

    /** Side margin of titles and body text on onboarding screens (24 in Kenalan1). */
    val gutterText: Dp = 24.dp

    // PROVISIONAL: step scale, the files use many one-off paddings
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 24.dp
}

object KelanaSizes {
    /** Card width in the 390 dp frames. */
    val cardWidth: Dp = 358.dp

    /** Bottom bar (`height: 84px`, padding 6/8/18, KatMuseum). */
    val bottomBarHeight: Dp = 84.dp

    /** Primary button (`height: 52px`, Kenalan1, Masuk). */
    val buttonHeight: Dp = 52.dp

    /** Filter chip (`height: 40px`, KatMuseum). */
    val chipHeight: Dp = 40.dp

    /** Search field and round icon buttons (`height: 44px`, Jelajah). */
    val inputHeight: Dp = 44.dp

    /** Round back button over an image (44 x 44, KatMuseum). */
    val backButton: Dp = 44.dp

    /** Pill behind the active tab icon (58 x 32, KatMuseum). */
    val tabPillWidth: Dp = 58.dp
    val tabPillHeight: Dp = 32.dp

    /** Thumbnail tile of a list row (60 x 60, KatMuseum). */
    val listThumb: Dp = 60.dp

    // PROVISIONAL: not measurable in the files (back arrow row sits on the image)
    val topBarHeight: Dp = 56.dp
}

object KelanaShapes {
    val badge = RoundedCornerShape(KelanaRadii.badge)
    val input = RoundedCornerShape(KelanaRadii.input)
    val card = RoundedCornerShape(KelanaRadii.card)
    val listCard = RoundedCornerShape(KelanaRadii.listCard)
    val imagePanel = RoundedCornerShape(KelanaRadii.imagePanel)

    /** Primary and all buttons are pills. */
    val button = RoundedCornerShape(50)
    val chip = RoundedCornerShape(50)
}

val KelanaMaterialShapes = Shapes(
    extraSmall = KelanaShapes.badge,
    small = KelanaShapes.input,
    medium = KelanaShapes.card,
    large = KelanaShapes.listCard,
    extraLarge = KelanaShapes.imagePanel,
)
