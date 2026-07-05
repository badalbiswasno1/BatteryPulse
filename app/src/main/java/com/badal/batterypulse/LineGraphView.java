package com.badal.batterypulse;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.List;

public class LineGraphView extends View {

    private Paint linePaint, bgPaint;
    private List<Float> data;
    private int lineColor = Color.parseColor("#22C55E");

    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
        linePaint.setColor(lineColor);

        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#141414"));
    }

    public void setData(List<Float> values, int color) {
        this.data = values;
        this.lineColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();

        canvas.drawRoundRect(0, 0, w, h, 20f, 20f, bgPaint);

        if (data == null || data.size() < 2) return;

        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (float v : data) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (max == min) max = min + 1;

        linePaint.setColor(lineColor);
        Path path = new Path();
        float padding = 16f;
        float usableW = w - padding * 2;
        float usableH = h - padding * 2;
        float stepX = usableW / (data.size() - 1);

        for (int i = 0; i < data.size(); i++) {
            float x = padding + i * stepX;
            float normalized = (data.get(i) - min) / (max - min);
            float y = padding + usableH - (normalized * usableH);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }

        canvas.drawPath(path, linePaint);
    }
}
