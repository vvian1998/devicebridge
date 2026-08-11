package com.devicebridge;

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

        String relayUrl = com.devicebridge.utils.Config.getRelayUrl(context);
        String deviceId = com.devicebridge.utils.Config.getDeviceId(context);

        if (relayUrl.isEmpty() || deviceId.isEmpty()) {
            return;
        }

        Intent service = new Intent(context, BridgeService.class);
        service.putExtra("relayUrl", relayUrl);
        service.putExtra("deviceId", deviceId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}
