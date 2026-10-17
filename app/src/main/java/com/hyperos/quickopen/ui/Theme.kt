package com.hyperos.quickopen.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 双 UI 库主题枢纽：
 *  - UI_STYLE_MATERIAL → androidx.compose.material3 MaterialTheme（深色玻璃质感）
 *  - UI_STYLE_MIUIX    → top.yukonga.miuix.kmp MiuixTheme（MIUI 澎湃 OS 浅色卡片）
 *
 * 两个库共享同一套语义色板（primary / background / surface / divider …），
 * 由 [SemanticPalette] 提供单一事实来源，保证两种风格切换时品牌色一致。
 */

// ── 品牌语义色（单一事实来源） ──────────────────────────────────────────
object SemColors {
    val Brand = Color(0xFFFF6900)          // MIUI 暖橙（两风格统一品牌色）
    val BrandContainer = Color(0xFF3E2A18)
    val GlassBg = Color(0xFF101418)        // Material 深色玻璃底
    val GlassSurface = Color(0xFF1A1F26)
    val GlassSurfaceAlt = Color(0xFF232A33)
    val GlassDivider = Color(0xFF2C333D)
    val GlassFg = Color(0xFFEAF0F6)
    val GlassFgDim = Color(0xFF9AA7B4)
    val MiuixBg = Color(0xFFF3F4F6)
    val MiuixSurface = Color(0xFFFFFFFF)
    val MiuixDivider = Color(0xFFE6E8EB)
    val MiuixFg = Color(0xFF181C21)
    val MiuixFgDim = Color(0xFF7C838C)
    val Warn = Color(0xFFE94634)
    val Ok = Color(0xFF07C160)
}

/** 语义调色板：把选中风格的库主题颜色抽象成同一个接口视图 */
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

@Composable
fun currentSemantic(): SemanticPalette =
    if (LocalUiStyle.current == UiStyle.MATERIAL) {
        val c = MaterialTheme.colorScheme
        SemanticPalette(c.primary, c.background, c.surface, c.surfaceVariant, c.outlineVariant, c.onBackground, c.onSurfaceVariant, SemColors.Warn, SemColors.Ok)
    } else {
        val c = MiuixTheme.colorScheme
        SemanticPalette(c.primary, c.background, c.surface, c.surfaceVariant, c.dividerLine, c.onBackground, c.onBackgroundVariant, SemColors.Warn, SemColors.Ok)
    }

// ── Material3 深色玻璃 ─────────────────────────────────────────────────
private val GlassDarkScheme = darkColorScheme(
    primary = SemColors.Brand,
    onPrimary = Color.White,
    primaryContainer = SemColors.BrandContainer,
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFF3A424C),
    onSecondary = Color.White,
    background = SemColors.GlassBg,
    onBackground = SemColors.GlassFg,
    surface = SemColors.GlassSurface,
    onSurface = SemColors.GlassFg,
    surfaceVariant = SemColors.GlassSurfaceAlt,
    onSurfaceVariant = SemColors.GlassFgDim,
    outline = SemColors.GlassDivider,
    outlineVariant = SemColors.GlassDivider,
    error = SemColors.Warn,
)

private val GlassLightScheme = lightColorScheme(
    primary = SemColors.Brand,
    onPrimary = Color.White,
)

// ── MIUIX 浅色卡片 ─────────────────────────────────────────────────────
private val MiuixLight = lightColorScheme(
    primary = SemColors.Brand,
    onPrimary = Color.White,
    primaryVariant = SemColors.Brand,
    onPrimaryVariant = Color(0xFFFFD9B8),
    background = SemColors.MiuixBg,
    onBackground = SemColors.MiuixFg,
    onBackgroundVariant = SemColors.MiuixFgDim,
    surface = SemColors.MiuixSurface,
    onSurface = SemColors.MiuixFg,
    surfaceVariant = Color(0xFFF8F9FB),
    onSurfaceVariantSummary = SemColors.MiuixFgDim,
    dividerLine = SemColors.MiuixDivider,
    outline = SemColors.MiuixDivider,
    secondaryContainer = Color(0xFFFFEFE2),
    onSecondaryContainer = SemColors.Brand,
)

private val MiuixDark = darkColorScheme(
    primary = SemColors.Brand,
    onPrimary = Color.White,
    background = Color(0xFF14171A),
    onBackground = Color(0xFFE8EBEF),
    onBackgroundVariant = Color(0xFF8B939D),
    surface = Color(0xFF1D2126),
    onSurface = Color(0xFFE8EBEF),
    surfaceVariant = Color(0xFF23282E),
    dividerLine = Color(0xFF2B3138),
)

@Composable
fun HyperOSQuickOpenTheme(
    uiStyle: UiStyle,
    content: @Composable () -> Unit,
) {
    when (uiStyle) {
        UiStyle.MATERIAL -> {
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) GlassDarkScheme else GlassLightScheme,
                content = content,
            )
        }
        UiStyle.MIUIX -> {
            val dark = isSystemInDarkTheme()
            MiuixTheme(
                colors = if (dark) MiuixDark else MiuixLight,
                content = content,
            )
        }
    }
}
