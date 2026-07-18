package app.drinkin.android.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DrinkinAccentBlue = Color(0xFF0E5FA8)
val DrinkinLightGray = Color(0xFFF3F2EF)
val DrinkinTextBlack = Color(0xFF191919)
val DrinkinMutedGray = Color(0xFF666666)
val DrinkinBorderColor = Color(0xFFE0E0E0)
val DrinkinCardBackground = Color(0xFFFFFFFF)

private val LightColorPalette = lightColors(
    primary = DrinkinAccentBlue,
    primaryVariant = Color(0xFF003D82),
    secondary = Color(0xFF0073B1),
    background = DrinkinLightGray,
    surface = DrinkinCardBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DrinkinTextBlack,
    onSurface = DrinkinTextBlack
)

@Composable
fun DrinkinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = LightColorPalette,
        content = content
    )
}
