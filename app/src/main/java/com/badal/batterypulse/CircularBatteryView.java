package com.badal.batterypulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircularBatteryView extends View {

    private Paint bgPaint, progressPaint, textPaint, subTextPaint;
    private RectF rectF = new RectF();
    private int percentage = 0;
    private int progressColor = Color.parseColor("#22C55E");
    private String centerText = "0%";
    private String subText = "";

    public CircularBatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(30f);
        bgPaint.setColor(Color.parseColor("#1E1E1E"));

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(30f);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(95f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(Color.parseColor("#9E9E9E"));
        subTextPaint.setTextSize(34f);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int pct, int color, String sub) {
        this.percentage = pct;
        this.progressColor = color;
        this.centerText = pct + "%";
        this.subText = sub;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float strokeWidth = 30f;
        float padding = strokeWidth / 2 + 10;
        rectF.set(padding, padding, w - padding, h - padding);

        canvas.drawArc(rectF, 0, 360, false, bgPaint);
        progressPaint.setColor(progressColor);
        float sweep = (percentage / 100f) * 360f;
        canvas.drawArc(rectF, -90, sweep, false, progressPaint);

        float cx = w / 2f;
        float cy = h / 2f;
        canvas.drawText(centerText, cx, cy - 5, textPaint);
        canvas.drawText(subText, cx, cy + 55, subTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
        setMeasuredDimension(size, size);
    }
}
