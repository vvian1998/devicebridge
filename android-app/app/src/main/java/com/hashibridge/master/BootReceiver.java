package com.hashibridge.master;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        String relayUrl = com.hashibridge.master.utils.Config.getRelayUrl(context);
        if (relayUrl.isEmpty()) {
            relayUrl = context.getString(R.string.default_relay_url);
        }
        String deviceId = com.hashibridge.master.utils.Config.getOrCreateDeviceId(context);

        if (relayUrl.isEmpty() || deviceId.isEmpty()) {
            return;
        }

        com.hashibridge.master.utils.Config.saveConfig(context, relayUrl, deviceId);

        Intent service = new Intent(context, BridgeService.class);
        service.putExtra("relayUrl", relayUrl);
        service.putExtra("deviceId", deviceId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
        ServiceWatchdog.schedule(context);
    }
}
