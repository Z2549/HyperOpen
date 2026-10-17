package com.hyperos.quickopen.ui

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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperos.quickopen.config.QuickOpenConfig
import com.hyperos.quickopen.model.ShortcutItem
import com.hyperos.quickopen.safety.SafetyStore
import com.hyperos.quickopen.view.IconGlyphFactory
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * Compose 设置界面：全面采用 miuix 官方 example 架构模板重构。
 * 遵循 Scaffold + TopAppBar + NavigationBar / FloatingNavigationBar 官方规范。
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
    var materialStyle by remember { mutableStateOf(initialConfig.materialStyle) }
    var offsetY by remember { mutableFloatStateOf(initialConfig.offsetY.toFloat()) }
    var regionDiameter by remember { mutableFloatStateOf(initialConfig.regionDiameter.toFloat()) }
    var showRegionBg by remember { mutableStateOf(initialConfig.showRegionBg) }
    var showHeaderPillBg by remember { mutableStateOf(initialConfig.showHeaderPillBg) }
    var iconSize by remember { mutableFloatStateOf(initialConfig.iconSize.toFloat()) }
    var maxScale by remember { mutableFloatStateOf(initialConfig.maxScale) }
    var influenceRadius by remember { mutableFloatStateOf(initialConfig.influenceRadius.toFloat()) }
    var themeMode by remember { mutableStateOf(initialConfig.themeMode) }
    var monetColors by remember { mutableStateOf(initialConfig.monetColors) }
    var barBlur by remember { mutableStateOf(initialConfig.barBlur) }
    var liquidGlass by remember { mutableStateOf(initialConfig.liquidGlass) }
    var floatingNavBar by remember { mutableStateOf(initialConfig.floatingNavBar) }
    var predictiveBack by remember { mutableStateOf(initialConfig.predictiveBack) }
    val shortcuts = remember { mutableStateListOf<ShortcutItem>().apply { addAll(initialConfig.shortcuts) } }
    var safetyTick by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }
    var subPage by remember { mutableStateOf(0) }

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
        c.themeMode = themeMode
        c.monetColors = monetColors
        c.barBlur = barBlur
        c.liquidGlass = liquidGlass
        c.floatingNavBar = floatingNavBar
        c.predictiveBack = predictiveBack
        c.shortcuts = ArrayList(shortcuts)
        c.save(ctx)
    }

    // 拦截系统返回键与手势返回：在二级页时优先返回软件主页
    LaunchedEffect(activity, subPage) {
        activity?.backPressConsumer = {
            if (subPage != 0) {
                subPage = 0
                true
            } else {
                false
            }
        }
    }
    BackHandler(enabled = subPage != 0) {
        subPage = 0
    }

    HyperOSQuickOpenTheme(
        themeMode = themeMode,
        monet = monetColors,
    ) {
        val topAppBarScrollBehavior = MiuixScrollBehavior()
        val title = when {
            subPage == 1 -> "主题设置"
            page == 0 -> "参数配置"
            else -> "软件设置"
        }

        val navItems = remember {
            listOf(
                NavigationItem("参数配置", MiuixIcons.Tune),
                NavigationItem("软件设置", MiuixIcons.Settings),
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = title,
                    scrollBehavior = topAppBarScrollBehavior,
                    navigationIcon = {
                        if (subPage != 0) {
                            BackNavigationIcon(onClick = { subPage = 0 })
                        }
                    },
                )
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
                        liquidGlass = liquidGlass,
                        onPageSelect = { page = it },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            ) {
                if (subPage == 1) {
                    ThemeSettingsPage(
                        contentPadding = innerPadding,
                        themeMode = themeMode,
                        onThemeMode = { themeMode = it; persist() },
                        monet = monetColors,
                        onMonet = { monetColors = it; persist() },
                        liquidGlass = liquidGlass,
                        onLiquidGlass = { liquidGlass = it; persist() },
                        floatingNavBar = floatingNavBar,
                        onFloatingNavBar = { floatingNavBar = it; persist() },
                    )
                } else if (page == 0) {
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
                        shortcuts = shortcuts,
                        onShortcutToggle = { item, enabled -> item.enabled = enabled; persist() },
                        onShortcutEdit = { updated ->
                            val i = shortcuts.indexOfFirst { it.id == updated.id }
                            if (i >= 0) shortcuts[i] = updated
                            persist()
                        },
                        onResetShortcuts = {
                            shortcuts.clear()
                            shortcuts.addAll(QuickOpenConfig.getDefaultShortcuts())
                            persist()
                            Toast.makeText(ctx, "已恢复默认快捷项目", Toast.LENGTH_SHORT).show()
                        },
                    )
                } else {
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
    liquidGlass: Boolean,
    onPageSelect: (Int) -> Unit,
) {
    if (floating) {
        val barColor = if (liquidGlass) {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        }
        FloatingNavigationBar(
            color = barColor,
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
        val barColor = if (liquidGlass) {
            MiuixTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            MiuixTheme.colorScheme.surface
        }
        NavigationBar(
            color = barColor,
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
    shortcuts: SnapshotStateList<ShortcutItem>,
    onShortcutToggle: (ShortcutItem, Boolean) -> Unit,
    onShortcutEdit: (ShortcutItem) -> Unit,
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
        // ── 材质与系统风格 ──
        item(key = "material_title") { SmallTitle("材质与系统风格") }
        item(key = "material_card") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                ArrowPreference(
                    title = "跟随系统",
                    summary = "System Auto",
                    endActions = {
                        if (materialStyle == QuickOpenConfig.MATERIAL_FOLLOW_SYSTEM) {
                            Text("✓", color = MiuixTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    },
                    onClick = { onMaterialStyle(QuickOpenConfig.MATERIAL_FOLLOW_SYSTEM) },
                )
                ArrowPreference(
                    title = "柔光玻璃",
                    summary = "Soft Light Glass",
                    endActions = {
                        if (materialStyle == QuickOpenConfig.MATERIAL_SOFT_GLASS) {
                            Text("✓", color = MiuixTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    },
                    onClick = { onMaterialStyle(QuickOpenConfig.MATERIAL_SOFT_GLASS) },
                )
                ArrowPreference(
                    title = "高斯模糊",
                    summary = "Gaussian Blur",
                    endActions = {
                        if (materialStyle == QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR) {
                            Text("✓", color = MiuixTheme.colorScheme.primary, fontSize = 16.sp)
                        }
                    },
                    onClick = { onMaterialStyle(QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR) },
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
            }
        }

        // ── 快捷项目列表 ──
        item(key = "shortcuts_title") { SmallTitle("快捷项目管理与动作排序") }
        itemsIndexed(shortcuts, key = { _, it -> it.id }) { _, item ->
            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                ShortcutRow(
                    item = item,
                    onToggle = { onShortcutToggle(item, it) },
                    onEdit = onShortcutEdit,
                )
            }
        }

        item(key = "reset_shortcuts") {
            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
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

        item(key = "footer_tips") {
            Text(
                "提示：所有修改即时保存，长按指纹即可验证效果，无需重启 SystemUI。",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
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
                    summary = "深浅模式 · Monet 动态取色 · 液态玻璃 · 悬浮底栏",
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

// ─────────────────────────────────────────────────────────────────────
// 快捷项列表行与自定义图标弹窗（采用官方 OverlayDialog）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ShortcutRow(
    item: ShortcutItem,
    onToggle: (Boolean) -> Unit,
    onEdit: (ShortcutItem) -> Unit,
) {
    val ctx = LocalContext.current
    var showEdit by remember { mutableStateOf(false) }
    val painter = remember(item.id, item.iconPackage, item.iconGlyph) { IconPainter.resolve(item, ctx) }

    Card(modifier = Modifier.fillMaxWidth()) {
        SwitchPreference(
            title = item.title,
            summary = item.subtitle,
            checked = item.enabled,
            startAction = {
                Image(
                    painter = painter,
                    contentDescription = item.title,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            },
            onCheckedChange = onToggle,
            modifier = Modifier
                .combinedClickable(
                    onClick = {},
                    onLongClick = { showEdit = true },
                )
                .fillMaxWidth(),
        )
    }

    if (showEdit) {
        IconEditOverlayDialog(
            item = item,
            onDismiss = { showEdit = false },
            onSave = {
                onEdit(it)
                showEdit = false
            },
        )
    }
}

@Composable
fun IconEditOverlayDialog(
    item: ShortcutItem,
    onDismiss: () -> Unit,
    onSave: (ShortcutItem) -> Unit,
) {
    val ctx = LocalContext.current
    var pkg by remember { mutableStateOf(item.iconPackage) }
    var glyph by remember { mutableStateOf(item.iconGlyph) }

    val livePreview = remember(pkg, glyph) {
        val d = when {
            pkg.isNotBlank() -> IconGlyphFactory.appIcon(ctx, pkg.trim()) ?: IconGlyphFactory.createGlyph("sparkle", item.tintColor)
            glyph.isNotBlank() -> IconGlyphFactory.createGlyph(glyph.trim().lowercase(), item.tintColor)
            else -> IconGlyphFactory.resolve(ctx, item)
        }
        val bmp = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        d.setBounds(0, 0, 96, 96)
        d.draw(canvas)
        BitmapPainter(bmp.toComposeImageBitmap())
    }

    OverlayDialog(
        show = true,
        title = "自定义图标",
        summary = item.title,
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = livePreview,
                        contentDescription = "preview",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
                TextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = "图标来源包名（如 com.tencent.mm）",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = glyph,
                    onValueChange = { glyph = it },
                    label = "内置符号 key（${IconGlyphFactory.GLYPH_KEYS.joinToString("/")}）",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        text = "取消",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        text = "保存",
                        onClick = {
                            item.iconPackage = pkg.trim()
                            item.iconGlyph = glyph.trim()
                            onSave(item)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        },
    )
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
// 二级页：主题设置（深浅模式 / Monet / 底栏液态玻璃 / 悬浮底栏）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ThemeSettingsPage(
    contentPadding: PaddingValues,
    themeMode: Int, onThemeMode: (Int) -> Unit,
    monet: Boolean, onMonet: (Boolean) -> Unit,
    liquidGlass: Boolean, onLiquidGlass: (Boolean) -> Unit,
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
        item(key = "appearance_title") { SmallTitle("外观与导航栏效果") }
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
                SwitchPreference(
                    title = "底栏液态玻璃",
                    summary = "底栏采用半透明磨砂质感",
                    checked = liquidGlass,
                    onCheckedChange = onLiquidGlass,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
