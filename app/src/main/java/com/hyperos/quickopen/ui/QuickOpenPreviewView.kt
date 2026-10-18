package com.hyperos.quickopen.ui

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.hyperos.quickopen.config.QuickOpenConfig
import com.hyperos.quickopen.factory.HyperOSMaterialFactory
import com.hyperos.quickopen.layout.CircularHoneycombPacking
import com.hyperos.quickopen.model.ShortcutItem
import com.hyperos.quickopen.view.CircularBubbleView
import com.hyperos.quickopen.view.CircularRegionBackdropView

/**
 * 整体指纹快捷面板高保真预览与长按拖拽交互视图。
 * 复用与 SystemUI 完全一致的 CircularBubbleView、CircularRegionBackdropView 与 HeaderPill 渲染，
 * 确保所见即所得。
 * - 普通滑动：模拟指纹解锁后手指滑过快捷图标的鱼眼动力学吸附与实时聚焦反馈；
 * - 长按图标：震动进入自定义排版模式，可自由拖拽改变图标在蜂窝阵列中的位置顺序；
 * - 松手磁吸：其他图标平滑让位，松手后磁吸回网格并同步保存。
 */
class QuickOpenPreviewView(
    context: Context,
    private val config: QuickOpenConfig,
    private val onBackClick: () -> Unit,
    private val onOrderChanged: (List<ShortcutItem>) -> Unit,
) : FrameLayout(context) {

    private val resolvedStyle = HyperOSMaterialFactory.resolveActiveStyle(context, config.materialStyle)
    private var vibrator: Vibrator? = null

    private lateinit var headerPill: LinearLayout
    private lateinit var headerTitle: TextView
    private lateinit var headerSubtitle: TextView
    private lateinit var editBanner: TextView

    private lateinit var regionBackdrop: CircularRegionBackdropView
    private lateinit var bubblesContainer: FrameLayout
    private val bubbleViews = mutableListOf<CircularBubbleView>()
    private val activeShortcuts = mutableListOf<ShortcutItem>()
    private val gridPoints = mutableListOf<PointF>()

    private var currentSelectedBubble: CircularBubbleView? = null

    // 拖拽排版相关状态
    var isEditMode: Boolean = false
        private set
    private var draggingBubble: CircularBubbleView? = null
    private var dragTouchStartX = 0f
    private var dragTouchStartY = 0f
    private var isDraggingActive = false
    private val longPressRunnable = Runnable {
        enterEditMode(pendingPressBubble)
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

    private fun triggerHaptic(duration: Long = 12) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
        } catch (_: Throwable) {}
    }

    private fun initViews() {
        clipChildren = false
        clipToPadding = false

        // 1. 顶部提示胶囊与编辑提示
        buildHeader()

        // 2. 蜂窝圆形区域底衬
        regionBackdrop = CircularRegionBackdropView(context)
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
    }

    private var floatingBackButton: FrameLayout? = null

    private fun buildFloatingBackButton() {
        val btnSize = dpToPx(40f).toInt()
        val backBtn = FrameLayout(context).apply {
            background = HyperOSMaterialFactory.createCircularBubbleDrawable(resolvedStyle, 0xFFFFFFFF.toInt(), false)
            elevation = dpToPx(8f)
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
            val arrowDrawable = com.hyperos.quickopen.view.IconGlyphFactory.createGlyph("arrow_left", 0xFFFFFFFF.toInt())
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

    private fun buildHeader() {
        val topContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        headerPill = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val padH = dpToPx(24f).toInt()
            val padV = dpToPx(10f).toInt()
            setPadding(padH, padV, padH, padV)
            background = HyperOSMaterialFactory.createHeaderPillDrawable(resolvedStyle)
        }

        headerTitle = TextView(context).apply {
            text = "滑动选择快捷方式"
            textSize = 15f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        headerSubtitle = TextView(context).apply {
            text = "长按图标编辑位置 · 松手直接打开"
            textSize = 11.5f
            setTextColor(Color.argb(190, 255, 255, 255))
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

        topContainer.addView(headerPill, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        val bannerLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dpToPx(8f).toInt()
        }
        topContainer.addView(editBanner, bannerLp)

        val topLp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = dpToPx(42f).toInt()
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        }
        addView(topContainer, topLp)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            setupLayout(w, h)
        }
    }

    /**
     * 重建蜂窝网格布局并绑定气泡
     */
    fun setupLayout(w: Int = width, h: Int = height) {
        val targetW = if (w > 0) w else resources.displayMetrics.widthPixels
        val targetH = if (h > 0) h else resources.displayMetrics.heightPixels
        if (targetW <= 0 || targetH <= 0) return

        bubblesContainer.removeAllViews()
        bubbleViews.clear()
        activeShortcuts.clear()
        gridPoints.clear()
        currentSelectedBubble = null

        val fodX = targetW / 2f
        val fodY = targetH - dpToPx(60f)
        val regionCenterY = fodY - dpToPx(config.offsetY.toFloat())
        val regionDiameterPx = dpToPx(config.regionDiameter.toFloat())
        val regionRadiusPx = regionDiameterPx / 2f
        val iconSizePx = dpToPx(config.iconSize.toFloat())

        // 定位底衬
        regionBackdrop.visibility = if (config.showRegionBg) View.VISIBLE else View.GONE
        val rLp = LayoutParams(regionDiameterPx.toInt(), regionDiameterPx.toInt()).apply {
            leftMargin = (fodX - regionRadiusPx).toInt()
            topMargin = (regionCenterY - regionRadiusPx).toInt()
        }
        regionBackdrop.layoutParams = rLp

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

        // 实例化气泡
        for (i in 0 until points.size) {
            val item = activeShortcuts[i]
            val pt = points[i]

            val bubble = CircularBubbleView(
                context, item, resolvedStyle, iconSizePx.toInt(), context.classLoader, config
            )
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

        // 强制气泡及图标进行初次绘制刷新，杜绝黑屏或未更新状态
        bubblesContainer.requestLayout()
        bubblesContainer.invalidate()
        for (b in bubbleViews) {
            b.loadItemIcon()
        }

        // 播放弹射散开动效
        playExpandAnimation(fodX, fodY)
    }

    private fun playExpandAnimation(fodX: Float, fodY: Float) {
        for (i in 0 until bubbleViews.size) {
            val b = bubbleViews[i]
            val startX = fodX - b.targetX
            val startY = fodY - b.targetY

            b.translationX = startX
            b.translationY = startY
            b.scaleX = 0.1f
            b.scaleY = 0.1f
            b.alpha = 0f

            b.animate()
                .translationX(0f)
                .translationY(0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setInterpolator(OvershootInterpolator(1.2f))
                .setDuration(320)
                .setStartDelay(i * 15L)
                .start()
        }
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
                dragTouchStartX = x
                dragTouchStartY = y
                pendingPressBubble = findTouchedBubble(x, y)

                if (pendingPressBubble != null) {
                    if (isEditMode) {
                        // 编辑模式下直接抓取拖拽
                        startDragging(pendingPressBubble!!, x, y)
                    } else {
                        // 准备长按识别 (450ms 进入自定义排版)
                        handler?.postDelayed(longPressRunnable, 450)
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val distFromDown = Math.hypot((x - dragTouchStartX).toDouble(), (y - dragTouchStartY).toDouble()).toFloat()
                if (distFromDown > dpToPx(10f) && !isDraggingActive) {
                    handler?.removeCallbacks(longPressRunnable)
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
                } else if (!isEditMode) {
                    // 普通模拟指纹滑动模式：物理鱼眼动力学
                    handleFingerHover(x, y)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler?.removeCallbacks(longPressRunnable)

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
        val threshold = dpToPx(config.iconSize.toFloat()) * 0.7f
        for (b in bubbleViews) {
            val dist = Math.hypot((x - b.targetX).toDouble(), (y - b.targetY).toDouble()).toFloat()
            if (dist <= threshold) {
                return b
            }
        }
        return null
    }

    /**
     * 进入自定义排版模式
     */
    fun enterEditMode(initialTarget: CircularBubbleView? = null) {
        if (isEditMode) return
        isEditMode = true
        triggerHaptic(40)

        editBanner.visibility = View.VISIBLE
        editBanner.alpha = 0f
        editBanner.animate().alpha(1f).setDuration(200).start()

        headerTitle.text = "自定义排版模式"
        headerTitle.setTextColor(0xFFFF9933.toInt())
        headerSubtitle.text = "长按并拖动图标交换位置，点击空白完成"

        // 所有气泡进入轻微呼吸抖动
        for (b in bubbleViews) {
            b.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(150)
                .start()
        }

        if (initialTarget != null) {
            startDragging(initialTarget, dragTouchStartX, dragTouchStartY)
        }
    }

    fun exitEditMode() {
        if (!isEditMode) return
        isEditMode = false
        triggerHaptic(20)

        editBanner.animate().alpha(0f).setDuration(150).withEndAction {
            editBanner.visibility = View.GONE
        }.start()

        headerTitle.text = "滑动选择快捷方式"
        headerTitle.setTextColor(Color.WHITE)
        headerSubtitle.text = "移至目标图标后松手直接打开"

        for (b in bubbleViews) {
            b.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(150)
                .start()
        }

        // 同步通知外部回写最新排序列表
        val newOrdered = bubbleViews.map { it.item }
        onOrderChanged(newOrdered)
    }

    private fun startDragging(bubble: CircularBubbleView, touchX: Float, touchY: Float) {
        isDraggingActive = true
        draggingBubble = bubble
        bubble.bringToFront()
        bubble.translationZ = 40f
        bubble.animate()
            .scaleX(1.22f)
            .scaleY(1.22f)
            .setDuration(100)
            .start()
        triggerHaptic(25)
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
                        .withEndAction {
                            lp.leftMargin = targetLeft
                            lp.topMargin = targetTop
                            b.layoutParams = lp
                            b.translationX = 0f
                            b.translationY = 0f
                        }
                        .start()
                }
            }
            triggerHaptic(15)
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

            b.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(160)
                .start()

            val lp = b.layoutParams as LayoutParams
            lp.leftMargin = targetLeft
            lp.topMargin = targetTop
            b.layoutParams = lp
            b.translationZ = 0f
        }
        draggingBubble = null
        triggerHaptic(18)
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
            if (dist < influenceRadiusPx) {
                val factor = Math.cos((dist / influenceRadiusPx) * (Math.PI / 2.0)).toFloat()
                scale = 1.0f + (config.maxScale - 1.0f) * factor
            }
            b.applyScale(scale)
        }

        if (closest != null && minDistance < hitDistanceThreshold) {
            if (currentSelectedBubble != closest) {
                currentSelectedBubble?.setSelectedState(false)
                currentSelectedBubble?.translationZ = 0f

                closest.setSelectedState(true)
                closest.bringToFront()
                closest.translationZ = 20f
                currentSelectedBubble = closest

                headerTitle.text = closest.item.title
                headerTitle.setTextColor(closest.item.tintColor)
                headerSubtitle.text = closest.item.subtitle
                triggerHaptic(12)
            }
        } else {
            if (currentSelectedBubble != null) {
                currentSelectedBubble?.setSelectedState(false)
                currentSelectedBubble?.translationZ = 0f
                currentSelectedBubble = null
                headerTitle.text = "滑动选择快捷方式"
                headerTitle.setTextColor(Color.WHITE)
                headerSubtitle.text = "移至目标图标后松手直接打开"
            }
        }
    }

    private fun resetHoverSelection() {
        currentSelectedBubble?.setSelectedState(false)
        currentSelectedBubble?.translationZ = 0f
        currentSelectedBubble = null
        for (b in bubbleViews) {
            b.applyScale(1.0f)
        }
        headerTitle.text = "滑动选择快捷方式"
        headerTitle.setTextColor(Color.WHITE)
        headerSubtitle.text = "移至目标图标后松手直接打开"
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
