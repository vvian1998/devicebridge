package com.hashibridge.master;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

public class NetworkStateReceiver extends BroadcastReceiver {

    private static final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            return;
        }

        if (BridgeService.isRunning() && isNetworkAvailable(context)) {
            handler.postDelayed(() -> {
                if (BridgeService.isRunning()) {
                    BridgeService.triggerReconnect();
                }
            }, 1500);
        }
    }

    private boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network active = cm.getActiveNetwork();
                if (active == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                return caps != null &&
                        (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                         caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            } else {
                android.net.NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
