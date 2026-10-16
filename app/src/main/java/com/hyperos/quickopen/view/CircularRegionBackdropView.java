package com.hyperos.quickopen.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

import com.hyperos.quickopen.factory.HyperOSMaterialFactory;

public class CircularRegionBackdropView extends View {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int style = HyperOSMaterialFactory.STYLE_SOFT_GLASS;

    public CircularRegionBackdropView(Context context) {
        super(context);
        init();
    }

    private void init() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f);
        borderPaint.setColor(0x33FFFFFF);
    }

    public void setStyle(int style) {
        this.style = style;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 2f;
        if (radius <= 0) return;

        // 柔和微底衬与发光边缘
        int c1 = (style == HyperOSMaterialFactory.STYLE_SOFT_GLASS) ? 0x14FFFFFF : 0x0FFFFFFF;
        RadialGradient grad = new RadialGradient(
            cx, cy, radius,
            new int[]{c1, 0x05FFFFFF, 0x00000000},
            new float[]{0f, 0.7f, 1f},
            Shader.TileMode.CLAMP
        );
        bgPaint.setShader(grad);
        canvas.drawCircle(cx, cy, radius, bgPaint);

        canvas.drawCircle(cx, cy, radius, borderPaint);
    }
}
