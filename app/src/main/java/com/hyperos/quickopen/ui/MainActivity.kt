package com.hyperos.quickopen.ui

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperos.quickopen.config.QuickOpenConfig
import com.hyperos.quickopen.model.ShortcutItem
import com.hyperos.quickopen.safety.SafetyStore
import com.hyperos.quickopen.view.IconGlyphFactory
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Compose 设置界面（固定 MIUIX 轨道）。
 * 底栏双页：Page0 参数配置、Page1 软件设置；二级页：主题设置（状态内导航，不 recreate）。
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

    // 配置镜像状态（单一大对象 + 手动触发持久化；直接驱动主题 → 实时生效）
    var materialStyle by remember { mutableStateOf(initialConfig.materialStyle) }
    var offsetY by remember { mutableStateOf(initialConfig.offsetY.toFloat()) }
    var regionDiameter by remember { mutableStateOf(initialConfig.regionDiameter.toFloat()) }
    var showRegionBg by remember { mutableStateOf(initialConfig.showRegionBg) }
    var showHeaderPillBg by remember { mutableStateOf(initialConfig.showHeaderPillBg) }
    var iconSize by remember { mutableStateOf(initialConfig.iconSize.toFloat()) }
    var maxScale by remember { mutableStateOf(initialConfig.maxScale) }
    var influenceRadius by remember { mutableStateOf(initialConfig.influenceRadius.toFloat()) }
    var themeMode by remember { mutableStateOf(initialConfig.themeMode) }
    var monetColors by remember { mutableStateOf(initialConfig.monetColors) }
    var barBlur by remember { mutableStateOf(initialConfig.barBlur) }
    var liquidGlass by remember { mutableStateOf(initialConfig.liquidGlass) }
    var floatingNavBar by remember { mutableStateOf(initialConfig.floatingNavBar) }
    var predictiveBack by remember { mutableStateOf(initialConfig.predictiveBack) }
    val shortcuts = remember { mutableStateListOf<ShortcutItem>().apply { addAll(initialConfig.shortcuts) } }
    var safetyTick by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }
    // 二级页：状态内导航（不再 recreate，返回/切换均瞬时）
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

    // 主题装配在状态内层 → themeMode/monet/floatingNavBar/liquidGlass 变化即时重组
    HyperOSQuickOpenTheme(
        themeMode = themeMode,
        monet = monetColors,
        barBlur = false,
        floatingNavBar = floatingNavBar,
        predictiveBack = false,
    ) {
        // —— 全部颜色读取必须在主题 provide 之内（否则拿到默认浅色，切换不生效）——
        val semantic = currentSemantic()
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val topSpace = topInset + 56.dp
        val bottomSpace = if (subPage == 1) {
            24.dp + navInset
        } else if (floatingNavBar) {
            72.dp + navInset
        } else {
            64.dp + navInset
        }
        val contentPadding = PaddingValues(top = topSpace, bottom = bottomSpace)

        Box(
            Modifier
                .fillMaxSize()
                .background(semantic.background),
        ) {
            // ── 内容（全屏滚动） ──────────────────────────────
            if (subPage == 1) {
                ThemeSettingsPage(
                    padding = contentPadding,
                    semantic = semantic,
                    themeMode = themeMode, onThemeMode = { themeMode = it; persist() },
                    monet = monetColors, onMonet = { monetColors = it; persist() },
                    liquidGlass = liquidGlass, onLiquidGlass = { liquidGlass = it; persist() },
                    floatingNavBar = floatingNavBar, onFloatingNavBar = { floatingNavBar = it; persist() },
                )
            } else if (page == 0) {
                ParamsPage(
                    padding = contentPadding,
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
                    padding = contentPadding,
                    semantic = semantic,
                    safetyTick = safetyTick,
                    onSafetyRefresh = { safetyTick++ },
                    onOpenThemePage = { subPage = 1 },
                )
            }

            // ── 顶栏（实色覆盖状态栏与标题区，防止列表内容从顶栏上方露白透出） ──
            Surface(
                color = semantic.surface,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp),
                ) {
                    if (subPage == 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 6.dp)
                                .size(44.dp)
                                .clip(CircleShape)
                                .combinedClickable(onClick = { subPage = 0 }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "‹",
                                color = semantic.fgPrimary,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Light,
                            )
                        }
                    }
                    Text(
                        if (subPage == 1) "主题设置" else "指纹快捷方式增强",
                        color = semantic.fgPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            // ── 底栏（二级页不显示） ────────────────────────────
            if (subPage == 0) {
                BottomNavBar(page, floatingNavBar, liquidGlass) { page = it }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// 底部导航栏（使用 miuix 官方 NavigationBar 与 FloatingNavigationBar）
// ─────────────────────────────────────────────────────────────────────

/** BoxScope 扩展：在覆盖层内用 align(BottomCenter) 定位，使用 ui 库组件 */
@Composable
fun BoxScope.BottomNavBar(
    page: Int,
    floating: Boolean,
    liquidGlass: Boolean,
    onPageChange: (Int) -> Unit,
) {
    if (floating) {
        // UI 库官方 FloatingNavigationBar 组件（Apple 风格胶囊悬浮浮岛）
        val barColor = if (liquidGlass) {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f)
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        }
        FloatingNavigationBar(
            color = barColor,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            FloatingNavigationBarItem(
                selected = page == 0,
                onClick = { onPageChange(0) },
                icon = Icons.Filled.Build,
                label = "参数配置",
            )
            FloatingNavigationBarItem(
                selected = page == 1,
                onClick = { onPageChange(1) },
                icon = Icons.Filled.Settings,
                label = "软件设置",
            )
        }
    } else {
        // UI 库官方 NavigationBar 组件（常规贴底底栏）
        val barColor = if (liquidGlass) {
            MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        }
        NavigationBar(
            color = barColor,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            NavigationBarItem(
                selected = page == 0,
                onClick = { onPageChange(0) },
                icon = Icons.Filled.Build,
                label = "参数配置",
            )
            NavigationBarItem(
                selected = page == 1,
                onClick = { onPageChange(1) },
                icon = Icons.Filled.Settings,
                label = "软件设置",
            )
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
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
            Text(
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
// Page1：软件设置（主题设置入口 / 安全模式 / 关于）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun SettingsPage(
    padding: PaddingValues,
    semantic: SemanticPalette,
    safetyTick: Int,
    onSafetyRefresh: () -> Unit,
    onOpenThemePage: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 主题设置入口（二级页） ───────────────────────────
        item { SmallTitle("主题") }
        item {
            SettingsCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = onOpenThemePage)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "主题设置",
                            color = semantic.fgPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "深浅模式 · Monet · 毛玻璃 · 液态玻璃 · 手势",
                            color = semantic.fgSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = semantic.fgSecondary,
                    )
                }
            }
        }
        item {
            Text(
                "主题调整实时生效，无需重启。",
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
                    AboutLine("UI 框架", "miuix 0.8.8 (Compose)")
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
        Text(
            label,
            color = semantic.fgSecondary,
            fontSize = 13.sp,
            modifier = Modifier.width(84.dp),
        )
        Text(
            value,
            color = semantic.fgPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
// 通用行组件（MIUIX 卡片）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) { Column { content() } }
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
            Text(title, color = semantic.fgPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = semantic.fgSecondary, fontSize = 12.sp)
        }
        if (selected) {
            Text("✓", color = semantic.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SliderRow(title: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val semantic = currentSemantic()
    var v by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = semantic.fgPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(valueText, color = semantic.fgSecondary, fontSize = 13.sp)
        }
        Slider(
            value = v,
            onValueChange = { v = it },
            onValueChangeFinished = { onChange(v) },
            valueRange = range,
            colors = SliderDefaults.colors(
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
        Text(title, color = semantic.fgPrimary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(
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
        Text(text, color = semantic.fgPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────
// 快捷项行（真实图标 + 开关 + 长按改图标弹窗）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ShortcutRow(item: ShortcutItem, onToggle: (Boolean) -> Unit, onEdit: (ShortcutItem) -> Unit) {
    val ctx = LocalContext.current
    val semantic = currentSemantic()
    var showEdit by remember { mutableStateOf(false) }
    val painter = remember(item.id, item.iconPackage, item.iconGlyph) { IconPainter.resolve(item, ctx) }

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
        BitmapPainter(bmp.toComposeImageBitmap())
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
            Text(statusText, color = statusColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                color = semantic.fgPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
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
            Spacer(Modifier.height(4.dp))
            Text(
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
    Text(text, color = semantic.fgSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
}

// ─────────────────────────────────────────────────────────────────────
// 二级页：主题设置（深浅模式 / Monet / 底栏液态玻璃 / 悬浮底栏）
// ─────────────────────────────────────────────────────────────────────

@Composable
fun ThemeSettingsPage(
    padding: PaddingValues,
    semantic: SemanticPalette,
    themeMode: Int, onThemeMode: (Int) -> Unit,
    monet: Boolean, onMonet: (Boolean) -> Unit,
    liquidGlass: Boolean, onLiquidGlass: (Boolean) -> Unit,
    floatingNavBar: Boolean, onFloatingNavBar: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 深浅模式三段选择器 ──────────────────────────────
        item {
            ThemeSegmented(
                options = listOf("跟随系统", "浅色", "深色"),
                selectedIndex = themeMode,
                onSelect = onThemeMode,
            )
        }

        // ── Monet ──────────────────────────────────────────
        item {
            SettingsCard {
                SwitchRowM3("启用 Monet 颜色", monet) { onMonet(it) }
            }
        }

        // ── 外观效果 ───────────────────────────────────────
        item {
            SettingsCard {
                SwitchRowM3("底栏液态玻璃", liquidGlass) { onLiquidGlass(it) }
                DividerRow()
                SwitchRowM3("悬浮底栏", floatingNavBar) { onFloatingNavBar(it) }
            }
        }
        item {
            Text(
                "底栏液态玻璃：底栏采用 Apple 风格半透明磨砂质感；悬浮底栏：UI 库原生胶囊浮岛底栏。",
                color = semantic.fgSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** 三段选择器（跟随系统 / 浅色 / 深色），胶囊段选风格。 */
@Composable
fun ThemeSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val semantic = currentSemantic()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(semantic.surface)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) semantic.primary.copy(alpha = 0.16f) else Color.Transparent)
                    .combinedClickable(onClick = { onSelect(i) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (selected) semantic.primary else semantic.fgSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
