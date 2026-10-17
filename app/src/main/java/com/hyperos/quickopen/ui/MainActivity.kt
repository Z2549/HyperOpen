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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.Photos
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Theme
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.window.WindowDialog
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
    var floatingNavBar by remember { mutableStateOf(initialConfig.floatingNavBar) }
    var predictiveBack by remember { mutableStateOf(initialConfig.predictiveBack) }
    val shortcuts = remember { mutableStateListOf<ShortcutItem>().apply { addAll(initialConfig.shortcuts) } }
    var safetyTick by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }

    // 导航路由：0=主界面, 1=主题设置页, 2=快捷方式编辑页
    var subPage by remember { mutableStateOf(0) }
    var editingItem by remember { mutableStateOf<ShortcutItem?>(null) }
    var isNewShortcut by remember { mutableStateOf(false) }

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
        c.floatingNavBar = floatingNavBar
        c.predictiveBack = predictiveBack
        c.shortcuts = ArrayList(shortcuts)
        c.save(ctx)
    }

    // 拦截系统返回键与手势返回：在二级页时优先返回主页
    LaunchedEffect(activity, subPage) {
        activity?.backPressConsumer = {
            if (subPage != 0) {
                subPage = 0
                editingItem = null
                true
            } else {
                false
            }
        }
    }
    BackHandler(enabled = subPage != 0) {
        subPage = 0
        editingItem = null
    }

    HyperOSQuickOpenTheme(
        themeMode = themeMode,
        monet = monetColors,
    ) {
        val topAppBarScrollBehavior = MiuixScrollBehavior()
        val title = when (subPage) {
            1 -> "主题设置"
            2 -> if (isNewShortcut) "新建快捷方式" else "快捷方式参数设置"
            else -> if (page == 0) "参数配置" else "软件设置"
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
                            BackNavigationIcon(onClick = {
                                subPage = 0
                                editingItem = null
                            })
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
                when (subPage) {
                    1 -> {
                        ThemeSettingsPage(
                            contentPadding = innerPadding,
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
                                contentPadding = innerPadding,
                                initialItem = currentEdit,
                                isNew = isNewShortcut,
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
                            )
                        }
                    }
                    else -> {
                        if (page == 0) {
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
                                onShortcutToggle = { item, enabled ->
                                    val i = shortcuts.indexOfFirst { it.id == item.id }
                                    if (i >= 0) {
                                        val updated = item.copy().apply { this.enabled = enabled }
                                        shortcuts[i] = updated
                                    }
                                    persist()
                                },
                                onShortcutClick = { item ->
                                    editingItem = item.copy()
                                    isNewShortcut = false
                                    subPage = 2
                                },
                                onAddShortcut = {
                                    editingItem = ShortcutItem(
                                        UUID.randomUUID().toString(),
                                        "新快捷方式",
                                        "轻触打开",
                                        ShortcutItem.ACTION_APP,
                                        "",
                                        "",
                                        "sparkle",
                                        0xFF3482FF.toInt(),
                                        true,
                                    )
                                    isNewShortcut = true
                                    subPage = 2
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
    shortcuts: SnapshotStateList<ShortcutItem>,
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
                    onClick = { onShortcutClick(item) },
                )
            }
        }

        // ── 新建与重置操作 ──
        item(key = "actions_card") {
            Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
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

        item(key = "footer_tips") {
            Text(
                "提示：轻触快捷卡片可编辑参数（修改图标、名称、启动选项等），右侧开关控制是否在面板显示。",
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

    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = item.title,
            summary = item.subtitle.ifBlank { "点击编辑快捷方式参数" },
            startAction = {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (miuixIcon != null) {
                        Icon(
                            imageVector = miuixIcon,
                            contentDescription = item.title,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
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
    onSave: (ShortcutItem) -> Unit,
    onDelete: (ShortcutItem) -> Unit,
) {
    val ctx = LocalContext.current
    val lazyListState = rememberLazyListState()

    var title by remember { mutableStateOf(initialItem.title) }
    var subtitle by remember { mutableStateOf(initialItem.subtitle) }
    var actionType by remember { mutableStateOf(initialItem.actionType) }
    var actionPayload by remember { mutableStateOf(initialItem.actionPayload) }
    var iconPackage by remember { mutableStateOf(initialItem.iconPackage) }
    var iconGlyph by remember { mutableStateOf(initialItem.iconGlyph) }
    var enabled by remember { mutableStateOf(initialItem.enabled) }

    var showAppPicker by remember { mutableStateOf(false) }
    var showMiuixIconPicker by remember { mutableStateOf(false) }

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
                val bmp = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                d.setBounds(0, 0, 96, 96)
                d.draw(canvas)
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
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MiuixTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (miuixVector != null) {
                                Icon(
                                    imageVector = miuixVector,
                                    contentDescription = title,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(38.dp),
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
                    title = "开关手电筒 (FLASHLIGHT)",
                    summary = "直接切换手机手电筒开关",
                    selected = actionType == ShortcutItem.ACTION_FLASHLIGHT,
                    onClick = { actionType = ShortcutItem.ACTION_FLASHLIGHT },
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
        if (actionType != ShortcutItem.ACTION_FLASHLIGHT) {
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
            currentIcon = iconGlyph,
            onDismiss = { showMiuixIconPicker = false },
            onSelectIcon = { glyphKey ->
                iconGlyph = glyphKey
                iconPackage = ""
                showMiuixIconPicker = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// 应用选择弹窗组件
// ─────────────────────────────────────────────────────────────────────

data class AppInfoItem(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?,
)

@Composable
fun AppListPickerDialog(
    onDismiss: () -> Unit,
    onSelectApp: (pkg: String, name: String) -> Unit,
) {
    val ctx = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<AppInfoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val list = resolveInfos.mapNotNull { ri ->
                val pkg = ri.activityInfo.packageName
                val name = ri.loadLabel(pm).toString()
                val icon = try {
                    ri.loadIcon(pm)
                } catch (t: Throwable) {
                    null
                }
                AppInfoItem(pkg, name, icon)
            }.distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
            withContext(Dispatchers.Main) {
                allApps = list
                isLoading = false
            }
        }
    }

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    WindowDialog(
        show = true,
        title = "选择应用程序",
        summary = "选取应用作为快捷图标或启动目标",
        onDismissRequest = onDismiss,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                    Text(
                                        text = app.appName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
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
        },
    )
}

// ─────────────────────────────────────────────────────────────────────
// Miuix 矢量图标选择弹窗组件
// ─────────────────────────────────────────────────────────────────────

@Composable
fun MiuixIconPickerDialog(
    currentIcon: String,
    onDismiss: () -> Unit,
    onSelectIcon: (String) -> Unit,
) {
    val icons = MiuixIconRegistry.ICONS

    WindowDialog(
        show = true,
        title = "选择 Miuix 图标",
        summary = "选择矢量图标作为快捷方式图标",
        onDismissRequest = onDismiss,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(icons) { (name, vector) ->
                        val isSelected = name.equals(currentIcon, ignoreCase = true)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MiuixTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { onSelectIcon(name) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = vector,
                                contentDescription = name,
                                tint = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp),
                            )
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
        },
    )
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
