package com.hyperos.quickopen.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperos.quickopen.config.QuickOpenConfig
import com.hyperos.quickopen.model.ShortcutItem
import com.hyperos.quickopen.safety.SafetyStore
import com.hyperos.quickopen.view.IconGlyphFactory
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.basic.TextField

/**
 * Compose 重构版设置主页（MiX 材质 / MIUIX 双风格由 [HyperOSQuickOpenTheme] 承载）。
 * 底栏双页：Page0 参数配置（材质/几何/图标物理/快捷项）、Page1 软件设置（界面风格/安全模式/关于）。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = QuickOpenConfig.loadFromApp(this)
        val style = if (config.uiStyle == QuickOpenConfig.UI_STYLE_MIUIX) UiStyle.MIUIX else UiStyle.MATERIAL

        setContent {
            HyperOSQuickOpenTheme(uiStyle = style) {
                QuickOpenApp(
                    initialConfig = config,
                    onStyleChange = { newStyle ->
                        val newUi = if (newStyle == UiStyle.MIUIX) QuickOpenConfig.UI_STYLE_MIUIX else QuickOpenConfig.UI_STYLE_MATERIAL
                        config.uiStyle = newUi
                        config.save(this)
                        recreate()
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// App 主结构
// ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickOpenApp(initialConfig: QuickOpenConfig, onStyleChange: (UiStyle) -> Unit) {
    val ctx = LocalContext.current
    val semantic = currentSemantic()

    // 配置镜像状态（单一大对象 + 手动触发重组）
    var materialStyle by remember { mutableStateOf(initialConfig.materialStyle) }
    var offsetY by remember { mutableStateOf(initialConfig.offsetY.toFloat()) }
    var regionDiameter by remember { mutableStateOf(initialConfig.regionDiameter.toFloat()) }
    var showRegionBg by remember { mutableStateOf(initialConfig.showRegionBg) }
    var showHeaderPillBg by remember { mutableStateOf(initialConfig.showHeaderPillBg) }
    var iconSize by remember { mutableStateOf(initialConfig.iconSize.toFloat()) }
    var maxScale by remember { mutableStateOf(initialConfig.maxScale) }
    var influenceRadius by remember { mutableStateOf(initialConfig.influenceRadius.toFloat()) }
    var uiStyle by remember { mutableStateOf(if (initialConfig.uiStyle == QuickOpenConfig.UI_STYLE_MIUIX) UiStyle.MIUIX else UiStyle.MATERIAL) }
    val shortcuts = remember { mutableStateListOf<ShortcutItem>().apply { addAll(initialConfig.shortcuts) } }
    var safetyTick by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }

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
        c.uiStyle = if (uiStyle == UiStyle.MIUIX) QuickOpenConfig.UI_STYLE_MIUIX else QuickOpenConfig.UI_STYLE_MATERIAL
        c.shortcuts = ArrayList(shortcuts)
        c.save(ctx)
    }

    androidx.compose.material3.Scaffold(
        containerColor = semantic.background,
        topBar = {
            TopAppBar(
                title = { androidx.compose.material3.Text("指纹快捷方式增强", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = semantic.background,
                    titleContentColor = semantic.fgPrimary,
                ),
            )
        },
        bottomBar = { BottomNavBar(page) { page = it } },
    ) { padding ->
        if (page == 0) {
            ParamsPage(
                padding = padding,
                semantic = semantic,
                materialStyle = materialStyle, onMaterialStyle = { materialStyle = it; persist() },
                offsetY = offsetY, onOffsetY = { offsetY = it; persist() },
                regionDiameter = regionDiameter, onRegionDiameter = { regionDiameter = it; persist() },
                showRegionBg = showRegionBg, onShowRegionBg = { showRegionBg = it; persist() },
                showHeaderPillBg = showHeaderPillBg, onShowHeaderPillBg = { showHeaderPillBg = it; persist() },
                iconSize = iconSize, onIconSize = { iconSize = it; persist() },
                maxScale = maxScale, onMaxScale = { maxScale = it; persist() },
                influenceRadius = influenceRadius, onInfluenceRadius = { influenceRadius = it; persist() },
                shortcuts = shortcuts, onShortcutToggle = { item, enabled -> item.enabled = enabled; persist() },
                onShortcutEdit = { updated ->
                    val i = shortcuts.indexOfFirst { it.id == updated.id }
                    if (i >= 0) shortcuts[i] = updated
                    persist()
                },
                onResetShortcuts = {
                    shortcuts.clear(); shortcuts.addAll(QuickOpenConfig.getDefaultShortcuts()); persist()
                    Toast.makeText(ctx, "已恢复默认快捷项目", Toast.LENGTH_SHORT).show()
                },
            )
        } else {
            SettingsPage(
                padding = padding,
                semantic = semantic,
                uiStyle = uiStyle,
                onStyleChange = onStyleChange,
                safetyTick = safetyTick,
                onSafetyRefresh = { safetyTick++ },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 底部导航栏（Material 轨 NavigationBar / MIUIX 轨自绘 Tab）
// ─────────────────────────────────────────────────────────────────────

private val NAV_PAGES = listOf(
    Triple("参数配置", "Parameters", Icons.Filled.Build),
    Triple("软件设置", "Preferences", Icons.Filled.Settings),
)

@Composable
fun BottomNavBar(page: Int, onPageChange: (Int) -> Unit) {
    val semantic = currentSemantic()
    val style = LocalUiStyle.current
    if (style == UiStyle.MIUIX) {
        // miuix 没有 NavigationBar 组件 —— 自绘澎湃风格底部 Tab（Surface + 权重行 + 图标文字）
        Surface(
            color = semantic.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NAV_PAGES.forEachIndexed { i, (title, sub, icon) ->
                    val selected = page == i
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .combinedClickable(onClick = { onPageChange(i) }),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (selected) semantic.primary else semantic.fgSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        androidx.compose.material3.Text(
                            title,
                            color = if (selected) semantic.primary else semantic.fgSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    } else {
        NavigationBar(
            containerColor = semantic.surface,
            contentColor = semantic.fgPrimary,
        ) {
            NAV_PAGES.forEachIndexed { i, (title, sub, icon) ->
                NavigationBarItem(
                    selected = page == i,
                    onClick = { onPageChange(i) },
                    icon = { Icon(icon, contentDescription = title) },
                    label = { androidx.compose.material3.Text(title, fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = semantic.primary,
                        selectedTextColor = semantic.primary,
                        unselectedIconColor = semantic.fgSecondary,
                        unselectedTextColor = semantic.fgSecondary,
                        indicatorColor = semantic.primary.copy(alpha = 0.16f),
                    ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Page0：参数配置（材质 / 几何 / 图标物理 / 快捷项）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ParamsPage(
    padding: PaddingValues,
    semantic: SemanticPalette,
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 材质 ─────────────────────────────────────────────
        item { SmallTitle("材质与系统风格") }
        item {
            SettingsCard {
                MaterialRow("跟随系统", "System Auto", materialStyle == QuickOpenConfig.MATERIAL_FOLLOW_SYSTEM) { onMaterialStyle(QuickOpenConfig.MATERIAL_FOLLOW_SYSTEM) }
                DividerRow()
                MaterialRow("柔光玻璃", "Soft Light Glass", materialStyle == QuickOpenConfig.MATERIAL_SOFT_GLASS) { onMaterialStyle(QuickOpenConfig.MATERIAL_SOFT_GLASS) }
                DividerRow()
                MaterialRow("高斯模糊", "Gaussian Blur", materialStyle == QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR) { onMaterialStyle(QuickOpenConfig.MATERIAL_GAUSSIAN_BLUR) }
            }
        }

        // ── 几何 ─────────────────────────────────────────────
        item { SmallTitle("几何与位置参数") }
        item {
            SettingsCard {
                SliderRow("整体垂直高度偏移", "%d dp".format(offsetY.toInt()), offsetY, range = 100f..450f) { onOffsetY(it) }
                DividerRow()
                SliderRow("圆形视界直径", "%d dp".format(regionDiameter.toInt()), regionDiameter, range = 220f..420f) { onRegionDiameter(it) }
                DividerRow()
                SwitchRowM3("显示圆形区域底衬与边框", showRegionBg) { onShowRegionBg(it) }
                DividerRow()
                SwitchRowM3("显示顶部提示胶囊背景", showHeaderPillBg) { onShowHeaderPillBg(it) }
            }
        }

        // ── 图标物理 ────────────────────────────────────────
        item { SmallTitle("图标与物理动力学") }
        item {
            SettingsCard {
                SliderRow("图标基础尺寸", "%d dp".format(iconSize.toInt()), iconSize, range = 40f..80f) { onIconSize(it) }
                DividerRow()
                SliderRow("鱼眼最大放大倍率", "%.2f x".format(maxScale), maxScale, range = 1.10f..1.80f) { onMaxScale(it) }
                DividerRow()
                SliderRow("感应辐射范围", "%d dp".format(influenceRadius.toInt()), influenceRadius, range = 60f..200f) { onInfluenceRadius(it) }
            }
        }

        // ── 快捷项 ──────────────────────────────────────────
        item { SmallTitle("快捷项目管理与动作排序") }
        itemsIndexed(shortcuts, key = { _, it -> it.id }) { _, item ->
            ShortcutRow(
                item = item,
                onToggle = { onShortcutToggle(item, it) },
                onEdit = onShortcutEdit,
            )
        }
        item {
            ResetButton("恢复预设布局") { onResetShortcuts() }
        }
        item {
            androidx.compose.material3.Text(
                "提示：所有修改即时保存，长按指纹即可验证效果，无需重启 SystemUI。",
                color = semantic.fgSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Page1：软件设置（界面风格 / 安全模式 / 关于）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun SettingsPage(
    padding: PaddingValues,
    semantic: SemanticPalette,
    uiStyle: UiStyle,
    onStyleChange: (UiStyle) -> Unit,
    safetyTick: Int,
    onSafetyRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 界面风格 ────────────────────────────────────────
        item { SmallTitle("界面风格") }
        item {
            SettingsCard {
                MaterialRow(
                    "Material · 玻璃暗色",
                    "material3 组件 · 深色玻璃质感",
                    uiStyle == UiStyle.MATERIAL,
                ) { onStyleChange(UiStyle.MATERIAL) }
                DividerRow()
                MaterialRow(
                    "MIUIX · 澎湃卡片",
                    "miuix 组件 · 小米 HyperOS 风格",
                    uiStyle == UiStyle.MIUIX,
                ) { onStyleChange(UiStyle.MIUIX) }
            }
        }
        item {
            androidx.compose.material3.Text(
                "切换后立即生效（界面会刷新一次）。",
                color = semantic.fgSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        // ── 安全 ────────────────────────────────────────────
        item { SmallTitle("安全模式与崩溃保护") }
        item {
            SafetyCard(tick = safetyTick, onRequestRefresh = onSafetyRefresh)
        }

        // ── 关于 ────────────────────────────────────────────
        item { SmallTitle("关于") }
        item {
            SettingsCard {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    AboutLine("模块名称", "HyperOS QuickOpen")
                    AboutLine("功能", "指纹快捷面板自定义 · 图标与动效增强")
                    AboutLine("UI 框架", "miuix 0.8.8 + Material3 1.4.0 (Compose)")
                    AboutLine("使用方式", "指纹解锁时按压指纹传感器唤出快捷面板")
                    AboutLine("生效条件", "LSPosed 激活模块并勾选 SystemUI 作用域，重启 SystemUI")
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun AboutLine(label: String, value: String) {
    val semantic = currentSemantic()
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        androidx.compose.material3.Text(
            label,
            color = semantic.fgSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(84.dp),
        )
        androidx.compose.material3.Text(
            value,
            color = semantic.fgPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// 通用行组件（Material 分支；MIUIX 分支用 miuix 组件由 SettingsCard 内部按风格分派）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val semantic = currentSemantic()
    val style = LocalUiStyle.current
    if (style == UiStyle.MIUIX) {
        Card(modifier = Modifier.fillMaxWidth()) { Column { content() } }
    } else {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = semantic.surface),
            shape = RoundedCornerShape(18.dp),
        ) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
    }
}

@Composable
fun DividerRow() {
    val semantic = currentSemantic()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(semantic.divider),
    )
}

@Composable
fun MaterialRow(title: String, sub: String, selected: Boolean, onSelect: () -> Unit) {
    val semantic = currentSemantic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) semantic.primary else Color.Transparent)
                .combinedClickable(onClick = onSelect),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            androidx.compose.material3.Text(title, color = semantic.fgPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            androidx.compose.material3.Text(sub, color = semantic.fgSecondary, fontSize = 12.sp)
        }
        if (selected) {
            androidx.compose.material3.Text("✓", color = semantic.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SliderRow(title: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val semantic = currentSemantic()
    var v by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Text(title, color = semantic.fgPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            androidx.compose.material3.Text(valueText, color = semantic.fgSecondary, fontSize = 13.sp)
        }
        Slider(
            value = v,
            onValueChange = { v = it },
            onValueChangeFinished = { onChange(v) },
            valueRange = range,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = semantic.primary,
                activeTrackColor = semantic.primary,
                inactiveTrackColor = semantic.divider,
            ),
        )
    }
}

@Composable
fun SwitchRowM3(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val semantic = currentSemantic()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Text(title, color = semantic.fgPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedTrackColor = semantic.primary,
        ))
    }
}

@Composable
fun ResetButton(text: String, onClick: () -> Unit) {
    val semantic = currentSemantic()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(semantic.surfaceAlt)
            .combinedClickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(text, color = semantic.fgPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────
// 快捷项行（真实图标 + 开关 + 长按改图标弹窗）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ShortcutRow(item: ShortcutItem, onToggle: (Boolean) -> Unit, onEdit: (ShortcutItem) -> Unit) {
    val ctx = LocalContext.current
    val semantic = currentSemantic()
    val style = LocalUiStyle.current
    var showEdit by remember { mutableStateOf(false) }
    val painter = remember(item.id, item.iconPackage, item.iconGlyph) { IconPainter.resolve(item, ctx) }

    if (style == UiStyle.MIUIX) {
        SuperSwitch(
            title = item.title,
            summary = item.subtitle,
            checked = item.enabled,
            startAction = {
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = item.title,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)),
                )
            },
            onCheckedChange = onToggle,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { showEdit = true }).fillMaxWidth(),
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = { showEdit = true })
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = item.title,
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                androidx.compose.material3.Text(item.title, color = semantic.fgPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                androidx.compose.material3.Text(item.subtitle, color = semantic.fgSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Switch(checked = item.enabled, onCheckedChange = onToggle, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = semantic.primary))
        }
    }

    if (showEdit) {
        IconEditDialog(item = item, onDismiss = { showEdit = false }, onSave = { onEdit(it); showEdit = false })
    }
}

// ─────────────────────────────────────────────────────────────────────
// 图标自定义弹窗（包名 + glyph key + 实时预览）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun IconEditDialog(item: ShortcutItem, onDismiss: () -> Unit, onSave: (ShortcutItem) -> Unit) {
    val ctx = LocalContext.current
    var pkg by remember { mutableStateOf(item.iconPackage) }
    var glyph by remember { mutableStateOf(item.iconGlyph) }

    // 实时预览：每次输入变化都重新光栅化（不走缓存，保证即时反馈）
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
        androidx.compose.ui.graphics.painter.BitmapPainter(bmp.toComposeImageBitmap())
    }

    SuperDialog(
        show = true,
        title = "自定义图标 - ${item.title}",
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.foundation.Image(
                painter = livePreview,
                contentDescription = "preview",
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
            )
            MiuixTextField("图标来源包名（如 com.tencent.mm）", pkg) { pkg = it }
            MiuixTextField("内置符号 key（${IconGlyphFactory.GLYPH_KEYS.joinToString("/")}）", glyph) { glyph = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { ResetButton("取消", onDismiss) }
                Box(Modifier.weight(1f)) { ResetButton("保存") {
                    item.iconPackage = pkg.trim()
                    item.iconGlyph = glyph.trim()
                    onSave(item)
                } }
            }
        }
    }
}

@Composable
fun MiuixTextField(label: String, value: String, onChange: (String) -> Unit) {
    top.yukonga.miuix.kmp.basic.TextField(
        value = value,
        onValueChange = onChange,
        label = label,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun android.graphics.Bitmap.toComposeImageBitmap(): androidx.compose.ui.graphics.ImageBitmap = this.asImageBitmap()

// ─────────────────────────────────────────────────────────────────────
// 安全卡片
// ─────────────────────────────────────────────────────────────────────

@Composable
fun SafetyCard(tick: Int, onRequestRefresh: () -> Unit) {
    val ctx = LocalContext.current
    val semantic = currentSemantic()
    val style = LocalUiStyle.current

    // 每次进入组合 / tick 变化时从 SafetyStore 重读
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
        safeMode -> semantic.warn
        !hookEnabled -> semantic.fgSecondary
        else -> semantic.ok
    }

    SettingsCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            androidx.compose.material3.Text(statusText, color = statusColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
            androidx.compose.material3.Text(
                "崩溃记录",
                color = semantic.fgPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            androidx.compose.material3.Text(
                if (log.isBlank()) "暂无崩溃记录。" else log,
                color = semantic.fgSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 4.dp).basicMarquee(iterations = Int.MAX_VALUE),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    ResetButton("退出安全模式并重新启用") {
                        SafetyStore.exitSafeMode(ctx)
                        Toast.makeText(ctx, "已退出安全模式，重启 SystemUI 后 Hook 恢复生效", Toast.LENGTH_LONG).show()
                        onRequestRefresh()
                    }
                }
                Box(Modifier.weight(1f)) {
                    ResetButton("清空崩溃记录") {
                        SafetyStore.clearCrash(ctx)
                        Toast.makeText(ctx, "崩溃记录已清空", Toast.LENGTH_SHORT).show()
                        onRequestRefresh()
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (style == UiStyle.MIUIX) {
                SuperSwitch(
                    title = "启用指纹快捷方式增强 (Hook 总开关)",
                    checked = hookEnabled && !safeMode,
                    onCheckedChange = { checked ->
                        val st = SafetyStore.setHookEnabled(ctx, checked)
                        Toast.makeText(
                            ctx,
                            if (checked) "Hook 已启用，重启 SystemUI 后生效" else "Hook 已停用，重启 SystemUI 后生效",
                            Toast.LENGTH_LONG,
                        ).show()
                        onRequestRefresh()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SwitchRowM3("启用指纹快捷方式增强 (Hook 总开关)", hookEnabled && !safeMode) { checked ->
                    SafetyStore.setHookEnabled(ctx, checked)
                    Toast.makeText(
                        ctx,
                        if (checked) "Hook 已启用，重启 SystemUI 后生效" else "Hook 已停用，重启 SystemUI 后生效",
                        Toast.LENGTH_LONG,
                    ).show()
                    onRequestRefresh()
                }
            }
            Spacer(Modifier.height(4.dp))
            androidx.compose.material3.Text(
                "当 Hook 导致 SystemUI 在 5 分钟内连续崩溃 2 次时，模块会自动进入安全模式并停用全部 Hook，SystemUI 恢复原生指纹界面。修复配置后点「退出安全模式」即可重新启用。",
                color = semantic.fgSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
fun InfoLine(text: String) {
    val semantic = currentSemantic()
    androidx.compose.material3.Text(text, color = semantic.fgSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
}
