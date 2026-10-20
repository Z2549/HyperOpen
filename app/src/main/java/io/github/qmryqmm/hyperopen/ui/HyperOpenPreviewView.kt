package io.github.qmryqmm.hyperopen.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.qmryqmm.hyperopen.config.HyperOpenConfig
import io.github.qmryqmm.hyperopen.factory.HyperOSMaterialFactory
import io.github.qmryqmm.hyperopen.layout.CircularHoneycombPacking
import io.github.qmryqmm.hyperopen.model.ShortcutItem
import io.github.qmryqmm.hyperopen.view.CircularBubbleView
import io.github.qmryqmm.hyperopen.view.CircularRegionBackdropView
import io.github.qmryqmm.hyperopen.view.CustomHyperOpenContainer

/**
 * 整体指纹快捷面板高保真预览与长按拖拽交互视图。
 * 复用与 SystemUI 完全一致的 CircularBubbleView、CircularRegionBackdropView 与 HeaderPill 渲染，
 * 确保所见即所得。
 * - 普通滑动：模拟指纹解锁后手指滑过快捷图标的鱼眼动力学吸附与实时聚焦反馈；
 * - 长按图标：震动进入自定义排版模式，可自由拖拽改变图标在蜂窝阵列中的位置顺序；
 * - 松手磁吸：其他图标平滑让位，松手后磁吸回网格并同步保存。
 */
data class BubbleBlurSpot(
    val id: String,
    val centerX: Float,
    val centerY: Float,
    val diameterPx: Float,
    val alpha: Float = 1.0f,
)

data class PreviewBlurSceneState(
    val regionCenterX: Float = 0f,
    val regionCenterY: Float = 0f,
    val regionDiameterPx: Float = 0f,
    val headerLeftPx: Float = 0f,
    val headerTopPx: Float = 0f,
    val headerWidthPx: Float = 0f,
    val headerHeightPx: Float = 0f,
    val backBtnLeftPx: Float = 0f,
    val backBtnTopPx: Float = 0f,
    val backBtnSizePx: Float = 0f,
    val bubbles: List<BubbleBlurSpot> = emptyList(),
)

/**
 * 整体指纹快捷面板高保真预览与长按拖拽交互视图。
 * 复用与 SystemUI 完全一致的 CircularBubbleView、CircularRegionBackdropView 与 HeaderPill 渲染，
 * 确保所见即所得。
 * - 普通滑动：模拟指纹解锁后手指滑过快捷图标的鱼眼动力学吸附与实时聚焦反馈；
 * - 长按图标：震动进入自定义排版模式，可自由拖拽改变图标在蜂窝阵列中的位置顺序；
 * - 松手磁吸：其他图标平滑让位，松手后磁吸回网格并同步保存。
 */
class HyperOpenPreviewView(
    context: Context,
    private var config: HyperOpenConfig,
    private val onBackClick: () -> Unit,
    private val onOrderChanged: (List<ShortcutItem>) -> Unit,
    private val onBlurSceneChanged: ((PreviewBlurSceneState) -> Unit)? = null,
) : FrameLayout(context) {

    private var resolvedStyle = HyperOSMaterialFactory.resolveActiveStyle(context, config.materialStyle)
    private var vibrator: Vibrator? = null

    private lateinit var topHeaderContainer: LinearLayout
    private lateinit var headerPill: LinearLayout
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var editBanner: TextView

    private lateinit var regionBackdrop: CircularRegionBackdropView
    private lateinit var bubblesContainer: FrameLayout
    private val bubbleViews = mutableListOf<CircularBubbleView>()
    private val activeShortcuts = mutableListOf<ShortcutItem>()
    private val gridPoints = mutableListOf<PointF>()

    private var lastRegionCenterX = 0f
    private var lastRegionCenterY = 0f
    private var lastRegionDiameterPx = 0f

    private var currentSelectedBubble: CircularBubbleView? = null

    // 拖拽排版相关状态
    var isEditMode: Boolean = false
        private set
    private var isTouching: Boolean = false
    private var draggingBubble: CircularBubbleView? = null
    private var dragTouchStartX = 0f
    private var dragTouchStartY = 0f
    private var isDraggingActive = false
    private val longPressRunnable = Runnable {
        val target = pendingPressBubble
        if (target != null && !isEditMode && !isDraggingActive) {
            currentSelectedBubble?.setSelectedState(false)
            currentSelectedBubble = null
            enterEditMode(target)
        }
    }
    private var pendingPressBubble: CircularBubbleView? = null

    init {
        initVibrator()
        initViews()
    }

    private fun initVibrator() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibrator = vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {}
    }

    private fun triggerHaptic() {
        CustomHyperOpenContainer.performConfiguredHaptic(vibrator, config)
    }

    private var lastLayoutW = 0
    private var lastLayoutH = 0

    private fun initViews() {
        clipChildren = false
        clipToPadding = false
        refreshBackdropMask()

        // 1. 顶部提示胶囊与编辑提示
        buildHeader()

        // 2. 蜂窝圆形区域底衬（预览界面由 Compose 的 textureBlur 统一渲染实时壁纸模糊，禁用窗口级 BackgroundBlur 避免双层底色叠加）
        regionBackdrop = CircularRegionBackdropView(context)
        regionBackdrop.setSystemBlurEnabled(false)
        regionBackdrop.setStyle(resolvedStyle)
        addView(regionBackdrop)

        // 3. 气泡容器
        bubblesContainer = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }
        addView(bubblesContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        // 4. 左上角小悬浮返回按钮
        buildFloatingBackButton()

        // 5. 在首次测量前提前建立蜂窝气泡视图树
        setupLayout()
    }

    private var floatingBackButton: FrameLayout? = null

    private fun buildFloatingBackButton() {
        val btnSize = dpToPx(40f).toInt()
        val backBtn = FrameLayout(context).apply {
            background = HyperOSMaterialFactory.createCircularBubbleDrawable(
                resolvedStyle, 0xFFFFFFFF.toInt(), false, false
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (isEditMode) {
                    exitEditMode()
                }
                onBackClick()
            }
        }

        val iconView = android.widget.ImageView(context).apply {
            val arrowDrawable = io.github.qmryqmm.hyperopen.view.IconGlyphFactory.createGlyph("arrow_left", 0xFFFFFFFF.toInt())
            setImageDrawable(arrowDrawable)
            val p = dpToPx(10f).toInt()
            setPadding(p, p, p, p)
        }
        backBtn.addView(iconView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val btnLp = LayoutParams(btnSize, btnSize).apply {
            gravity = Gravity.TOP or Gravity.START
            leftMargin = dpToPx(18f).toInt()
            topMargin = dpToPx(46f).toInt()
        }
        addView(backBtn, btnLp)
        floatingBackButton = backBtn
    }

    private fun refreshFloatingBackButtonBackground() {
        val btn = floatingBackButton ?: return
        btn.background = HyperOSMaterialFactory.createCircularBubbleDrawable(
            resolvedStyle, 0xFFFFFFFF.toInt(), false, false
        )
        btn.invalidate()
    }

    private fun buildHeader() {
        topHeaderContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        headerPill = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padH = dpToPx(24f).toInt()
            val padV = dpToPx(10f).toInt()
            setPadding(padH, padV, padH, padV)
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val w = view.width
                    val h = view.height
                    if (w <= 0 || h <= 0) return
                    outline.setRoundRect(0, 0, w, h, Math.min(w, h) / 2f)
                    outline.alpha = 0f
                }
            }
            addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
                if ((r - l) != (or - ol) || (b - t) != (ob - ot) || l != ol || t != ot) {
                    applyHeaderPillBackground()
                    notifyBlurSceneChanged()
                }
            }
        }

        headerTitle = TextView(context).apply {
            text = "滑动选择快捷方式"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(Color.WHITE)
            paint.isFakeBoldText = true
            gravity = Gravity.CENTER
        }

        headerSubtitle = TextView(context).apply {
            text = "长按图标编辑位置 · 松手直接打开"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(0xAAFFFFFF.toInt())
            gravity = Gravity.CENTER
        }

        headerPill.addView(headerTitle)
        headerPill.addView(headerSubtitle)

        // 编辑模式横幅
        editBanner = TextView(context).apply {
            text = "✦ 自定义排版模式：拖拽移动图标，点击空白完成 ✦"
            textSize = 11f
            setTextColor(0xFFFF9933.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val padH = dpToPx(14f).toInt()
            val padV = dpToPx(5f).toInt()
            setPadding(padH, padV, padH, padV)
            visibility = View.GONE
        }

        topHeaderContainer.addView(headerPill, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val bannerLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dpToPx(8f).toInt()
        }
        topHeaderContainer.addView(editBanner, bannerLp)

        val topLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dpToPx(100f).toInt()
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        addView(topHeaderContainer, topLp)

        applyHeaderPillStyleAndLayout()
    }

    /**
     * 1:1 对齐 CustomHyperOpenContainer：同步应用顶部提示显隐、高度位置、文字大小、水平居中和胶囊背景
     */
    private fun applyHeaderPillStyleAndLayout() {
        if (!::topHeaderContainer.isInitialized || !::headerPill.isInitialized) return
        val showHeader = config.showHeaderPill
        headerPill.visibility = if (showHeader) View.VISIBLE else View.GONE

        val titleSp = config.headerTitleSize.coerceIn(12, 26).toFloat()
        val subtitleSp = config.headerSubtitleSize.coerceIn(9, 18).toFloat()
        val centered = config.headerTextCentered
        val topMarginDp = config.headerTopMargin.coerceIn(20, 420).toFloat()

        headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp)
        headerTitle.gravity = if (centered) Gravity.CENTER else Gravity.START
        headerSubtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, subtitleSp)
        headerSubtitle.gravity = if (centered) Gravity.CENTER else Gravity.START

        headerPill.gravity = if (centered) Gravity.CENTER else (Gravity.START or Gravity.CENTER_VERTICAL)
        topHeaderContainer.gravity = if (centered) Gravity.CENTER_HORIZONTAL else Gravity.START

        val pillLp = (headerPill.layoutParams as? LinearLayout.LayoutParams)
            ?: LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        pillLp.gravity = if (centered) Gravity.CENTER_HORIZONTAL else Gravity.START
        pillLp.leftMargin = if (centered) 0 else dpToPx(24f).toInt()
        pillLp.rightMargin = if (centered) 0 else dpToPx(24f).toInt()
        headerPill.layoutParams = pillLp

        val topLp = (topHeaderContainer.layoutParams as? LayoutParams)
            ?: LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        topLp.topMargin = dpToPx(topMarginDp).toInt()
        topLp.gravity = Gravity.TOP or (if (centered) Gravity.CENTER_HORIZONTAL else Gravity.START)
        topHeaderContainer.layoutParams = topLp

        applyHeaderPillBackground()
    }

    /**
     * 按 showHeaderPillBg 开关与 materialStyle 材质动态设置顶部提示胶囊背景
     */
    private fun applyHeaderPillBackground() {
        if (!::headerPill.isInitialized) return
        val show = config.showHeaderPill && config.showHeaderPillBg
        if (show) {
            headerPill.background = HyperOSMaterialFactory.createHeaderPillDrawable(resolvedStyle)
            headerPill.clipToOutline = true
        } else {
            headerPill.background = null
            headerPill.clipToOutline = false
        }
        headerPill.invalidateOutline()
        headerPill.invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0 && (w != lastLayoutW || h != lastLayoutH || bubbleViews.isEmpty())) {
            setupLayout(w, h)
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(longPressRunnable)
        if (isEditMode && bubbleViews.isNotEmpty()) {
            isEditMode = false
            onOrderChanged(bubbleViews.map { it.item })
        }
        super.onDetachedFromWindow()
    }

    /**
     * 获取与真实锁屏 SystemUI (GxzwWindowFrameLayout 全屏物理窗口) 1:1 完全对齐的 FOD 指纹中心坐标：
     * 1) 优先使用 SystemUI 真实展开时上报并持久化的 lastRealFodX / lastRealFodY；
     * 2) 其次尝试读取系统属性 persist.vendor.sys.fp.fod.location.X_Y 与 size.width_height；
     * 3) 再次尝试反射调用 MiuiGxzwUtils；
     * 4) 最终使用 Display.getRealSize 真实物理屏幕高度 * 0.795f 兜底（绝不受 Activity 导航栏裁剪高度影响），
     *    并按当前预览 View 在屏幕上的真实 window location 偏移换算为局部坐标。
     */
    private fun calculateFodCoordinates(targetW: Float, targetH: Float): PointF {
        val loc = IntArray(2)
        try {
            getLocationOnScreen(loc)
        } catch (_: Throwable) {}
        val viewOffsetX = loc[0].toFloat()
        val viewOffsetY = loc[1].toFloat()

        // 1) 优先读取 SystemUI 真实上报的精准 FOD 中心坐标
        if (config.lastRealFodX > 0f && config.lastRealFodY > 0f) {
            return PointF(config.lastRealFodX - viewOffsetX, config.lastRealFodY - viewOffsetY)
        }

        // 2) 尝试读取小米底层 FOD 硬件坐标系统属性 (与 MiuiGxzwUtils 数据源一致)
        try {
            val spClass = Class.forName("android.os.SystemProperties")
            val getMethod = spClass.getMethod("get", String::class.java, String::class.java)
            val locStr = (getMethod.invoke(null, "persist.vendor.sys.fp.fod.location.X_Y", "") as? String)?.trim().orEmpty()
            val sizeStr = (getMethod.invoke(null, "persist.vendor.sys.fp.fod.size.width_height", "") as? String)?.trim().orEmpty()
            if (locStr.contains(",") && sizeStr.contains(",")) {
                val locParts = locStr.split(",")
                val sizeParts = sizeStr.split(",")
                val fx = locParts[0].trim().toFloat()
                val fy = locParts[1].trim().toFloat()
                val fw = sizeParts[0].trim().toFloat()
                val fh = sizeParts[1].trim().toFloat()
                if (fw > 0f && fh > 0f && fy > 0f) {
                    return PointF(fx + fw / 2f - viewOffsetX, fy + fh / 2f - viewOffsetY)
                }
            }
        } catch (_: Throwable) {}

        // 3) 尝试反射读取 MiuiGxzwUtils
        try {
            val utilsClass = Class.forName("com.miui.keyguard.biometrics.fod.MiuiGxzwUtils")
            val caculateMethod = utilsClass.getMethod("caculateGxzwIconSize", Context::class.java)
            caculateMethod.invoke(null, context)
            val iconX = utilsClass.getField("GXZW_ICON_X").getInt(null)
            val iconY = utilsClass.getField("GXZW_ICON_Y").getInt(null)
            val iconW = utilsClass.getField("GXZW_ICON_WIDTH").getInt(null)
            val iconH = utilsClass.getField("GXZW_ICON_HEIGHT").getInt(null)
            if (iconW > 0 && iconH > 0 && iconY > 0) {
                return PointF(iconX + iconW / 2.0f - viewOffsetX, iconY + iconH / 2.0f - viewOffsetY)
            }
        } catch (_: Throwable) {}

        // 4) 真实物理屏幕尺寸兜底（使用 Display.getRealSize，与 HyperOpenHook.getDisplaySize 完全一致）
        val realSize = Point()
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.getRealSize(realSize)
        } catch (_: Throwable) {}
        val realW = if (realSize.x > 0) realSize.x.toFloat() else targetW
        val realH = if (realSize.y > 0) realSize.y.toFloat() else targetH
        return PointF(realW / 2.0f - viewOffsetX, realH * 0.795f - viewOffsetY)
    }

    private fun refreshBackdropMask() {
        // 保持预览容器自身透明，避免遮挡下层 Compose 统一渲染的实时毛玻璃图层
        setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * 参数热更新：材质、胶囊背景开关、尺寸、气泡模糊半径与偏移即刻生效。
     * 若当前正处于手指按压、悬停放大或长按拖拽排版过程中，绝不重建视图树，避免打断手势或重置缩放。
     */
    fun updateConfig(newConfig: HyperOpenConfig) {
        if (isTouching || isEditMode || isDraggingActive) {
            this.config = newConfig
            return
        }
        val old = this.config
        val needRebuild = old.offsetY != newConfig.offsetY ||
            old.regionDiameter != newConfig.regionDiameter ||
            old.showRegionBg != newConfig.showRegionBg ||
            old.iconSize != newConfig.iconSize ||
            old.iconSpacingOffset != newConfig.iconSpacingOffset ||
            old.materialStyle != newConfig.materialStyle ||
            old.bubblePaddingPercent != newConfig.bubblePaddingPercent ||
            old.bubbleBlurRadius != newConfig.bubbleBlurRadius ||
            old.appIconCropMode != newConfig.appIconCropMode ||
            old.shortcuts.size != newConfig.shortcuts.size

        this.config = newConfig
        this.resolvedStyle = HyperOSMaterialFactory.resolveActiveStyle(context, config.materialStyle)
        refreshBackdropMask()
        if (::regionBackdrop.isInitialized) {
            regionBackdrop.setStyle(resolvedStyle)
            regionBackdrop.setBlurRadius(config.regionBlurRadius.toFloat())
        }
        applyHeaderPillStyleAndLayout()
        refreshFloatingBackButtonBackground()
        if (needRebuild || bubbleViews.isEmpty()) {
            setupLayout()
        } else {
            notifyBlurSceneChanged()
        }
    }

    /**
     * 重建蜂窝网格布局并绑定气泡
     */
    fun setupLayout(w: Int = width, h: Int = height) {
        val targetW = if (w > 0) w else resources.displayMetrics.widthPixels
        val targetH = if (h > 0) h else resources.displayMetrics.heightPixels
        if (targetW <= 0 || targetH <= 0) return

        lastLayoutW = targetW
        lastLayoutH = targetH

        bubblesContainer.removeAllViews()
        bubbleViews.clear()
        activeShortcuts.clear()
        gridPoints.clear()
        currentSelectedBubble = null

        val fod = calculateFodCoordinates(targetW.toFloat(), targetH.toFloat())
        val fodX = fod.x
        val fodY = fod.y
        val regionCenterY = fodY - dpToPx(config.offsetY.toFloat())
        val regionDiameterPx = dpToPx(config.regionDiameter.toFloat())
        val regionRadiusPx = regionDiameterPx / 2f
        val iconSizePx = dpToPx(config.iconSize.toFloat())

        lastRegionCenterX = fodX
        lastRegionCenterY = regionCenterY
        lastRegionDiameterPx = regionDiameterPx

        // 定位底衬
        regionBackdrop.visibility = if (config.showRegionBg) View.VISIBLE else View.GONE
        regionBackdrop.setSystemBlurEnabled(false)
        regionBackdrop.setStyle(resolvedStyle)
        regionBackdrop.setBlurRadius(config.regionBlurRadius.toFloat())
        regionBackdrop.updateGeometry(regionRadiusPx)
        val rLp = LayoutParams(regionDiameterPx.toInt(), regionDiameterPx.toInt()).apply {
            leftMargin = (fodX - regionRadiusPx).toInt()
            topMargin = (regionCenterY - regionRadiusPx).toInt()
        }
        regionBackdrop.layoutParams = rLp

        // 确保顶部提示胶囊与悬浮返回按钮样式即时更新
        applyHeaderPillStyleAndLayout()
        refreshFloatingBackButtonBackground()

        // 筛选有效快捷项
        for (item in config.shortcuts) {
            if (item.enabled) activeShortcuts.add(item)
        }

        // 计算蜂窝坐标
        val spacingOffsetPx = dpToPx(config.iconSpacingOffset.toFloat())
        val points = CircularHoneycombPacking.calculatePoints(
            activeShortcuts.size,
            fodX,
            regionCenterY,
            regionRadiusPx,
            iconSizePx,
            spacingOffsetPx
        )
        gridPoints.addAll(points)

        // 实例化气泡（预览界面禁用底层窗口级 BackgroundBlur，由 Compose 的 textureBlur 渲染实时壁纸模糊，确保样式与真实锁屏 1:1 对齐且无双层叠加闪烁）
        for (i in 0 until points.size) {
            val item = activeShortcuts[i]
            val pt = points[i]

            val bubble = CircularBubbleView(
                context, item, resolvedStyle, iconSizePx.toInt(), context.classLoader, config
            )
            bubble.setSystemBlurEnabled(false)
            bubble.setIconAlpha(config.iconAlpha)
            bubble.setOnScaleChangedListener {
                notifyBlurSceneChanged()
            }
            bubble.targetX = pt.x
            bubble.targetY = pt.y

            val bLp = LayoutParams(iconSizePx.toInt(), iconSizePx.toInt()).apply {
                leftMargin = (pt.x - iconSizePx / 2f).toInt()
                topMargin = (pt.y - iconSizePx / 2f).toInt()
            }
            bubble.layoutParams = bLp

            bubblesContainer.addView(bubble)
            bubbleViews.add(bubble)
        }

        // 立即同步执行 bubblesContainer 与内部所有气泡及 iconView 的 measure + layout
        val wSpec = MeasureSpec.makeMeasureSpec(targetW, MeasureSpec.EXACTLY)
        val hSpec = MeasureSpec.makeMeasureSpec(targetH, MeasureSpec.EXACTLY)
        bubblesContainer.measure(wSpec, hSpec)
        bubblesContainer.layout(0, 0, targetW, targetH)
        if (config.showRegionBg) {
            val rSpec = MeasureSpec.makeMeasureSpec(regionDiameterPx.toInt(), MeasureSpec.EXACTLY)
            regionBackdrop.measure(rSpec, rSpec)
            regionBackdrop.layout(
                rLp.leftMargin,
                rLp.topMargin,
                rLp.leftMargin + regionDiameterPx.toInt(),
                rLp.topMargin + regionDiameterPx.toInt()
            )
        }

        bubblesContainer.invalidate()
        notifyBlurSceneChanged()
        post { notifyBlurSceneChanged() }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // 如果点击发生在左上角悬浮返回按钮区域，放行给子 View 处理点击
        floatingBackButton?.let { btn ->
            if (btn.visibility == View.VISIBLE) {
                val btnLp = btn.layoutParams as? LayoutParams
                if (btnLp != null) {
                    val l = btnLp.leftMargin
                    val t = btnLp.topMargin
                    val r = l + btn.width.coerceAtLeast(dpToPx(48f).toInt())
                    val b = t + btn.height.coerceAtLeast(dpToPx(48f).toInt())
                    if (ev.x >= l && ev.x <= r && ev.y >= t && ev.y <= b) {
                        return false
                    }
                }
            }
        }
        // 其余区域拦截触摸事件以便处理全局滑动动力学与拖拽
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isTouching = true
                dragTouchStartX = x
                dragTouchStartY = y
                pendingPressBubble = findTouchedBubble(x, y)

                if (pendingPressBubble != null) {
                    if (isEditMode) {
                        // 编辑模式下直接抓取拖拽
                        startDragging(pendingPressBubble!!, x, y)
                    } else {
                        // 准备长按识别 (450ms 进入自定义排版)，同时立即展现按压鱼眼放大反馈
                        removeCallbacks(longPressRunnable)
                        postDelayed(longPressRunnable, 450)
                        handleFingerHover(x, y)
                    }
                } else if (!isEditMode) {
                    handleFingerHover(x, y)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val distFromDown = Math.hypot((x - dragTouchStartX).toDouble(), (y - dragTouchStartY).toDouble()).toFloat()
                if (distFromDown > dpToPx(12f) && !isDraggingActive) {
                    removeCallbacks(longPressRunnable)
                }

                if (isDraggingActive && draggingBubble != null) {
                    // 拖拽中：跟随手指自由移动
                    val iconSizePx = dpToPx(config.iconSize.toFloat())
                    val lp = draggingBubble!!.layoutParams as LayoutParams
                    lp.leftMargin = (x - iconSizePx / 2f).toInt()
                    lp.topMargin = (y - iconSizePx / 2f).toInt()
                    draggingBubble!!.layoutParams = lp

                    // 检测碰撞并实时重排其余气泡
                    checkDragReorder(x, y)
                    notifyBlurSceneChanged()
                } else if (!isEditMode) {
                    // 普通模拟指纹滑动模式：物理鱼眼动力学（手指停住时保持当前放大状态，绝不失效）
                    handleFingerHover(x, y)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isTouching = false
                removeCallbacks(longPressRunnable)

                if (isDraggingActive && draggingBubble != null) {
                    // 结束拖拽：吸附回最终目标槽位
                    finishDragging()
                } else if (!isEditMode) {
                    resetHoverSelection()
                } else if (isEditMode && pendingPressBubble == null) {
                    // 编辑模式下点击空白处退出
                    exitEditMode()
                }
                pendingPressBubble = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findTouchedBubble(x: Float, y: Float): CircularBubbleView? {
        val threshold = dpToPx(config.iconSize.toFloat()) * 0.75f
        var best: CircularBubbleView? = null
        var minDist = Float.MAX_VALUE
        for (b in bubbleViews) {
            val dist = Math.hypot((x - b.targetX).toDouble(), (y - b.targetY).toDouble()).toFloat()
            if (dist <= threshold && dist < minDist) {
                minDist = dist
                best = b
            }
        }
        return best
    }

    /**
     * 进入自定义排版模式
     */
    fun enterEditMode(initialTarget: CircularBubbleView? = null) {
        if (isEditMode) return
        isEditMode = true
        triggerHaptic()

        editBanner.visibility = View.VISIBLE
        editBanner.alpha = 0f
        editBanner.animate().alpha(1f).setDuration(200).start()

        headerTitle.text = "自定义排版模式"
        headerTitle.setTextColor(0xFFFF9933.toInt())
        headerSubtitle.text = "长按并拖动图标交换位置，点击空白完成"

        // 所有气泡进入统一排版微放状态，清除单点悬停缩放
        for (b in bubbleViews) {
            b.setSelectedState(false)
            b.setIconAlpha(config.iconAlpha)
            b.applyScale(1.05f)
        }

        if (initialTarget != null) {
            startDragging(initialTarget, dragTouchStartX, dragTouchStartY)
        } else {
            notifyBlurSceneChanged()
        }
    }

    fun exitEditMode() {
        if (!isEditMode) return
        isEditMode = false
        triggerHaptic()

        editBanner.animate().alpha(0f).setDuration(150).withEndAction {
            editBanner.visibility = View.GONE
        }.start()

        headerTitle.text = "滑动选择快捷方式"
        headerTitle.setTextColor(Color.WHITE)
        headerSubtitle.text = "长按图标编辑位置 · 松手直接打开"

        for (b in bubbleViews) {
            b.setSelectedState(false)
            b.applyScale(1.0f)
        }
        notifyBlurSceneChanged()

        // 同步通知外部回写最新排序列表
        val newOrdered = bubbleViews.map { it.item }
        onOrderChanged(newOrdered)
    }

    private fun startDragging(bubble: CircularBubbleView, touchX: Float, touchY: Float) {
        isDraggingActive = true
        draggingBubble = bubble
        bubble.setSelectedState(false)
        bubble.bringToFront()
        bubble.applyScale(1.22f)
        notifyBlurSceneChanged()
        triggerHaptic()
    }

    private fun checkDragReorder(touchX: Float, touchY: Float) {
        val currentIdx = bubbleViews.indexOf(draggingBubble)
        if (currentIdx < 0) return

        var closestSlot = currentIdx
        var minDistance = Float.MAX_VALUE
        val iconSizePx = dpToPx(config.iconSize.toFloat())

        for (i in 0 until gridPoints.size) {
            val pt = gridPoints[i]
            val dist = Math.hypot((touchX - pt.x).toDouble(), (touchY - pt.y).toDouble()).toFloat()
            if (dist < minDistance) {
                minDistance = dist
                closestSlot = i
            }
        }

        if (closestSlot != currentIdx && minDistance < iconSizePx * 0.75f) {
            // 在内存中重排 View 与 Item
            val movingBubble = bubbleViews.removeAt(currentIdx)
            bubbleViews.add(closestSlot, movingBubble)

            // 平滑将其他气泡过渡位移到新槽位
            for (i in 0 until bubbleViews.size) {
                val b = bubbleViews[i]
                val slotPt = gridPoints[i]
                b.targetX = slotPt.x
                b.targetY = slotPt.y

                if (b != draggingBubble) {
                    val lp = b.layoutParams as LayoutParams
                    val targetLeft = (slotPt.x - iconSizePx / 2f).toInt()
                    val targetTop = (slotPt.y - iconSizePx / 2f).toInt()

                    b.animate()
                        .x(targetLeft.toFloat())
                        .y(targetTop.toFloat())
                        .setDuration(220)
                        .setInterpolator(OvershootInterpolator(1.1f))
                        .setUpdateListener { notifyBlurSceneChanged() }
                        .withEndAction {
                            lp.leftMargin = targetLeft
                            lp.topMargin = targetTop
                            b.layoutParams = lp
                            b.translationX = 0f
                            b.translationY = 0f
                            notifyBlurSceneChanged()
                        }
                        .start()
                }
            }
            notifyBlurSceneChanged()
            triggerHaptic()
        }
    }

    private fun finishDragging() {
        val b = draggingBubble ?: return
        isDraggingActive = false
        val idx = bubbleViews.indexOf(b)
        val iconSizePx = dpToPx(config.iconSize.toFloat())

        if (idx >= 0 && idx < gridPoints.size) {
            val pt = gridPoints[idx]
            b.targetX = pt.x
            b.targetY = pt.y
            val targetLeft = (pt.x - iconSizePx / 2f).toInt()
            val targetTop = (pt.y - iconSizePx / 2f).toInt()

            b.applyScale(1.05f)

            val lp = b.layoutParams as LayoutParams
            lp.leftMargin = targetLeft
            lp.topMargin = targetTop
            b.layoutParams = lp
            b.translationX = 0f
            b.translationY = 0f
        }
        draggingBubble = null
        notifyBlurSceneChanged()
        triggerHaptic()
    }

    /**
     * 模拟指纹滑动物理动力学
     */
    private fun handleFingerHover(touchX: Float, touchY: Float) {
        val influenceRadiusPx = dpToPx(config.influenceRadius.toFloat())
        val iconSizePx = dpToPx(config.iconSize.toFloat())
        val hitDistanceThreshold = iconSizePx * 0.95f

        var closest: CircularBubbleView? = null
        var minDistance = Float.MAX_VALUE

        for (b in bubbleViews) {
            val dist = Math.hypot((touchX - b.targetX).toDouble(), (touchY - b.targetY).toDouble()).toFloat()
            if (dist < minDistance) {
                minDistance = dist
                closest = b
            }

            var scale = 1.0f
            var currentAlpha = config.iconAlpha
            if (dist < influenceRadiusPx) {
                val factor = Math.cos((dist / influenceRadiusPx) * (Math.PI / 2.0)).toFloat()
                scale = 1.0f + (config.maxScale - 1.0f) * factor
                currentAlpha = config.iconAlpha + (config.fisheyeZoomAlpha - config.iconAlpha) * factor
            }
            b.applyScale(scale)
            b.setIconAlpha(currentAlpha)
        }

        if (closest != null && minDistance < hitDistanceThreshold) {
            if (currentSelectedBubble != closest) {
                currentSelectedBubble?.setSelectedState(false)

                closest.setSelectedState(true)
                closest.bringToFront()
                currentSelectedBubble = closest

                headerTitle.text = closest.item.title
                headerTitle.setTextColor(closest.item.tintColor)
                headerSubtitle.text = closest.item.subtitle
                triggerHaptic()
            }
        } else {
            if (currentSelectedBubble != null) {
                currentSelectedBubble?.setSelectedState(false)
                currentSelectedBubble = null
                headerTitle.text = "滑动选择快捷方式"
                headerTitle.setTextColor(Color.WHITE)
                headerSubtitle.text = "长按图标编辑位置 · 松手直接打开"
            }
        }
        notifyBlurSceneChanged()
    }

    private fun resetHoverSelection() {
        currentSelectedBubble?.setSelectedState(false)
        currentSelectedBubble = null
        for (b in bubbleViews) {
            b.applyScale(1.0f)
            b.setIconAlpha(config.iconAlpha)
        }
        headerTitle.text = "滑动选择快捷方式"
        headerTitle.setTextColor(Color.WHITE)
        headerSubtitle.text = "长按图标编辑位置 · 松手直接打开"
        notifyBlurSceneChanged()
    }

    private fun notifyBlurSceneChanged() {
        val listener = onBlurSceneChanged ?: return
        val iconSizePx = dpToPx(config.iconSize.toFloat())
        val spots = ArrayList<BubbleBlurSpot>(bubbleViews.size)
        for (b in bubbleViews) {
            val lp = b.layoutParams as? LayoutParams
            val cx = if (lp != null) lp.leftMargin + iconSizePx / 2f + b.translationX else b.targetX
            val cy = if (lp != null) lp.topMargin + iconSizePx / 2f + b.translationY else b.targetY
            val scale = b.currentScale.coerceAtLeast(0.1f)
            // 内缩 3px (直径减 6px) 使预览毛玻璃圆严格收拢在 2.2px 气泡描边内侧，放大时绝不溢出边框
            val effectiveDiameter = (iconSizePx - 6f).coerceAtLeast(1f) * scale
            spots.add(
                BubbleBlurSpot(
                    id = b.item.id,
                    centerX = cx,
                    centerY = cy,
                    diameterPx = effectiveDiameter,
                    alpha = b.iconAlpha,
                )
            )
        }

        val headerLeft = if (::topHeaderContainer.isInitialized && ::headerPill.isInitialized) {
            (topHeaderContainer.left + headerPill.left).toFloat()
        } else 0f
        val headerTop = if (::topHeaderContainer.isInitialized && ::headerPill.isInitialized) {
            (topHeaderContainer.top + headerPill.top).toFloat()
        } else dpToPx(config.headerTopMargin.toFloat())
        val headerW = if (::headerPill.isInitialized && config.showHeaderPill) headerPill.width.toFloat() else 0f
        val headerH = if (::headerPill.isInitialized && config.showHeaderPill) headerPill.height.toFloat() else 0f

        listener(
            PreviewBlurSceneState(
                regionCenterX = lastRegionCenterX,
                regionCenterY = lastRegionCenterY,
                regionDiameterPx = lastRegionDiameterPx,
                headerLeftPx = headerLeft,
                headerTopPx = headerTop,
                headerWidthPx = headerW,
                headerHeightPx = headerH,
                backBtnLeftPx = dpToPx(18f),
                backBtnTopPx = dpToPx(46f),
                backBtnSizePx = (dpToPx(40f) - 6f).coerceAtLeast(1f),
                bubbles = spots,
            )
        )
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
