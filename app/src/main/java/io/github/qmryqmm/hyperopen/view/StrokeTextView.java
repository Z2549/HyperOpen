package io.github.qmryqmm.hyperopen.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.TextView;

/**
 * 支持「填充 + 对比描边」双层文字的 TextView。
 *
 * <p>单个 Paint 无法让描边与填充异色（Skia 文字绘制的描边色 = Paint 当前颜色），
 * 因此在 onDraw 中分两遍绘制：第一遍切换为 STROKE 样式 + 描边色画轮廓，
 * 还原后第二遍由 super 绘制正常填充。strokePx &lt;= 0 时完全走 super 单遍绘制，
 * 与普通 TextView 零差异（硬件加速下无额外开销）。
 *
 * <p>SystemUI 侧 CustomHyperOpenContainer 与模块 App 侧 HyperOpenPreviewView
 * 的提示胶囊标题/副标题共用本类，由 {@link HintTextStyler} 统一驱动。
 */
public class StrokeTextView extends TextView {

    /** 描边半宽（px），0 = 关闭描边 */
    private float strokePx = 0f;
    /** 描边颜色（通常深色，保证亮色文字在浅色壁纸上可读） */
    private int strokeColor = 0xB3000000;

    public StrokeTextView(Context context) {
        super(context);
    }

    /** 设置描边宽度（px，双向外扩）与颜色；width &lt;= 0 关闭描边 */
    public void setStroke(float widthPx, int color) {
        if (this.strokePx != widthPx || this.strokeColor != color) {
            this.strokePx = widthPx;
            this.strokeColor = color;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (strokePx <= 0f) {
            super.onDraw(canvas);
            return;
        }
        Paint p = getPaint();
        int oldColor = p.getColor();
        Paint.Style oldStyle = p.getStyle();
        float oldStrokeWidth = p.getStrokeWidth();
        Paint.Join oldJoin = p.getStrokeJoin();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokePx * 2f);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(strokeColor);
        super.onDraw(canvas);

        p.setColor(oldColor);
        p.setStyle(oldStyle);
        p.setStrokeWidth(oldStrokeWidth);
        p.setStrokeJoin(oldJoin);
        super.onDraw(canvas);
    }
}
