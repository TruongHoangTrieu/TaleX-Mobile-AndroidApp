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

    private Paint backgroundPaint;
    private Paint transparentPaint;
    private Paint borderPaint;
    private Paint dotPaint; // Cọ vẽ các dấu chấm công nghệ
    private RectF ovalRect;

    // Định nghĩa các mã màu viền
    private final int COLOR_DEFAULT = Color.parseColor("#D4AF37"); // Vàng Gold mặc định
    private final int COLOR_SUCCESS = Color.parseColor("#4CAF50"); // Xanh lá khi đang quét
    private final int COLOR_ERROR = Color.parseColor("#F44336");   // Đỏ khi mặt bị lệch

    public FaceOverlayView(Context context) {
        super(context);
        init();
    }

    public FaceOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Tắt tăng tốc phần cứng để PorterDuff.Mode.CLEAR đục lỗ xuyên thấu chính xác
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // 1. Cọ vẽ nền đen đặc (Chuẩn tông màu Web)
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#121212"));
        backgroundPaint.setStyle(Paint.Style.FILL);

        // 2. Cọ tàng hình (Dùng để đục lỗ Ovan)
        transparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // 3. Cọ vẽ viền đứt đoạn (Đường cong)
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(COLOR_DEFAULT);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(10f); // Độ dày nét cong
        borderPaint.setStrokeCap(Paint.Cap.ROUND); // Bo tròn đầu nét vẽ

        // 4. Cọ vẽ các chấm tròn nhỏ
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(COLOR_DEFAULT);
        dotPaint.setStyle(Paint.Style.FILL);

        ovalRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float density = getResources().getDisplayMetrics().density;

        // Kích thước lỗ Ovan chuẩn
        float ovalWidth = 260 * density;
        float ovalHeight = 340 * density;

        float left = (w - ovalWidth) / 2f;
        // Đẩy Ovan lên trên 40dp để có khoảng trống cho dòng chữ hướng dẫn
        float top = (h - ovalHeight) / 2f - (40 * density);

        ovalRect.set(left, top, left + ovalWidth, top + ovalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Bước 1: Phủ màu nền đen tuyền lên toàn bộ màn hình
        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        // Bước 2: Dùng cọ tàng hình khoét một lỗ Ovan ở giữa để lộ Camera
        canvas.drawOval(ovalRect, transparentPaint);

        // Bước 3: Họa tiết chuẩn VNPT AI (Vẽ 4 đoạn cong bao quanh)
        // Mỗi đoạn cong có độ dài 40 độ
        float sweepAngle = 40f;

        // Đoạn bên Phải (Tâm 0 độ, lùi về -20)
        canvas.drawArc(ovalRect, -20f, sweepAngle, false, borderPaint);
        // Đoạn bên Dưới (Tâm 90 độ, lùi về 70)
        canvas.drawArc(ovalRect, 70f, sweepAngle, false, borderPaint);
        // Đoạn bên Trái (Tâm 180 độ, lùi về 160)
        canvas.drawArc(ovalRect, 160f, sweepAngle, false, borderPaint);
        // Đoạn bên Trên (Tâm 270 độ, lùi về 250)
        canvas.drawArc(ovalRect, 250f, sweepAngle, false, borderPaint);

        // Bước 4: Họa tiết chuẩn VNPT AI (Vẽ 4 dấu chấm ở 4 góc chéo)
        // Góc: 45, 135, 225, 315 độ
        float dotRadius = 12f; // Kích thước hạt chấm
        drawDotOnEllipse(canvas, 45f, dotRadius);
        drawDotOnEllipse(canvas, 135f, dotRadius);
        drawDotOnEllipse(canvas, 225f, dotRadius);
        drawDotOnEllipse(canvas, 315f, dotRadius);
    }

    /**
     * Hàm toán học tính toán tọa độ chính xác của 1 điểm trên đường elip
     * dựa vào góc nghiêng, sau đó vẽ dấu chấm tại tọa độ đó.
     */
    private void drawDotOnEllipse(Canvas canvas, float angleDegrees, float radius) {
        float angleRadians = (float) Math.toRadians(angleDegrees);
        float a = ovalRect.width() / 2f;  // Bán trục lớn
        float b = ovalRect.height() / 2f; // Bán trục nhỏ

        // Phương trình tham số của Elip: x = a*cos(t), y = b*sin(t)
        float cx = ovalRect.centerX() + a * (float) Math.cos(angleRadians);
        float cy = ovalRect.centerY() + b * (float) Math.sin(angleRadians);

        canvas.drawCircle(cx, cy, radius, dotPaint);
    }

    /**
     * Đổi màu đồng bộ cả Viền cong và Dấu chấm
     */
    public void setBorderState(OverlayState state) {
        int targetColor;
        switch (state) {
            case SUCCESS:
                targetColor = COLOR_SUCCESS;
                break;
            case ERROR:
                targetColor = COLOR_ERROR;
                break;
            case DEFAULT:
            default:
                targetColor = COLOR_DEFAULT;
                break;
        }
        borderPaint.setColor(targetColor);
        dotPaint.setColor(targetColor);

        invalidate(); // Yêu cầu vẽ lại giao diện lập tức
    }

    public enum OverlayState {
        DEFAULT, // Vàng: Đang chờ quét
        SUCCESS, // Xanh lá: Mặt chuẩn, đang đếm ngược quay
        ERROR    // Đỏ: Lệch mặt, rớt mặt ra ngoài
    }
}