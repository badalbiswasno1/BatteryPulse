package com.badal.batterypulse;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class BatteryTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(android.app.PendingIntent.getActivity(
                this, 0, intent,
                android.app.PendingIntent.FLAG_IMMUTABLE));
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = registerReceiver(null, filter);

        if (battery != null) {
            int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int pct = (int) ((level / (float) scale) * 100);

            tile.setLabel("Battery: " + pct + "%");
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
        }
    }
}
