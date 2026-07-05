package com.badal.batterypulse;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.RemoteViews;

public class BatteryWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_battery);

            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent battery = context.registerReceiver(null, filter);

            if (battery != null) {
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int pct = (int) ((level / (float) scale) * 100);

                String statusStr = status == BatteryManager.BATTERY_STATUS_CHARGING ? "Charging" : "BatteryPulse";

                views.setTextViewText(R.id.widgetPct, pct + "%");
                views.setTextViewText(R.id.widgetStatus, statusStr);
            }

            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }
}
