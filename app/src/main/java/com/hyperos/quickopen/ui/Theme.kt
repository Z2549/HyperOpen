package com.hyperos.quickopen.ui

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/**
 * 语义色板（浅色 / 深色各一份），全部 UI 组件只从这里取色。
 * 单一事实来源：HyperOSQuickOpenTheme 根据 themeMode+系统深色状态装配。
 */
data class SemanticPalette(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val divider: Color,
    val fgPrimary: Color,
    val fgSecondary: Color,
    val warn: Color,
    val ok: Color,
)

/** 全局共享的语义色板，任何 @Composable 里 currentSemantic() 直接取。 */
val LocalSemantic = compositionLocalOf { GlassLight }

@Composable
fun currentSemantic(): SemanticPalette = LocalSemantic.current

/** Material 玻璃拟态配色（深色） */
private val GlassDark = SemanticPalette(
    primary = Color(0xFFFF6900),
    background = Color(0xFF101418),
    surface = Color(0xFF1A1F26),
    surfaceAlt = Color(0xFF232A33),
    divider = Color(0xFF2C333D),
    fgPrimary = Color(0xFFEAF0F6),
    fgSecondary = Color(0xFF9AA7B4),
    warn = Color(0xFFE94634),
    ok = Color(0xFF07C160),
)

/** Material 玻璃拟态配色（浅色） */
private val GlassLight = SemanticPalette(
    primary = Color(0xFFE85D00),
    background = Color(0xFFF2F4F7),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF0F2F5),
    divider = Color(0xFFE2E5EA),
    fgPrimary = Color(0xFF17191D),
    fgSecondary = Color(0xFF6E7680),
    warn = Color(0xFFD93A2B),
    ok = Color(0xFF07C160),
)

/** MIUIX 配色（深色） */
private val MiuixSemDark = SemanticPalette(
    primary = Color(0xFFFF6900),
    background = Color(0xFF14171A),
    surface = Color(0xFF1D2126),
    surfaceAlt = Color(0xFF23282E),
    divider = Color(0xFF2B3138),
    fgPrimary = Color(0xFFE8EBEF),
    fgSecondary = Color(0xFF8B939D),
    warn = Color(0xFFE94634),
    ok = Color(0xFF07C160),
)

/** MIUIX 配色（浅色） */
private val MiuixSemLight = SemanticPalette(
    primary = Color(0xFFFF6900),
    background = Color(0xFFF3F4F6),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF8F9FB),
    divider = Color(0xFFE6E8EB),
    fgPrimary = Color(0xFF181C21),
    fgSecondary = Color(0xFF7C838C),
    warn = Color(0xFFE94634),
    ok = Color(0xFF07C160),
)

/** 顶栏/底栏玻璃拟态参数（浮点 0f..1f，blurRadius=0 表示关闭模糊）。 */
data class GlassSpec(val topAlpha: Float, val bottomAlpha: Float, val blurRadius: Int)

val LocalGlassSpec = compositionLocalOf { GlassSpec(0.72f, 0.88f, 1) }

/** 当前会话主题配置快照（供二级页以外的组件读取），由 HyperOSQuickOpenTheme 装配。 */
data class ThemePrefs(
    val themeMode: Int,
    val monet: Boolean,
    val barBlur: Boolean,
    val floatingNavBar: Boolean,
    val predictiveBack: Boolean,
    val edgeBack: Boolean,
    val pageSwitchGesture: Int,
)

val LocalThemePrefs = compositionLocalOf {
    ThemePrefs(ThemeMode.FOLLOW_SYSTEM, false, true, false, true, true, PageGesture.HORIZONTAL)
}

/** 主题模式常量（与 QuickOpenConfig.THEME_* 对齐） */
object ThemeMode {
    const val FOLLOW_SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

/** 页面切换手势常量（与 QuickOpenConfig.PAGE_GESTURE_* 对齐） */
object PageGesture {
    const val OFF = 0
    const val HORIZONTAL = 1
}

/**
 * 主题装配根：
 * 1) themeMode 用户显式覆盖 isSystemInDarkTheme()（修复深色模式不生效——此前硬跟随系统）；
 * 2) 双轨 UI 风格 Material 玻璃 / MIUIX 卡片；
 * 3) Monet 动态取色（Android 12+ 系统动态色板，Material 轨整套映射，MIUIX 轨主色映射）；
 * 4) 同步系统栏外观（底色 + 图标深浅）。
 */
@Composable
fun HyperOSQuickOpenTheme(
    uiStyle: Int,
    themeMode: Int,
    monet: Boolean,
    barBlur: Boolean,
    floatingNavBar: Boolean = false,
    predictiveBack: Boolean = true,
    edgeBack: Boolean = true,
    pageGesture: Int = PageGesture.HORIZONTAL,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> systemDark
    }

    // Monet：Android 12+ 从壁纸取系统动态色板
    val ctx = LocalContext.current
    val m3Dynamic: ColorScheme? = if (monet && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else null

    val semBase = when {
        uiStyle == UiStyle.MIUIX.ordinal && dark -> MiuixSemDark
        uiStyle == UiStyle.MIUIX.ordinal -> MiuixSemLight
        dark -> GlassDark
        else -> GlassLight
    }
    val sem = if (m3Dynamic != null) semBase.copy(primary = m3Dynamic.primary) else semBase

    val m3Scheme = m3Dynamic ?: if (dark) GlassDarkScheme else GlassLightScheme

    ApplySystemBars(dark, sem)

    CompositionLocalProvider(
        LocalSemantic provides sem,
        LocalGlassSpec provides glassSpec(barBlur),
        LocalThemePrefs provides ThemePrefs(
            themeMode, monet, barBlur, floatingNavBar, predictiveBack, edgeBack, pageGesture,
        ),
    ) {
        if (uiStyle == UiStyle.MIUIX.ordinal) {
            val monetPrimary = m3Dynamic?.primary
            val colors = if (dark) {
                miuixDarkColors(monetPrimary ?: Color(0xFFFF6900))
            } else {
                miuixLightColors(monetPrimary ?: Color(0xFFFF6900))
            }
            MiuixTheme(colors = colors) { content() }
        } else {
            MaterialTheme(colorScheme = m3Scheme) { content() }
        }
    }
}

private fun glassSpec(barBlur: Boolean): GlassSpec =
    if (barBlur) GlassSpec(0.72f, 0.88f, 1) else GlassSpec(0.92f, 0.98f, 0)

/** Material3 玻璃拟态配色（深色） */
private val GlassDarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF6900),
    onPrimary = Color(0xFF2B1500),
    primaryContainer = Color(0xFF3E2A18),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFF9AA7B4),
    onSecondary = Color(0xFF101418),
    secondaryContainer = Color(0xFF232A33),
    onSecondaryContainer = Color(0xFFEAF0F6),
    tertiary = Color(0xFF07C160),
    onTertiary = Color(0xFF00210F),
    background = Color(0xFF101418),
    onBackground = Color(0xFFEAF0F6),
    surface = Color(0xFF1A1F26),
    onSurface = Color(0xFFEAF0F6),
    surfaceVariant = Color(0xFF232A33),
    onSurfaceVariant = Color(0xFF9AA7B4),
    outline = Color(0xFF2C333D),
    error = Color(0xFFE94634),
    onError = Color(0xFF2B0500),
)

/** Material3 玻璃拟态配色（浅色） */
private val GlassLightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFFE85D00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF3E2000),
    secondary = Color(0xFF6E7680),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F2F5),
    onSecondaryContainer = Color(0xFF17191D),
    tertiary = Color(0xFF07C160),
    onTertiary = Color.White,
    background = Color(0xFFF2F4F7),
    onBackground = Color(0xFF17191D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17191D),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF6E7680),
    outline = Color(0xFFE2E5EA),
    error = Color(0xFFD93A2B),
    onError = Color.White,
)

/** miuix 浅色配色（品牌橙 / Monet 主色） */
private fun miuixLightColors(primary: Color): Colors = miuixLightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryVariant = primary,
    onPrimaryVariant = Color.White,
    secondary = Color(0xFF7C838C),
    onSecondary = Color.White,
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C21),
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF181C21),
    surfaceVariant = Color(0xFFF8F9FB),
    onSurfaceSecondary = Color(0xFF5C636B),
    onSurfaceVariantSummary = Color(0xFF7C838C),
    dividerLine = Color(0xFFE6E8EB),
)

/** miuix 深色配色（品牌橙 / Monet 主色） */
private fun miuixDarkColors(primary: Color): Colors = miuixDarkColorScheme(
    primary = primary,
    onPrimary = Color(0xFF2B1500),
    primaryVariant = primary,
    onPrimaryVariant = Color(0xFF2B1500),
    secondary = Color(0xFF8B939D),
    onSecondary = Color(0xFF14171A),
    surface = Color(0xFF1D2126),
    onSurface = Color(0xFFE8EBEF),
    background = Color(0xFF14171A),
    onBackground = Color(0xFFE8EBEF),
    surfaceVariant = Color(0xFF23282E),
    onSurfaceSecondary = Color(0xFFB4BCC6),
    onSurfaceVariantSummary = Color(0xFF8B939D),
    dividerLine = Color(0xFF2B3138),
)

/** 系统栏外观同步：底色 + 图标深浅（API 23+/30+ 两段兼容）。 */
@Composable
private fun ApplySystemBars(dark: Boolean, sem: SemanticPalette) {
    val view = LocalView.current
    val ctx = LocalContext.current
    SideEffect {
        val window = (ctx as? Activity)?.window ?: return@SideEffect
        val bg = sem.background.toArgb()
        window.setStatusBarColor(bg)
        window.setNavigationBarColor(bg)
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= 30) {
            val insets = window.insetsController ?: return@SideEffect
            val appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            insets.setSystemBarsAppearance(
                if (dark) 0 else appearance,
                appearance,
            )
        } else {
            val navFlag = if (Build.VERSION.SDK_INT >= 26) {
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else 0
            val lightFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or navFlag
            @Suppress("DEPRECATION")
            view.systemUiVisibility = if (dark) {
                view.systemUiVisibility and lightFlags.inv()
            } else {
                view.systemUiVisibility or lightFlags
            }
        }
    }
}
