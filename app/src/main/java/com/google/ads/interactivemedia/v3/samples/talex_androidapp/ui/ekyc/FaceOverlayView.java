package com.google.ads.interactivemedia.v3.samples.talex_androidapp.ui.ekyc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class FaceOverlayView extends View {

    private static final int COLOR_DEFAULT = Color.parseColor("#D4AF37");
    private static final int COLOR_SUCCESS = Color.parseColor("#57E3A5");
    private static final int COLOR_ERROR = Color.parseColor("#FF5252");

    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fullBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalRect = new RectF();

    private int borderColor = COLOR_DEFAULT;

    public FaceOverlayView(Context context) {
        super(context);
        init();
    }

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        overlayPaint.setColor(Color.parseColor("#D9121212"));
        overlayPaint.setStyle(Paint.Style.FILL);

        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        fullBorderPaint.setColor(Color.parseColor("#EAF7F1D9"));
        fullBorderPaint.setStyle(Paint.Style.STROKE);
        fullBorderPaint.setStrokeWidth(dp(2.5f));

        accentPaint.setColor(borderColor);
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(dp(5f));
        accentPaint.setStrokeCap(Paint.Cap.ROUND);

        dotPaint.setColor(borderColor);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float ovalWidth = Math.min(w * 0.72f, dp(310f));
        float ovalHeight = Math.min(h * 0.58f, dp(430f));
        float centerY = h * 0.48f;

        float left = (w - ovalWidth) / 2f;
        float top = centerY - ovalHeight / 2f;
        float minTop = dp(120f);
        float maxTop = h - ovalHeight - dp(96f);
        top = Math.max(minTop, Math.min(top, maxTop));

        ovalRect.set(left, top, left + ovalWidth, top + ovalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int checkpoint = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        canvas.drawRect(0, 0, getWidth(), getHeight(), overlayPaint);
        canvas.drawOval(ovalRect, clearPaint);
        canvas.restoreToCount(checkpoint);

        canvas.drawOval(ovalRect, fullBorderPaint);

        RectF accentRect = new RectF(ovalRect);
        accentRect.inset(-dp(8f), -dp(8f));
        accentPaint.setColor(borderColor);
        dotPaint.setColor(borderColor);

        canvas.drawArc(accentRect, -18f, 42f, false, accentPaint);
        canvas.drawArc(accentRect, 66f, 48f, false, accentPaint);
        canvas.drawArc(accentRect, 156f, 48f, false, accentPaint);
        canvas.drawArc(accentRect, 246f, 48f, false, accentPaint);

        drawDotOnEllipse(canvas, accentRect, 45f);
        drawDotOnEllipse(canvas, accentRect, 135f);
        drawDotOnEllipse(canvas, accentRect, 225f);
        drawDotOnEllipse(canvas, accentRect, 315f);
    }

    private void drawDotOnEllipse(Canvas canvas, RectF rect, float angleDegrees) {
        float angleRadians = (float) Math.toRadians(angleDegrees);
        float radiusX = rect.width() / 2f;
        float radiusY = rect.height() / 2f;
        float cx = rect.centerX() + radiusX * (float) Math.cos(angleRadians);
        float cy = rect.centerY() + radiusY * (float) Math.sin(angleRadians);
        canvas.drawCircle(cx, cy, dp(4.5f), dotPaint);
    }

    public void setBorderState(OverlayState state) {
        switch (state) {
            case SUCCESS:
                borderColor = COLOR_SUCCESS;
                break;
            case ERROR:
                borderColor = COLOR_ERROR;
                break;
            case DEFAULT:
            default:
                borderColor = COLOR_DEFAULT;
                break;
        }
        invalidate();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    public enum OverlayState {
        DEFAULT,
        SUCCESS,
        ERROR
    }
}
