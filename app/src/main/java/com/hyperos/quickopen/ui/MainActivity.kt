package com.hyperos.quickopen.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperos.quickopen.config.QuickOpenConfig
import com.hyperos.quickopen.model.ShortcutItem
import com.hyperos.quickopen.safety.SafetyStore
import com.hyperos.quickopen.view.IconGlyphFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.ScreenMirroring
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurBlendMode
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.util.UUID

/**
 * Compose 设置界面：全面采用 miuix 官方 example 架构模板。
 * 支持快捷方式参数详情编辑（图标/名称/动作选项等）、应用图标与 miuix 矢量图标自定义选取，以及新建快捷方式。
 */
class MainActivity : ComponentActivity() {

    internal var backPressConsumer: (() -> Boolean)? = null

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (backPressConsumer?.invoke() == true) {
            return
        }
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = QuickOpenConfig.loadFromApp(this)
        setContent {
            QuickOpenApp(activity = this, initialConfig = config)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// App 主结构
// ─────────────────────────────────────────────────────────────────────

@Composable
fun QuickOpenApp(
    activity: MainActivity? = null,
    initialConfig: QuickOpenConfig,
) {
    val ctx = LocalContext.current

    // 配置镜像状态
    var materialStyle by remember {
        mutableStateOf(
            if (initialConfig.materialStyle == QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR) {
                QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR
            } else {
                QuickOpenConfig.MATERIAL_SOFT_GLASS
            }
        )
    }
    var offsetY by remember { mutableFloatStateOf(initialConfig.offsetY.toFloat()) }
    var regionDiameter by remember { mutableFloatStateOf(initialConfig.regionDiameter.toFloat()) }
    var showRegionBg by remember { mutableStateOf(initialConfig.showRegionBg) }
    var showHeaderPillBg by remember { mutableStateOf(initialConfig.showHeaderPillBg) }
    var iconSize by remember { mutableFloatStateOf(initialConfig.iconSize.toFloat()) }
    var maxScale by remember { mutableFloatStateOf(initialConfig.maxScale) }
    var influenceRadius by remember { mutableFloatStateOf(initialConfig.influenceRadius.toFloat()) }
    var iconAlpha by remember { mutableFloatStateOf(initialConfig.iconAlpha) }
    var fisheyeZoomAlpha by remember { mutableFloatStateOf(initialConfig.fisheyeZoomAlpha) }
    var fullscreenBlurMaskAlpha by remember { mutableFloatStateOf(initialConfig.fullscreenBlurMaskAlpha) }
    var regionAlpha by remember { mutableFloatStateOf(initialConfig.regionAlpha) }
    var regionBlurRadius by remember { mutableFloatStateOf(initialConfig.regionBlurRadius.toFloat()) }
    var iconSpacingOffset by remember { mutableFloatStateOf(initialConfig.iconSpacingOffset.toFloat()) }
    var bubblePaddingPercent by remember { mutableFloatStateOf(initialConfig.bubblePaddingPercent.toFloat()) }
    var selectedIconScale by remember { mutableFloatStateOf(initialConfig.selectedIconScale) }
    var appIconCropMode by remember { mutableIntStateOf(initialConfig.appIconCropMode) }
    var glyphPadEnabled by remember { mutableStateOf(initialConfig.glyphPadEnabled) }
    var glyphPadColor by remember { mutableIntStateOf(initialConfig.glyphPadColor) }
    var glyphPadMode by remember { mutableIntStateOf(initialConfig.glyphPadMode) }
    var iconGlowEffect by remember { mutableStateOf(initialConfig.iconGlowEffect) }
    var themeMode by remember { mutableStateOf(initialConfig.themeMode) }
    var monetColors by remember { mutableStateOf(initialConfig.monetColors) }
    var barBlur by remember { mutableStateOf(initialConfig.barBlur) }
    var floatingNavBar by remember { mutableStateOf(initialConfig.floatingNavBar) }
    var predictiveBack by remember { mutableStateOf(initialConfig.predictiveBack) }
    val shortcuts = remember { mutableStateListOf<ShortcutItem>().apply { addAll(initialConfig.shortcuts) } }
    var safetyTick by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }

    // 导航路由：0=主界面, 1=主题设置页, 2=快捷方式编辑页
    var subPage by remember { mutableStateOf(0) }
    var editingItem by remember { mutableStateOf<ShortcutItem?>(null) }
    var isNewShortcut by remember { mutableStateOf(false) }
    var requestEditBackTrigger by remember { mutableIntStateOf(0) }
    var showNewShortcutPresetDialog by remember { mutableStateOf(false) }

    fun persist() {
        val c = QuickOpenConfig.loadFromApp(ctx)
        c.materialStyle = materialStyle
        c.offsetY = offsetY.toInt()
        c.regionDiameter = regionDiameter.toInt()
        c.showRegionBg = showRegionBg
        c.showHeaderPillBg = showHeaderPillBg
        c.iconSize = iconSize.toInt()
        c.maxScale = maxScale
        c.influenceRadius = influenceRadius.toInt()
        c.iconAlpha = iconAlpha
        c.fisheyeZoomAlpha = fisheyeZoomAlpha
        c.fullscreenBlurMaskAlpha = fullscreenBlurMaskAlpha
        c.regionAlpha = regionAlpha
        c.regionBlurRadius = regionBlurRadius.toInt()
        c.iconSpacingOffset = iconSpacingOffset.toInt()
        c.bubblePaddingPercent = bubblePaddingPercent.toInt()
        c.selectedIconScale = selectedIconScale
        c.appIconCropMode = appIconCropMode
        c.glyphPadEnabled = glyphPadEnabled
        c.glyphPadColor = glyphPadColor
        c.glyphPadMode = glyphPadMode
        c.iconGlowEffect = iconGlowEffect
        c.themeMode = themeMode
        c.monetColors = monetColors
        c.barBlur = barBlur
        c.floatingNavBar = floatingNavBar
        c.predictiveBack = predictiveBack
        c.shortcuts = ArrayList(shortcuts)
        c.save(ctx)
    }

    // 拦截系统返回键与手势返回：在二级页时优先处理
    LaunchedEffect(activity, subPage) {
        activity?.backPressConsumer = {
            if (subPage == 2) {
                requestEditBackTrigger++
                true
            } else if (subPage != 0) {
                subPage = 0
                editingItem = null
                true
            } else {
                false
            }
        }
    }
    BackHandler(enabled = subPage != 0) {
        if (subPage == 2) {
            requestEditBackTrigger++
        } else {
            subPage = 0
            editingItem = null
        }
    }

    HyperOSQuickOpenTheme(
        themeMode = themeMode,
        monet = monetColors,
    ) {
        val topAppBarScrollBehavior = MiuixScrollBehavior()
        val title = when (subPage) {
            1 -> "主题设置"
            2 -> if (isNewShortcut) "新建快捷方式" else "快捷方式参数设置"
            3 -> "指纹快捷整体预览"
            else -> when (page) {
                0 -> "参数配置"
                1 -> "快捷项目"
                else -> "软件设置"
            }
        }

        val navItems = remember {
            listOf(
                NavigationItem("参数配置", MiuixIcons.Tune),
                NavigationItem("快捷项目", MiuixIcons.ListView),
                NavigationItem("软件设置", MiuixIcons.Settings),
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (subPage != 3) {
                    TopAppBar(
                        title = title,
                        scrollBehavior = topAppBarScrollBehavior,
                        navigationIcon = {
                            if (subPage != 0) {
                                BackNavigationIcon(onClick = {
                                    if (subPage == 2) {
                                        requestEditBackTrigger++
                                    } else {
                                        subPage = 0
                                        editingItem = null
                                    }
                                })
                            }
                        },
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = subPage == 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    AppBottomBar(
                        items = navItems,
                        selectedPage = page,
                        floating = floatingNavBar,
                        onPageSelect = { page = it },
                    )
                }
            },
        ) { innerPadding ->
            val pagePadding = if (subPage == 3) PaddingValues(0.dp) else innerPadding
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            ) {
                when (subPage) {
                    1 -> {
                        ThemeSettingsPage(
                            contentPadding = pagePadding,
                            themeMode = themeMode,
                            onThemeMode = { themeMode = it; persist() },
                            monet = monetColors,
                            onMonet = { monetColors = it; persist() },
                            floatingNavBar = floatingNavBar,
                            onFloatingNavBar = { floatingNavBar = it; persist() },
                        )
                    }
                    2 -> {
                        editingItem?.let { currentEdit ->
                            ShortcutEditPage(
                                contentPadding = pagePadding,
                                initialItem = currentEdit,
                                isNew = isNewShortcut,
                                backTrigger = requestEditBackTrigger,
                                onSave = { updated ->
                                    if (isNewShortcut) {
                                        shortcuts.add(updated)
                                        Toast.makeText(ctx, "已添加快捷方式: ${updated.title}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val idx = shortcuts.indexOfFirst { it.id == updated.id }
                                        if (idx >= 0) {
                                            shortcuts[idx] = updated.copy()
                                            Toast.makeText(ctx, "已保存参数设置", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    persist()
                                    subPage = 0
                                    editingItem = null
                                },
                                onDelete = { toDelete ->
                                    shortcuts.removeAll { it.id == toDelete.id }
                                    persist()
                                    Toast.makeText(ctx, "已删除快捷方式", Toast.LENGTH_SHORT).show()
                                    subPage = 0
                                    editingItem = null
                                },
                                onExitWithoutSave = {
                                    subPage = 0
                                    editingItem = null
                                },
                            )
                        }
                    }
                    3 -> {
                        // 指纹快捷方式整体预览与长按自由排版界面
                        val currentConfig = QuickOpenConfig.loadFromApp(ctx).apply {
                            this.materialStyle = materialStyle
                            this.offsetY = offsetY.toInt()
                            this.regionDiameter = regionDiameter.toInt()
                            this.showRegionBg = showRegionBg
                            this.showHeaderPillBg = showHeaderPillBg
                            this.iconSize = iconSize.toInt()
                            this.maxScale = maxScale
                            this.influenceRadius = influenceRadius.toInt()
                            this.iconAlpha = iconAlpha
                            this.fisheyeZoomAlpha = fisheyeZoomAlpha
                            this.fullscreenBlurMaskAlpha = fullscreenBlurMaskAlpha
                            this.regionAlpha = regionAlpha
                            this.regionBlurRadius = regionBlurRadius.toInt()
                            this.iconSpacingOffset = iconSpacingOffset.toInt()
                            this.bubblePaddingPercent = bubblePaddingPercent.toInt()
                            this.selectedIconScale = selectedIconScale
                            this.appIconCropMode = appIconCropMode
                            this.iconGlowEffect = iconGlowEffect
                            this.shortcuts = ArrayList(shortcuts)
                        }

                        val previewBackdrop = rememberLayerBackdrop {
                            drawRect(Color(0xFF0C0E14))
                            drawContent()
                        }

                        // 彻底实现系统桌面壁纸完全自动、实时动态拉取与变更监听
                        val liveWallpaperBitmap by com.hyperos.quickopen.util.LiveWallpaperLoader.rememberLiveWallpaper(ctx)

                        Box(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // 1. 底层真实桌面壁纸层 (使用 layerBackdrop 捕获供 miuix-blur 进行次世代毛玻璃实时运算)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(previewBackdrop)
                            ) {
                                val currentWallpaper = liveWallpaperBitmap
                                if (currentWallpaper != null) {
                                    Image(
                                        bitmap = currentWallpaper,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(Color(0xFF0D1117))
                                        drawCircle(
                                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                                colors = listOf(Color(0xFF2A82E4).copy(alpha = 0.45f), Color.Transparent),
                                                center = androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.38f),
                                                radius = size.width * 0.65f
                                            )
                                        )
                                        drawCircle(
                                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                                colors = listOf(Color(0xFF7928CA).copy(alpha = 0.40f), Color.Transparent),
                                                center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.68f),
                                                radius = size.width * 0.70f
                                            )
                                        )
                                    }
                                }
                            }

                            // 2. 全屏毛玻璃遮罩层 (与全屏遮罩滑块实时独立联动，关闭时全屏壁纸 100% 原始清晰)
                            if (currentConfig.fullscreenBlurMaskAlpha > 0.001f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .textureBlur(
                                            backdrop = previewBackdrop,
                                            shape = RectangleShape,
                                            blurRadius = (currentConfig.fullscreenBlurMaskAlpha * 40f).coerceAtLeast(1f),
                                            colors = BlurColors(
                                                blendColors = listOf(
                                                    BlendColorEntry(
                                                        color = Color.Black.copy(alpha = 0.55f * currentConfig.fullscreenBlurMaskAlpha),
                                                        mode = BlurBlendMode.SrcOver
                                                    )
                                                )
                                            )
                                        )
                                )
                            }

                            // 3. 圆形视界区域的独立毛玻璃 (实时由 miuix-blur 硬件渲染，绝不死黑，与真实锁屏效果完全一致)
                            if (currentConfig.showRegionBg) {
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val screenH = ctx.resources.displayMetrics.heightPixels
                                val screenW = ctx.resources.displayMetrics.widthPixels
                                val fodYPx = screenH * 0.795f
                                val fodXPx = screenW / 2.0f
                                val regionCenterYPx = fodYPx - with(density) { currentConfig.offsetY.dp.toPx() }
                                val regionDiameterDp = currentConfig.regionDiameter.dp
                                val regionRadiusPx = with(density) { (regionDiameterDp / 2).toPx() }

                                Box(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .offset(
                                                x = with(density) { (fodXPx - regionRadiusPx).toDp() },
                                                y = with(density) { (regionCenterYPx - regionRadiusPx).toDp() }
                                            )
                                            .size(regionDiameterDp)
                                            .textureBlur(
                                                backdrop = previewBackdrop,
                                                shape = CircleShape,
                                                blurRadius = currentConfig.regionBlurRadius.toFloat().coerceAtLeast(1f),
                                                colors = BlurColors(
                                                    blendColors = listOf(
                                                        BlendColorEntry(
                                                            color = if (materialStyle == QuickOpenConfig.MATERIAL_SOFT_GLASS)
                                                                Color.White.copy(alpha = 0.12f * currentConfig.regionAlpha)
                                                            else
                                                                Color.White.copy(alpha = 0.06f * currentConfig.regionAlpha),
                                                            mode = BlurBlendMode.SrcOver
                                                        )
                                                    ),
                                                    saturation = if (materialStyle == QuickOpenConfig.MATERIAL_SOFT_GLASS) 1.65f else 1.20f
                                                ),
                                                highlight = if (materialStyle == QuickOpenConfig.MATERIAL_SOFT_GLASS)
                                                    Highlight.GlassStrokeBigLight
                                                else
                                                    Highlight.GlassStrokeBigDark
                                            )
                                    )
                                }
                            }

                            // 4. 交互式预览与拖拽排版控件
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { viewCtx ->
                                    QuickOpenPreviewView(
                                        context = viewCtx,
                                        config = currentConfig,
                                        onBackClick = {
                                            subPage = 0
                                        },
                                        onOrderChanged = { newOrder ->
                                            // 保持启用/未启用项与整体列表同步重排
                                            val reordered = ArrayList<ShortcutItem>()
                                            reordered.addAll(newOrder)
                                            for (orig in shortcuts) {
                                                if (reordered.none { it.id == orig.id }) {
                                                    reordered.add(orig)
                                                }
                                            }
                                            shortcuts.clear()
                                            shortcuts.addAll(reordered)
                                            persist()
                                            Toast.makeText(viewCtx, "已更新并保存快捷方式排版", Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                },
                                update = { previewView ->
                                    previewView.updateConfig(currentConfig)
                                },
                            )
                        }
                    }
                    else -> {
                        when (page) {
                            0 -> {
                                ParamsPage(
                                    contentPadding = innerPadding,
                                    materialStyle = materialStyle,
                                    onMaterialStyle = { materialStyle = it; persist() },
                                    offsetY = offsetY,
                                    onOffsetY = { offsetY = it; persist() },
                                    regionDiameter = regionDiameter,
                                    onRegionDiameter = { regionDiameter = it; persist() },
                                    showRegionBg = showRegionBg,
                                    onShowRegionBg = { showRegionBg = it; persist() },
                                    showHeaderPillBg = showHeaderPillBg,
                                    onShowHeaderPillBg = { showHeaderPillBg = it; persist() },
                                    iconSize = iconSize,
                                    onIconSize = { iconSize = it; persist() },
                                    maxScale = maxScale,
                                    onMaxScale = { maxScale = it; persist() },
                                    influenceRadius = influenceRadius,
                                    onInfluenceRadius = { influenceRadius = it; persist() },
                                    iconAlpha = iconAlpha,
                                    onIconAlpha = { iconAlpha = it; persist() },
                                    fisheyeZoomAlpha = fisheyeZoomAlpha,
                                    onFisheyeZoomAlpha = { fisheyeZoomAlpha = it; persist() },
                                    fullscreenBlurMaskAlpha = fullscreenBlurMaskAlpha,
                                    onFullscreenBlurMaskAlpha = { fullscreenBlurMaskAlpha = it; persist() },
                                    regionAlpha = regionAlpha,
                                    onRegionAlpha = { regionAlpha = it; persist() },
                                    regionBlurRadius = regionBlurRadius,
                                    onRegionBlurRadius = { regionBlurRadius = it; persist() },
                                    iconSpacingOffset = iconSpacingOffset,
                                    onIconSpacingOffset = { iconSpacingOffset = it; persist() },
                                    bubblePaddingPercent = bubblePaddingPercent,
                                    onBubblePaddingPercent = { bubblePaddingPercent = it; persist() },
                                    selectedIconScale = selectedIconScale,
                                    onSelectedIconScale = { selectedIconScale = it; persist() },
                                    appIconCropMode = appIconCropMode,
                                    onAppIconCropMode = { appIconCropMode = it; persist() },
                                    iconGlowEffect = iconGlowEffect,
                                    onIconGlowEffect = { iconGlowEffect = it; persist() },
                                )
                            }
                            1 -> {
                                ShortcutsManagementPage(
                                    contentPadding = innerPadding,
                                    shortcuts = shortcuts,
                                    onOpenPreview = {
                                        subPage = 3
                                    },
                                    onShortcutToggle = { item, enabled ->
                                        val i = shortcuts.indexOfFirst { it.id == item.id }
                                        if (i >= 0) {
                                            val updated = item.copy().apply { this.enabled = enabled }
                                            shortcuts[i] = updated
                                        }
                                        persist()
                                    },
                                    onShortcutClick = { item ->
                                        requestEditBackTrigger = 0
                                        editingItem = item.copy()
                                        isNewShortcut = false
                                        subPage = 2
                                    },
                                    onAddShortcut = {
                                        showNewShortcutPresetDialog = true
                                    },
                                    onResetShortcuts = {
                                        shortcuts.clear()
                                        shortcuts.addAll(QuickOpenConfig.getDefaultShortcuts())
                                        persist()
                                        Toast.makeText(ctx, "已恢复默认快捷项目", Toast.LENGTH_SHORT).show()
                                    },
                                )
                            }
                            else -> {
                                SettingsPage(
                                    contentPadding = innerPadding,
                                    safetyTick = safetyTick,
                                    onSafetyRefresh = { safetyTick++ },
                                    onOpenThemePage = { subPage = 1 },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showNewShortcutPresetDialog) {
            PresetActionPickerDialog(
                onDismiss = { showNewShortcutPresetDialog = false },
                onSelect = { preset ->
                    showNewShortcutPresetDialog = false
                    requestEditBackTrigger = 0
                    editingItem = ShortcutItem(
                        UUID.randomUUID().toString(),
                        preset.title,
                        preset.subtitle,
                        preset.actionType,
                        preset.actionPayload,
                        preset.iconPackage,
                        preset.iconGlyph,
                        preset.tintColor,
                        true,
                        preset.glyphPadEnabled,
                        preset.glyphPadColor,
                        preset.glyphPadMode,
                    )
                    isNewShortcut = true
                    subPage = 2
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 顶部返回按钮组件（miuix 官方模板 component/BackNavigationIcon.kt 规范）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun BackNavigationIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    IconButton(
        modifier = modifier,
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.graphicsLayer {
                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
            },
            imageVector = MiuixIcons.Back,
            contentDescription = "返回",
            tint = MiuixTheme.colorScheme.onBackground,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// 底部导航栏组件（遵循 miuix 官方模板 AppContent.kt 规范）
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun AppBottomBar(
    items: List<NavigationItem>,
    selectedPage: Int,
    floating: Boolean,
    onPageSelect: (Int) -> Unit,
) {
    if (floating) {
        FloatingNavigationBar(
            showDivider = true,
        ) {
            items.forEachIndexed { index, item ->
                FloatingNavigationBarItem(
                    selected = selectedPage == index,
                    onClick = { onPageSelect(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    } else {
        NavigationBar(
            showDivider = true,
        ) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = selectedPage == index,
                    onClick = { onPageSelect(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Page0：参数配置（miuix 官方 Preference 规范）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ParamsPage(
    contentPadding: PaddingValues,
    materialStyle: Int, onMaterialStyle: (Int) -> Unit,
    offsetY: Float, onOffsetY: (Float) -> Unit,
    regionDiameter: Float, onRegionDiameter: (Float) -> Unit,
    showRegionBg: Boolean, onShowRegionBg: (Boolean) -> Unit,
    showHeaderPillBg: Boolean, onShowHeaderPillBg: (Boolean) -> Unit,
    iconSize: Float, onIconSize: (Float) -> Unit,
    maxScale: Float, onMaxScale: (Float) -> Unit,
    influenceRadius: Float, onInfluenceRadius: (Float) -> Unit,
    iconAlpha: Float, onIconAlpha: (Float) -> Unit,
    fisheyeZoomAlpha: Float, onFisheyeZoomAlpha: (Float) -> Unit,
    fullscreenBlurMaskAlpha: Float, onFullscreenBlurMaskAlpha: (Float) -> Unit,
    regionAlpha: Float, onRegionAlpha: (Float) -> Unit,
    regionBlurRadius: Float, onRegionBlurRadius: (Float) -> Unit,
    iconSpacingOffset: Float, onIconSpacingOffset: (Float) -> Unit,
    bubblePaddingPercent: Float, onBubblePaddingPercent: (Float) -> Unit,
    selectedIconScale: Float, onSelectedIconScale: (Float) -> Unit,
    appIconCropMode: Int, onAppIconCropMode: (Int) -> Unit,
    iconGlowEffect: Boolean, onIconGlowEffect: (Boolean) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 材质与系统风格 ──
        item(key = "material_title") { SmallTitle("材质与系统风格") }
        item(key = "material_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                ArrowPreference(
                    title = "液态玻璃",
                    summary = "Liquid Glass（高动态次表面散射与水润折射）",
                    endActions = {
                        if (materialStyle == QuickOpenConfig.MATERIAL_SOFT_GLASS) {
                            Text("✓", color = MiuixTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    },
                    onClick = { onMaterialStyle(QuickOpenConfig.MATERIAL_SOFT_GLASS) },
                )
                ArrowPreference(
                    title = "高斯模糊",
                    summary = "Gaussian Blur（沉稳细腻深色哑光微晶磨砂）",
                    endActions = {
                        if (materialStyle == QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR) {
                            Text("✓", color = MiuixTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    },
                    onClick = { onMaterialStyle(QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR) },
                )
                SliderPreference(
                    title = "全屏模糊遮罩背景透明度",
                    valueText = "${(fullscreenBlurMaskAlpha * 100).toInt()} %",
                    value = fullscreenBlurMaskAlpha * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { onFullscreenBlurMaskAlpha(it / 100f) },
                )
            }
        }

        // ── 几何与位置参数 ──
        item(key = "geometry_title") { SmallTitle("几何与位置参数") }
        item(key = "geometry_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                SliderPreference(
                    title = "整体垂直高度偏移",
                    valueText = "${offsetY.toInt()} dp",
                    value = offsetY,
                    valueRange = 100f..450f,
                    onValueChange = onOffsetY,
                )
                SliderPreference(
                    title = "圆形视界直径",
                    valueText = "${regionDiameter.toInt()} dp",
                    value = regionDiameter,
                    valueRange = 220f..420f,
                    onValueChange = onRegionDiameter,
                )
                SwitchPreference(
                    title = "显示圆形区域底衬与边框",
                    checked = showRegionBg,
                    onCheckedChange = onShowRegionBg,
                )
                if (showRegionBg) {
                    SliderPreference(
                        title = "圆形视界底衬透明度",
                        valueText = "${(regionAlpha * 100).toInt()} %",
                        value = regionAlpha * 100f,
                        valueRange = 0f..100f,
                        onValueChange = { onRegionAlpha(it / 100f) },
                    )
                    SliderPreference(
                        title = "圆形视界背景模糊半径",
                        summary = "结合高斯模糊与次表面散射渲染毛玻璃底盘",
                        valueText = if (regionBlurRadius.toInt() == 0) "已关闭模糊" else "${regionBlurRadius.toInt()} dp",
                        value = regionBlurRadius,
                        valueRange = 0f..50f,
                        onValueChange = onRegionBlurRadius,
                    )
                }
                SwitchPreference(
                    title = "显示顶部提示胶囊背景",
                    checked = showHeaderPillBg,
                    onCheckedChange = onShowHeaderPillBg,
                )
            }
        }

        // ── 图标与物理动力学 ──
        item(key = "physics_title") { SmallTitle("图标与物理动力学") }
        item(key = "physics_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                SliderPreference(
                    title = "图标基础尺寸",
                    valueText = "${iconSize.toInt()} dp",
                    value = iconSize,
                    valueRange = 40f..80f,
                    onValueChange = onIconSize,
                )
                SliderPreference(
                    title = "图标常态透明度",
                    valueText = "${(iconAlpha * 100).toInt()} %",
                    value = iconAlpha * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { onIconAlpha(it / 100f) },
                )
                SliderPreference(
                    title = "鱼眼放大时透明度",
                    summary = "手指划过受鱼眼动力学放大时的动态透明度",
                    valueText = "${(fisheyeZoomAlpha * 100).toInt()} %",
                    value = fisheyeZoomAlpha * 100f,
                    valueRange = 0f..100f,
                    onValueChange = { onFisheyeZoomAlpha(it / 100f) },
                )
                SliderPreference(
                    title = "蜂窝图标紧凑程度",
                    valueText = when {
                        iconSpacingOffset.toInt() < 0 -> "紧凑 (${iconSpacingOffset.toInt()} dp)"
                        iconSpacingOffset.toInt() > 0 -> "松散 (+${iconSpacingOffset.toInt()} dp)"
                        else -> "标准 (0 dp)"
                    },
                    value = iconSpacingOffset,
                    valueRange = -10f..20f,
                    onValueChange = onIconSpacingOffset,
                )
                SliderPreference(
                    title = "鱼眼最大放大倍率",
                    valueText = "%.2f x".format(maxScale),
                    value = maxScale,
                    valueRange = 1.10f..1.80f,
                    onValueChange = onMaxScale,
                )
                SliderPreference(
                    title = "感应辐射范围",
                    valueText = "${influenceRadius.toInt()} dp",
                    value = influenceRadius,
                    valueRange = 60f..200f,
                    onValueChange = onInfluenceRadius,
                )
                SliderPreference(
                    title = "气泡内部留白比例",
                    valueText = "${bubblePaddingPercent.toInt()} %",
                    value = bubblePaddingPercent,
                    valueRange = 0f..40f,
                    onValueChange = onBubblePaddingPercent,
                )
                SliderPreference(
                    title = "选中时图标缩放倍率",
                    valueText = "%.2f x".format(selectedIconScale),
                    value = selectedIconScale,
                    valueRange = 1.00f..1.50f,
                    onValueChange = onSelectedIconScale,
                )
                SwitchPreference(
                    title = "图标激活呼吸辉光动效",
                    summary = "手指滑过选中目标时泛起柔光微晕反馈",
                    checked = iconGlowEffect,
                    onCheckedChange = onIconGlowEffect,
                )
            }
        }

        // ── 应用图标裁切与样式方案 ──
        item(key = "app_icon_crop_title") { SmallTitle("应用图标形状方案") }
        item(key = "app_icon_crop_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                RadioButtonPreference(
                    title = "自适应外扩满铺 (推荐)",
                    summary = "消除白底留白隔阂，图标背景自然铺满圆形气泡",
                    selected = appIconCropMode == QuickOpenConfig.CROP_ADAPTIVE_EXPAND,
                    onClick = { onAppIconCropMode(QuickOpenConfig.CROP_ADAPTIVE_EXPAND) },
                )
                RadioButtonPreference(
                    title = "标准抗锯齿圆形裁切",
                    summary = "无论方形还是异形应用图标，强制高精度裁切为圆徽章",
                    selected = appIconCropMode == QuickOpenConfig.CROP_BITMAP_CIRCLE,
                    onClick = { onAppIconCropMode(QuickOpenConfig.CROP_BITMAP_CIRCLE) },
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Page1：快捷项目管理（独立底栏页面）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ShortcutsManagementPage(
    contentPadding: PaddingValues,
    shortcuts: SnapshotStateList<ShortcutItem>,
    onOpenPreview: () -> Unit,
    onShortcutToggle: (ShortcutItem, Boolean) -> Unit,
    onShortcutClick: (ShortcutItem) -> Unit,
    onAddShortcut: () -> Unit,
    onResetShortcuts: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 操作按钮卡片 ──
        item(key = "actions_card") {
            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = "快捷方式整体预览",
                        summary = "所见即所得，支持长按图标拖拽调整蜂窝顺序",
                        startAction = {
                            Icon(
                                imageVector = MiuixIcons.ScreenMirroring,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        },
                        onClick = onOpenPreview,
                    )
                    ArrowPreference(
                        title = "新建快捷方式",
                        summary = "添加自定义应用、Intent 或快捷指令",
                        startAction = {
                            Icon(
                                imageVector = MiuixIcons.Add,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        },
                        onClick = onAddShortcut,
                    )
                    ArrowPreference(
                        title = "恢复预设布局",
                        summary = "重置为微信扫一扫/付款码/扫一扫等初始快捷项",
                        startAction = {
                            Icon(
                                imageVector = MiuixIcons.Reset,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        },
                        onClick = onResetShortcuts,
                    )
                }
            }
        }

        // ── 快捷项目列表 ──
        item(key = "shortcuts_title") { SmallTitle("快捷项目列表 (${shortcuts.size})") }
        itemsIndexed(shortcuts, key = { _, it -> it.id }) { _, item ->
            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                ShortcutRow(
                    item = item,
                    onToggle = { onShortcutToggle(item, it) },
                    onClick = { onShortcutClick(item) },
                )
            }
        }

        item(key = "footer_tips") {
            Text(
                "提示：轻触卡片可修改参数（图标、字重底垫、名称及启动选项），右侧开关控制是否在指纹面板中展示。",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 快捷方式列表项：点击卡片进入编辑，点击右侧开关切换启用状态
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ShortcutRow(
    item: ShortcutItem,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val painter = remember(item.id, item.iconPackage, item.iconGlyph) {
        val miuixIcon = MiuixIconRegistry.find(item.iconGlyph)
        if (item.iconPackage.isBlank() && miuixIcon != null) {
            null
        } else {
            IconPainter.resolve(item, ctx)
        }
    }
    val miuixIcon = remember(item.iconPackage, item.iconGlyph) {
        if (item.iconPackage.isBlank()) MiuixIconRegistry.find(item.iconGlyph) else null
    }

    val itemPadBg = if (miuixIcon != null && item.glyphPadEnabled) {
        when (item.glyphPadMode) {
            QuickOpenConfig.GLYPH_PAD_MODE_SOLID -> Color(item.glyphPadColor)
            QuickOpenConfig.GLYPH_PAD_MODE_INVERTED -> Color(0x30FFFFFF)
            else -> Color(item.glyphPadColor).copy(alpha = 0.65f)
        }
    } else {
        MiuixTheme.colorScheme.surfaceVariant
    }
    val itemIconTint = if (miuixIcon != null && item.glyphPadEnabled) {
        if (item.glyphPadMode == QuickOpenConfig.GLYPH_PAD_MODE_INVERTED) Color(item.glyphPadColor)
        else Color.White
    } else {
        MiuixTheme.colorScheme.primary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = item.title,
            summary = item.subtitle.ifBlank { "点击编辑快捷方式参数" },
            startAction = {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(itemPadBg),
                    contentAlignment = Alignment.Center,
                ) {
                    if (miuixIcon != null) {
                        Icon(
                            imageVector = miuixIcon,
                            contentDescription = item.title,
                            tint = itemIconTint,
                            modifier = Modifier.size(if (item.glyphPadEnabled) 20.dp else 24.dp),
                        )
                    } else if (painter != null) {
                        Image(
                            painter = painter,
                            contentDescription = item.title,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            },
            endActions = {
                Switch(
                    checked = item.enabled,
                    onCheckedChange = onToggle,
                )
            },
            onClick = onClick,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// 快捷方式参数编辑界面（全新二级设置界面，完全符合 miuix 规范）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ShortcutEditPage(
    contentPadding: PaddingValues,
    initialItem: ShortcutItem,
    isNew: Boolean,
    backTrigger: Int = 0,
    onSave: (ShortcutItem) -> Unit,
    onDelete: (ShortcutItem) -> Unit,
    onExitWithoutSave: () -> Unit,
) {
    val ctx = LocalContext.current
    val lazyListState = rememberLazyListState()

    var title by remember { mutableStateOf(initialItem.title) }
    var subtitle by remember { mutableStateOf(initialItem.subtitle) }
    var actionType by remember { mutableStateOf(initialItem.actionType) }
    var actionPayload by remember { mutableStateOf(initialItem.actionPayload) }
    var iconPackage by remember { mutableStateOf(initialItem.iconPackage) }
    var iconGlyph by remember { mutableStateOf(initialItem.iconGlyph) }
    var glyphPadEnabled by remember { mutableStateOf(initialItem.glyphPadEnabled) }
    var glyphPadColor by remember { mutableIntStateOf(initialItem.glyphPadColor) }
    var glyphPadMode by remember { mutableIntStateOf(initialItem.glyphPadMode) }
    var enabled by remember { mutableStateOf(initialItem.enabled) }

    var showAppPicker by remember { mutableStateOf(false) }
    var showMiuixIconPicker by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // 检查是否有未保存的修改
    val isModified = remember(title, subtitle, actionType, actionPayload, iconPackage, iconGlyph, glyphPadEnabled, glyphPadColor, glyphPadMode, enabled) {
        title != initialItem.title ||
        subtitle != initialItem.subtitle ||
        actionType != initialItem.actionType ||
        actionPayload != initialItem.actionPayload ||
        iconPackage != initialItem.iconPackage ||
        iconGlyph != initialItem.iconGlyph ||
        glyphPadEnabled != initialItem.glyphPadEnabled ||
        glyphPadColor != initialItem.glyphPadColor ||
        glyphPadMode != initialItem.glyphPadMode ||
        enabled != initialItem.enabled
    }

    // 响应顶部返回键或系统返回手势（记录初始 trigger，只有在当前页面内新发生点击时才响应）
    var lastHandledTrigger by remember { mutableIntStateOf(backTrigger) }
    LaunchedEffect(backTrigger) {
        if (backTrigger > lastHandledTrigger) {
            lastHandledTrigger = backTrigger
            if (isModified || isNew) {
                showUnsavedDialog = true
            } else {
                onExitWithoutSave()
            }
        }
    }

    // 实时图标预览
    val miuixVector = remember(iconPackage, iconGlyph) {
        if (iconPackage.isBlank()) MiuixIconRegistry.find(iconGlyph) else null
    }
    val appDrawablePainter = remember(iconPackage, iconGlyph, actionType, actionPayload) {
        if (miuixVector != null) {
            null
        } else {
            val d = when {
                iconPackage.isNotBlank() -> IconGlyphFactory.appIcon(ctx, iconPackage.trim())
                actionType == ShortcutItem.ACTION_APP && actionPayload.isNotBlank() -> IconGlyphFactory.appIcon(ctx, actionPayload.trim())
                else -> IconGlyphFactory.createGlyph(iconGlyph.ifBlank { "sparkle" }, initialItem.tintColor)
            }
            if (d != null) {
                val processed = com.hyperos.quickopen.view.AppIconCropProcessor.process(d, QuickOpenConfig.CROP_ADAPTIVE_EXPAND) ?: d
                val bmp = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                processed.setBounds(0, 0, 96, 96)
                processed.draw(canvas)
                BitmapPainter(bmp.toComposeImageBitmap())
            } else null
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 预览卡片 ──
        item(key = "preview_card") {
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val isMiuixSelected = miuixVector != null && iconPackage.isBlank()
                        val previewPadBg = if (isMiuixSelected && glyphPadEnabled) {
                            when (glyphPadMode) {
                                QuickOpenConfig.GLYPH_PAD_MODE_SOLID -> Color(glyphPadColor)
                                QuickOpenConfig.GLYPH_PAD_MODE_INVERTED -> Color(0x30FFFFFF)
                                else -> Color(glyphPadColor).copy(alpha = 0.65f)
                            }
                        } else {
                            MiuixTheme.colorScheme.surfaceVariant
                        }
                        val previewIconTint = if (isMiuixSelected && glyphPadEnabled) {
                            if (glyphPadMode == QuickOpenConfig.GLYPH_PAD_MODE_INVERTED) Color(glyphPadColor)
                            else Color.White
                        } else {
                            MiuixTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(previewPadBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (miuixVector != null) {
                                Icon(
                                    imageVector = miuixVector,
                                    contentDescription = title,
                                    tint = previewIconTint,
                                    modifier = Modifier.size(if (glyphPadEnabled) 32.dp else 38.dp),
                                )
                            } else if (appDrawablePainter != null) {
                                Image(
                                    painter = appDrawablePainter,
                                    contentDescription = title,
                                    modifier = Modifier.size(52.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = title.ifBlank { "快捷方式名称" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                text = subtitle,
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        TextButton(
                            text = "立即启动测试",
                            onClick = {
                                val testItem = initialItem.copy().apply {
                                    this.title = title.trim().ifBlank { "测试快捷方式" }
                                    this.actionType = actionType
                                    this.actionPayload = actionPayload.trim()
                                }
                                testItem.execute(ctx)
                            },
                        )
                    }
                }
            }
        }

        // ── 图标自定义配置 ──
        item(key = "icon_settings_title") { SmallTitle("图标设置") }
        item(key = "icon_settings_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                ArrowPreference(
                    title = "选择系统/应用图标",
                    summary = if (iconPackage.isNotBlank()) "当前选择：$iconPackage" else "未选择（使用动作或默认图标）",
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Photos,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    },
                    onClick = { showAppPicker = true },
                )
                ArrowPreference(
                    title = "选择 Miuix 矢量图标",
                    summary = if (iconPackage.isBlank() && iconGlyph.isNotBlank() && miuixVector != null) "当前选择：$iconGlyph" else "未选择",
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Scan,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    },
                    onClick = { showMiuixIconPicker = true },
                )
                if (iconPackage.isNotBlank() || iconGlyph.isNotBlank()) {
                    ArrowPreference(
                        title = "清除自定义图标",
                        summary = "恢复跟随启动应用或默认符号",
                        onClick = {
                            iconPackage = ""
                            iconGlyph = ""
                        },
                    )
                }
            }
        }

        // ── 仅当选择 Miuix 矢量图标时，展示独享的底垫与色彩配置 ──
        if (miuixVector != null && iconPackage.isBlank()) {
            item(key = "single_miuix_pad_title") { SmallTitle("Miuix 矢量图标底垫与色彩") }
            item(key = "single_miuix_pad_card") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = "启用此图标圆形底垫",
                        summary = "为当前矢量图标添加独立圆形底板",
                        checked = glyphPadEnabled,
                        onCheckedChange = { glyphPadEnabled = it },
                    )

                    if (glyphPadEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Text(
                            text = "底垫渲染质感",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                        )

                        RadioButtonPreference(
                            title = "磨砂微透底板 (推荐)",
                            summary = "65% 半透明高光底板，颜色与背景毛玻璃相互交融",
                            selected = glyphPadMode == QuickOpenConfig.GLYPH_PAD_MODE_TRANSLUCENT,
                            onClick = { glyphPadMode = QuickOpenConfig.GLYPH_PAD_MODE_TRANSLUCENT },
                        )
                        RadioButtonPreference(
                            title = "饱满纯色底板",
                            summary = "100% 不透明纯色实心圆，上方呈现白色高对比矢量",
                            selected = glyphPadMode == QuickOpenConfig.GLYPH_PAD_MODE_SOLID,
                            onClick = { glyphPadMode = QuickOpenConfig.GLYPH_PAD_MODE_SOLID },
                        )
                        RadioButtonPreference(
                            title = "反色渲染模式",
                            summary = "微透亮白底板 + 彩色矢量图标",
                            selected = glyphPadMode == QuickOpenConfig.GLYPH_PAD_MODE_INVERTED,
                            onClick = { glyphPadMode = QuickOpenConfig.GLYPH_PAD_MODE_INVERTED },
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Text(
                            text = "快捷品牌色预设",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp),
                        )

                        val brandColors = listOf(
                            "微信绿" to 0xFF07C160.toInt(),
                            "支付宝蓝" to 0xFF1677FF.toInt(),
                            "小米橙" to 0xFFFF6900.toInt(),
                            "网易红" to 0xFFE60026.toInt(),
                            "极客紫" to 0xFF7928CA.toInt(),
                            "暗夜黑" to 0xFF1F2329.toInt(),
                            "科技蓝" to 0xFF2A82E4.toInt(),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            brandColors.forEach { (name, colorValue) ->
                                val isSelectedColor = glyphPadColor == colorValue
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorValue))
                                        .clickable { glyphPadColor = colorValue },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelectedColor) {
                                        Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        // ── 基础信息 ──
        item(key = "basic_info_title") { SmallTitle("基础参数") }
        item(key = "basic_info_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        label = "快捷方式标题",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    TextField(
                        value = subtitle,
                        onValueChange = { subtitle = it },
                        label = "副标题 / 功能描述（可选）",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchPreference(
                    title = "在指纹快捷面板中启用",
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                )
            }
        }

        // ── 启动选项类型选择 ──
        item(key = "action_type_title") { SmallTitle("启动选项与执行动作") }
        item(key = "action_type_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                ArrowPreference(
                    title = "从预选动作库快速填充",
                    summary = "一键填充小爱、静音、勿扰、手电筒、相机等推荐模板",
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Tune,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    },
                    onClick = { showPresetPicker = true },
                )
                RadioButtonPreference(
                    title = "唤醒小爱 (XIAOAI)",
                    summary = "直接唤醒 HyperOS 小爱语音助手",
                    selected = actionType == ShortcutItem.ACTION_XIAOAI,
                    onClick = { actionType = ShortcutItem.ACTION_XIAOAI },
                )
                RadioButtonPreference(
                    title = "静音模式 (MUTE)",
                    summary = "快速切换系统全局静音与响铃模式",
                    selected = actionType == ShortcutItem.ACTION_MUTE,
                    onClick = { actionType = ShortcutItem.ACTION_MUTE },
                )
                RadioButtonPreference(
                    title = "勿扰模式 (DND)",
                    summary = "快速开启或退出静音防打扰模式",
                    selected = actionType == ShortcutItem.ACTION_DND,
                    onClick = { actionType = ShortcutItem.ACTION_DND },
                )
                RadioButtonPreference(
                    title = "开关手电筒 (FLASHLIGHT)",
                    summary = "直接切换手机后置 LED 手电筒",
                    selected = actionType == ShortcutItem.ACTION_FLASHLIGHT,
                    onClick = { actionType = ShortcutItem.ACTION_FLASHLIGHT },
                )
                RadioButtonPreference(
                    title = "街拍相机 (CAMERA)",
                    summary = "极速启动原生系统相机抓拍",
                    selected = actionType == ShortcutItem.ACTION_CAMERA,
                    onClick = { actionType = ShortcutItem.ACTION_CAMERA },
                )
                RadioButtonPreference(
                    title = "打开应用 (APP)",
                    summary = "启动手机安装的目标应用程序",
                    selected = actionType == ShortcutItem.ACTION_APP,
                    onClick = { actionType = ShortcutItem.ACTION_APP },
                )
                RadioButtonPreference(
                    title = "深度链接 / Intent (INTENT_URI)",
                    summary = "调起扫一扫、付款码等特定 Intent URI",
                    selected = actionType == ShortcutItem.ACTION_INTENT_URI,
                    onClick = { actionType = ShortcutItem.ACTION_INTENT_URI },
                )
                RadioButtonPreference(
                    title = "Shell 指令 (SHELL)",
                    summary = "执行终端 Shell 脚本命令",
                    selected = actionType == ShortcutItem.ACTION_SHELL,
                    onClick = { actionType = ShortcutItem.ACTION_SHELL },
                )
                RadioButtonPreference(
                    title = "发送广播 (BROADCAST)",
                    summary = "向系统分发广播 Action",
                    selected = actionType == ShortcutItem.ACTION_BROADCAST,
                    onClick = { actionType = ShortcutItem.ACTION_BROADCAST },
                )
            }
        }

        // ── 动作参数详情填写 ──
        val isNoPayloadAction = actionType == ShortcutItem.ACTION_FLASHLIGHT ||
            actionType == ShortcutItem.ACTION_XIAOAI ||
            actionType == ShortcutItem.ACTION_MUTE ||
            actionType == ShortcutItem.ACTION_DND ||
            actionType == ShortcutItem.ACTION_CAMERA

        if (!isNoPayloadAction) {
            item(key = "payload_title") { SmallTitle("动作参数详情 (Payload)") }
            item(key = "payload_card") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                ) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Column {
                            TextField(
                                value = actionPayload,
                                onValueChange = { actionPayload = it },
                                label = when (actionType) {
                                    ShortcutItem.ACTION_APP -> "应用包名（例如 com.tencent.mm）"
                                    ShortcutItem.ACTION_INTENT_URI -> "Intent URI 字符串"
                                    ShortcutItem.ACTION_SHELL -> "Shell 脚本命令"
                                    ShortcutItem.ACTION_BROADCAST -> "广播 Action"
                                    else -> "参数详情"
                                },
                                singleLine = actionType == ShortcutItem.ACTION_APP || actionType == ShortcutItem.ACTION_BROADCAST,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (actionType == ShortcutItem.ACTION_APP) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    text = "从已安装应用选取",
                                    onClick = {
                                        showAppPicker = true
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 保存与删除按钮 ──
        item(key = "confirm_buttons") {
            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        text = if (isNew) "确认添加" else "保存修改",
                        onClick = {
                            val newItem = initialItem.copy()
                            newItem.title = title.trim().ifBlank { "快捷方式" }
                            newItem.subtitle = subtitle.trim()
                            newItem.actionType = actionType
                            newItem.actionPayload = actionPayload.trim()
                            newItem.iconPackage = iconPackage.trim()
                            newItem.iconGlyph = iconGlyph.trim()
                            newItem.glyphPadEnabled = glyphPadEnabled
                            newItem.glyphPadColor = glyphPadColor
                            newItem.glyphPadMode = glyphPadMode
                            newItem.enabled = enabled
                            onSave(newItem)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                    if (!isNew) {
                        TextButton(
                            text = "删除此快捷方式",
                            onClick = {
                                onDelete(initialItem)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }

    // ── 应用选择弹窗 ──
    if (showAppPicker) {
        AppListPickerDialog(
            onDismiss = { showAppPicker = false },
            onSelectApp = { pkg, appName ->
                if (actionType == ShortcutItem.ACTION_APP && actionPayload.isBlank()) {
                    actionPayload = pkg
                }
                if (title.isBlank() || title == "新快捷方式") {
                    title = appName
                }
                iconPackage = pkg
                iconGlyph = ""
                showAppPicker = false
            },
        )
    }

    // ── Miuix 图标选择弹窗 ──
    if (showMiuixIconPicker) {
        MiuixIconPickerDialog(
            currentIconWithWeight = iconGlyph,
            onDismiss = { showMiuixIconPicker = false },
            onSelectIcon = { glyphKey ->
                iconGlyph = glyphKey
                iconPackage = ""
                showMiuixIconPicker = false
            },
        )
    }

    // ── 预选动作列表弹窗 ──
    if (showPresetPicker) {
        PresetActionPickerDialog(
            onDismiss = { showPresetPicker = false },
            onSelect = { preset ->
                title = preset.title
                subtitle = preset.subtitle
                actionType = preset.actionType
                actionPayload = preset.actionPayload
                iconPackage = preset.iconPackage
                iconGlyph = preset.iconGlyph
                glyphPadEnabled = preset.glyphPadEnabled
                glyphPadColor = preset.glyphPadColor
                glyphPadMode = preset.glyphPadMode
                showPresetPicker = false
            },
        )
    }

    // ── 退出时未保存确认弹窗（MIUIX 质感弹窗） ──
    if (showUnsavedDialog) {
        Dialog(
            onDismissRequest = { showUnsavedDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .padding(vertical = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Text(
                        text = "保存修改？",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (isNew) "您刚刚创建的快捷方式尚未保存，是否保存并生效？" else "当前快捷方式的参数已发生更改，退出前是否保存？",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            text = "放弃",
                            onClick = {
                                showUnsavedDialog = false
                                onExitWithoutSave()
                            },
                        )
                        Spacer(Modifier.width(10.dp))
                        TextButton(
                            text = "保存并退出",
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                            onClick = {
                                showUnsavedDialog = false
                                val newItem = initialItem.copy()
                                newItem.title = title.trim().ifBlank { "快捷方式" }
                                newItem.subtitle = subtitle.trim()
                                newItem.actionType = actionType
                                newItem.actionPayload = actionPayload.trim()
                                newItem.iconPackage = iconPackage.trim()
                                newItem.iconGlyph = iconGlyph.trim()
                                newItem.glyphPadEnabled = glyphPadEnabled
                                newItem.glyphPadColor = glyphPadColor
                                newItem.glyphPadMode = glyphPadMode
                                newItem.enabled = enabled
                                onSave(newItem)
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 预选动作列表项数据与弹窗
// ─────────────────────────────────────────────────────────────────────

data class PresetAction(
    val title: String,
    val subtitle: String,
    val actionType: String,
    val actionPayload: String = "",
    val iconGlyph: String = "",
    val iconPackage: String = "",
    val tintColor: Int = 0xFF2A82E4.toInt(),
    val glyphPadEnabled: Boolean = true,
    val glyphPadColor: Int = 0xFF2A82E4.toInt(),
    val glyphPadMode: Int = 1,
    val tag: String = "",
)

val PRESET_ACTIONS = listOf(
    PresetAction(
        title = "唤醒小爱",
        subtitle = "一键唤醒语音助手与小爱同学",
        actionType = ShortcutItem.ACTION_XIAOAI,
        iconGlyph = "Mic:Regular",
        tintColor = 0xFF1485EE.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFF1485EE.toInt(),
        glyphPadMode = 1,
        tag = "系统助手",
    ),
    PresetAction(
        title = "静音模式",
        subtitle = "快速切换响铃与静音震动",
        actionType = ShortcutItem.ACTION_MUTE,
        iconGlyph = "VolumeOff:Regular",
        tintColor = 0xFFFA8C16.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFFFA8C16.toInt(),
        glyphPadMode = 1,
        tag = "系统开关",
    ),
    PresetAction(
        title = "勿扰模式",
        subtitle = "快速开启或退出静音勿扰模式",
        actionType = ShortcutItem.ACTION_DND,
        iconGlyph = "Blocklist:Regular",
        tintColor = 0xFF722ED1.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFF722ED1.toInt(),
        glyphPadMode = 1,
        tag = "系统开关",
    ),
    PresetAction(
        title = "强光手电筒",
        subtitle = "开关手机后置 LED 强光照明",
        actionType = ShortcutItem.ACTION_FLASHLIGHT,
        iconGlyph = "Flashlight:Regular",
        tintColor = 0xFFFAAD14.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFFFAAD14.toInt(),
        glyphPadMode = 1,
        tag = "常用工具",
    ),
    PresetAction(
        title = "街拍相机",
        subtitle = "秒级直达系统原生相机抓拍界面",
        actionType = ShortcutItem.ACTION_CAMERA,
        iconGlyph = "Camera:Regular",
        tintColor = 0xFFFF4D4F.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFFFF4D4F.toInt(),
        glyphPadMode = 1,
        tag = "系统相机",
    ),
    PresetAction(
        title = "微信扫一扫",
        subtitle = "直接唤起微信官方扫码镜头",
        actionType = ShortcutItem.ACTION_INTENT_URI,
        actionPayload = "intent://dl/scan#Intent;scheme=weixin;package=com.tencent.mm;end",
        iconGlyph = "Scan:Regular",
        tintColor = 0xFF07C160.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFF07C160.toInt(),
        glyphPadMode = 1,
        tag = "快捷扫码",
    ),
    PresetAction(
        title = "支付宝付款码",
        subtitle = "快速出示收银台向商家付款",
        actionType = ShortcutItem.ACTION_INTENT_URI,
        actionPayload = "intent://platformapi/startapp?appId=20000056#Intent;scheme=alipays;package=com.eg.android.AlipayGphone;end",
        iconGlyph = "BankCards:Regular",
        tintColor = 0xFF1677FF.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFF1677FF.toInt(),
        glyphPadMode = 1,
        tag = "移动支付",
    ),
    PresetAction(
        title = "支付宝扫一扫",
        subtitle = "快速识别二维码或扫码骑行",
        actionType = ShortcutItem.ACTION_INTENT_URI,
        actionPayload = "intent://platformapi/startapp?appId=10000007#Intent;scheme=alipays;package=com.eg.android.AlipayGphone;end",
        iconGlyph = "Scan:Regular",
        tintColor = 0xFF1677FF.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFF1677FF.toInt(),
        glyphPadMode = 1,
        tag = "移动支付",
    ),
    PresetAction(
        title = "乘车码",
        subtitle = "地铁与公交交通一卡通直接刷码",
        actionType = ShortcutItem.ACTION_INTENT_URI,
        actionPayload = "intent://platformapi/startapp?appId=200011235#Intent;scheme=alipays;package=com.eg.android.AlipayGphone;end",
        iconGlyph = "QrCode:Regular",
        tintColor = 0xFF52C41A.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFF52C41A.toInt(),
        glyphPadMode = 1,
        tag = "绿色出行",
    ),
    PresetAction(
        title = "自定义快捷方式",
        subtitle = "自由配置目标应用、自定义 Intent 或 Shell",
        actionType = ShortcutItem.ACTION_APP,
        iconGlyph = "sparkle",
        tintColor = 0xFF3482FF.toInt(),
        glyphPadEnabled = true,
        glyphPadColor = 0xFF3482FF.toInt(),
        glyphPadMode = 1,
        tag = "自定义",
    ),
)

@Composable
fun PresetActionPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (PresetAction) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "新建快捷方式",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "提供丰富预选常用动作，轻触即可自动填充配置",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(PRESET_ACTIONS) { action ->
                        val vector = remember(action.iconGlyph) {
                            MiuixIconRegistry.find(action.iconGlyph)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MiuixTheme.colorScheme.surfaceVariant)
                                .clickable { onSelect(action) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(action.glyphPadColor).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (vector != null) {
                                    Icon(
                                        imageVector = vector,
                                        contentDescription = null,
                                        tint = Color(action.tintColor),
                                        modifier = Modifier.size(20.dp),
                                    )
                                } else {
                                    Text("⚡", fontSize = 16.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = action.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                    if (action.tag.isNotBlank()) {
                                        Spacer(Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp),
                                        ) {
                                            Text(
                                                text = action.tag,
                                                fontSize = 10.sp,
                                                color = MiuixTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = action.subtitle,
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                "选用",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        text = "取消",
                        onClick = onDismiss,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 应用选择弹窗组件
// ─────────────────────────────────────────────────────────────────────

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    val icon: android.graphics.drawable.Drawable?,
)

@Composable
fun AppListPickerDialog(
    onDismiss: () -> Unit,
    onSelectApp: (pkg: String, name: String) -> Unit,
) {
    val ctx = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableIntStateOf(0) } // 0=全部, 1=用户应用, 2=系统应用
    var allApps by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            val list = installedPackages.mapNotNull { pi ->
                val appInfo = pi.applicationInfo ?: return@mapNotNull null
                val pkg = pi.packageName
                val name = appInfo.loadLabel(pm).toString().ifBlank { pkg }
                val isSys = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val icon = try {
                    appInfo.loadIcon(pm)
                } catch (t: Throwable) {
                    null
                }
                AppInfoItem(pkg, name, isSys, icon)
            }.distinctBy { it.packageName }
                .sortedWith(compareBy({ it.appName.lowercase() }, { it.packageName }))
            withContext(Dispatchers.Main) {
                allApps = list
                isLoading = false
            }
        }
    }

    val filteredApps = remember(searchQuery, filterTab, allApps) {
        var base = when (filterTab) {
            1 -> allApps.filter { !it.isSystem }
            2 -> allApps.filter { it.isSystem }
            else -> allApps
        }
        if (searchQuery.isNotBlank()) {
            base = base.filter {
                it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
        base
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "选择应用程序",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "选取应用作为快捷图标或启动目标",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TabRow(
                    tabs = listOf("全部 (${allApps.size})", "用户应用", "系统应用"),
                    selectedTabIndex = filterTab,
                    onTabSelected = { filterTab = it },
                    colors = TabRowDefaults.tabRowColors(backgroundColor = Color.Transparent),
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "搜索应用名称或包名...",
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("正在加载已安装应用...", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                    ) {
                        items(filteredApps.size, key = { filteredApps[it].packageName }) { idx ->
                            val app = filteredApps[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectApp(app.packageName, app.appName) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (app.icon != null) {
                                    val bmp = remember(app.packageName) {
                                        val b = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
                                        val c = android.graphics.Canvas(b)
                                        app.icon.setBounds(0, 0, 96, 96)
                                        app.icon.draw(c)
                                        b.toComposeImageBitmap()
                                    }
                                    Image(
                                        painter = BitmapPainter(bmp),
                                        contentDescription = app.appName,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MiuixTheme.colorScheme.surfaceVariant),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = app.appName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MiuixTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )
                                        if (app.isSystem) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "系统",
                                                fontSize = 10.sp,
                                                color = MiuixTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .background(
                                                        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        RoundedCornerShape(4.dp),
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = app.packageName,
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Miuix 矢量图标选择弹窗组件（完整 125+ 图标库 + 5 档粗细切换 + 搜索）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun MiuixIconPickerDialog(
    currentIconWithWeight: String,
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit,
) {
    val initialWeight = remember(currentIconWithWeight) {
        if (currentIconWithWeight.contains(":")) currentIconWithWeight.split(":")[1].trim() else "Regular"
    }
    val initialName = remember(currentIconWithWeight) {
        if (currentIconWithWeight.contains(":")) currentIconWithWeight.split(":")[0].trim() else currentIconWithWeight.trim()
    }

    var selectedWeight by remember { mutableStateOf(initialWeight) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredNames = remember(searchQuery) {
        if (searchQuery.isBlank()) MiuixIconRegistry.ICON_NAMES
        else MiuixIconRegistry.ICON_NAMES.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val weightIndex = remember(selectedWeight) {
        MiuixIconRegistry.WEIGHTS.indexOf(selectedWeight).coerceAtLeast(0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "选择 Miuix 矢量图标",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = "支持 125+ 完整官方矢量库，并可实时调节线条粗细",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                // 粗细字重切换器
                Text(
                    text = "图标粗细：$selectedWeight",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                TabRow(
                    tabs = MiuixIconRegistry.WEIGHTS,
                    selectedTabIndex = weightIndex,
                    onTabSelected = { selectedWeight = MiuixIconRegistry.WEIGHTS[it] },
                    colors = TabRowDefaults.tabRowColors(backgroundColor = Color.Transparent),
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "搜索图标名称（如 Scan, Search, Settings...）",
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredNames, key = { it }) { name ->
                        val isSelected = name.equals(initialName, ignoreCase = true)
                        val vector = remember(name, selectedWeight) {
                            MiuixIconRegistry.find(name, selectedWeight)
                        }

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MiuixTheme.colorScheme.surfaceVariant,
                                )
                                .clickable {
                                    val finalKey = "$name:$selectedWeight"
                                    onSelectIcon(finalKey)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (vector != null) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = name,
                                    tint = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Page1：软件设置（主题入口 / 安全模式 / 关于）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun SettingsPage(
    contentPadding: PaddingValues,
    safetyTick: Int,
    onSafetyRefresh: () -> Unit,
    onOpenThemePage: () -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 主题设置 ──
        item(key = "theme_title") { SmallTitle("主题") }
        item(key = "theme_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                ArrowPreference(
                    title = "主题设置",
                    summary = "深浅模式 · Monet 动态取色 · 悬浮底栏",
                    startAction = {
                        Icon(
                            imageVector = MiuixIcons.Theme,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    },
                    onClick = onOpenThemePage,
                )
            }
        }

        // ── 安全模式与崩溃保护 ──
        item(key = "safety_title") { SmallTitle("安全模式与崩溃保护") }
        item(key = "safety_card") {
            SafetyCard(tick = safetyTick, onRequestRefresh = onSafetyRefresh)
        }

        // ── 关于 ──
        item(key = "about_title") { SmallTitle("关于") }
        item(key = "about_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                AboutLine("模块名称", "HyperOS QuickOpen")
                AboutLine("当前版本", "v${com.hyperos.quickopen.BuildConfig.VERSION_NAME}")
                AboutLine("功能", "指纹快捷面板自定义 · 图标与动效增强")
                AboutLine("UI 框架", "Miuix Compose 0.9.4 (官方模板标准)")
                AboutLine("使用方式", "指纹解锁时按压指纹传感器唤出快捷面板")
                AboutLine("生效条件", "LSPosed 激活模块并勾选 SystemUI 作用域，重启 SystemUI")
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun AboutLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 13.sp,
            modifier = Modifier.size(width = 84.dp, height = 20.dp),
        )
        Text(
            value,
            color = MiuixTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun android.graphics.Bitmap.toComposeImageBitmap(): androidx.compose.ui.graphics.ImageBitmap = this.asImageBitmap()

// ─────────────────────────────────────────────────────────────────────
// 安全模式卡片
// ─────────────────────────────────────────────────────────────────────

@Composable
fun SafetyCard(tick: Int, onRequestRefresh: () -> Unit) {
    val ctx = LocalContext.current

    val state = remember(tick) { SafetyStore.getState(ctx) }
    val safeMode = state.getBoolean("safe_mode", false)
    val hookEnabled = state.getBoolean("hook_enabled", true)
    val crashCount = state.getInt("crash_count", 0)
    val threshold = state.getInt("threshold", SafetyStore.CRASH_THRESHOLD)
    val total = state.getInt("total_crash", 0)
    val lastTs = state.getLong("last_crash_ts", 0L)
    val sessionHealthy = state.getBoolean("session_healthy", true)
    val log = state.getString("crash_log", "") ?: ""

    val statusText = when {
        safeMode -> "安全模式已激活 · Hook 已停用"
        !hookEnabled -> "Hook 总开关已关闭 · 不安装任何 Hook"
        else -> "运行正常 · Hook 已启用"
    }
    val statusColor = when {
        safeMode -> Color(0xFFE94634)
        !hookEnabled -> MiuixTheme.colorScheme.onSurfaceVariantSummary
        else -> Color(0xFF07C160)
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(statusText, color = statusColor, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            InfoLine("连续崩溃计数：$crashCount / $threshold")
            InfoLine("历史累计崩溃：$total 次")
            InfoLine("上次会话存活确认：${if (sessionHealthy) "已确认" else "未确认（疑似崩溃）"}")
            InfoLine(
                if (lastTs > 0L) {
                    val fmt = java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastTs))
                    "最近一次崩溃：$fmt"
                } else "最近一次崩溃：无",
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "崩溃记录",
                color = MiuixTheme.colorScheme.onSurface,
                fontSize = 13.sp,
            )
            Text(
                if (log.isBlank()) "暂无崩溃记录。" else log,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .basicMarquee(iterations = Int.MAX_VALUE),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    text = "退出安全模式",
                    onClick = {
                        SafetyStore.exitSafeMode(ctx)
                        Toast.makeText(ctx, "已退出安全模式，重启 SystemUI 后 Hook 恢复生效", Toast.LENGTH_LONG).show()
                        onRequestRefresh()
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "清空崩溃记录",
                    onClick = {
                        SafetyStore.clearCrash(ctx)
                        Toast.makeText(ctx, "崩溃记录已清空", Toast.LENGTH_SHORT).show()
                        onRequestRefresh()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            SwitchPreference(
                title = "启用指纹快捷方式增强 (Hook 总开关)",
                checked = hookEnabled && !safeMode,
                onCheckedChange = { checked ->
                    SafetyStore.setHookEnabled(ctx, checked)
                    Toast.makeText(
                        ctx,
                        if (checked) "Hook 已启用，重启 SystemUI 后生效" else "Hook 已停用，重启 SystemUI 后生效",
                        Toast.LENGTH_LONG,
                    ).show()
                    onRequestRefresh()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "当 Hook 导致 SystemUI 在 5 分钟内连续崩溃 2 次时，模块会自动进入安全模式并停用全部 Hook，SystemUI 恢复原生指纹界面。修复配置后点「退出安全模式」即可重新启用。",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
fun InfoLine(text: String) {
    Text(
        text,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 2.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────
// 二级页：主题设置（深浅模式 / Monet / 悬浮底栏）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ThemeSettingsPage(
    contentPadding: PaddingValues,
    themeMode: Int, onThemeMode: (Int) -> Unit,
    monet: Boolean, onMonet: (Boolean) -> Unit,
    floatingNavBar: Boolean, onFloatingNavBar: (Boolean) -> Unit,
) {
    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .overScrollVertical()
            .scrollEndHaptic(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 深浅模式选择器 ──
        item(key = "theme_mode_title") { SmallTitle("深浅模式") }
        item(key = "theme_mode_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                ArrowPreference(
                    title = "跟随系统",
                    summary = "随系统深色模式设置自适应切换",
                    endActions = {
                        if (themeMode == ThemeMode.FOLLOW_SYSTEM) {
                            Text("✓", color = MiuixTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    },
                    onClick = { onThemeMode(ThemeMode.FOLLOW_SYSTEM) },
                )
                ArrowPreference(
                    title = "浅色模式",
                    summary = "始终使用明亮色彩",
                    endActions = {
                        if (themeMode == ThemeMode.LIGHT) {
                            Text("✓", color = MiuixTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    },
                    onClick = { onThemeMode(ThemeMode.LIGHT) },
                )
                ArrowPreference(
                    title = "深色模式",
                    summary = "始终使用暗色护眼色彩",
                    endActions = {
                        if (themeMode == ThemeMode.DARK) {
                            Text("✓", color = MiuixTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    },
                    onClick = { onThemeMode(ThemeMode.DARK) },
                )
            }
        }

        // ── Monet 动态取色 ──
        item(key = "monet_title") { SmallTitle("色彩风格") }
        item(key = "monet_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                SwitchPreference(
                    title = "启用 Monet 动态取色",
                    summary = "吸取系统壁纸主题色，与系统界面深度融合",
                    checked = monet,
                    onCheckedChange = onMonet,
                )
            }
        }

        // ── 外观效果 ──
        item(key = "appearance_title") { SmallTitle("导航栏样式") }
        item(key = "appearance_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                SwitchPreference(
                    title = "悬浮底栏",
                    summary = "Apple / HyperOS 风格胶囊浮岛底栏",
                    checked = floatingNavBar,
                    onCheckedChange = onFloatingNavBar,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
