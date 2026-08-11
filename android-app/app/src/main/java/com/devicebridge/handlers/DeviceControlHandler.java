package com.devicebridge.handlers;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;
import android.hardware.camera2.CameraManager;
import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;

public class DeviceControlHandler {

    private final Context context;
    private boolean torchOn = false;
    private MediaPlayer currentPlayer;

    public DeviceControlHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "vibrate": return vibrate(payload);
            case "ring": return ring();
            case "torch": return toggleTorch();
            case "screenOn": return screenOn();
            case "screenOff": return screenOff();
            case "openApp": return openApp(payload);
            case "playAudio": return playAudio(payload);
            case "lockScreen": return lockScreen();
            default: return JsonHelper.error("Unknown device control action: " + action);
        }
    }

    private String vibrate(JsonObject payload) {
        try {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null) return JsonHelper.error("Vibrator not available");

            long duration = payload.has("duration") ? payload.get("duration").getAsLong() : 500;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(duration);
            }

            return JsonHelper.success("vibrated");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String ring() {
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return JsonHelper.error("AudioManager not available");

            int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_RING);
            am.setStreamVolume(AudioManager.STREAM_RING, maxVolume, 0);

            am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            return JsonHelper.success("ringing");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String toggleTorch() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                if (cm != null) {
                    String cameraId = cm.getCameraIdList()[0];
                    torchOn = !torchOn;
                    cm.setTorchMode(cameraId, torchOn);
                }
            }

            JsonObject resp = new JsonObject();
            resp.addProperty("on", torchOn);
            return JsonHelper.success(resp);
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String screenOn() {
        try {
            android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.os.PowerManager.WakeLock wl = pm.newWakeLock(
                        android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "DeviceBridge::ScreenOn");
                wl.acquire(3000);
                wl.release();
            }
            return JsonHelper.success("screen on");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String screenOff() {
        try {
            Class<?> devicePolicyManager = Class.forName("android.app.admin.DevicePolicyManager");
            android.app.admin.DevicePolicyManager dpm =
                    (android.app.admin.DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                dpm.lockNow();
            }
            return JsonHelper.success("screen off");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String openApp(JsonObject payload) {
        try {
            String packageName = payload.has("package") ? payload.get("package").getAsString() : null;
            if (packageName == null) return JsonHelper.error("package name required");

            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return JsonHelper.success("opened");
            }
            return JsonHelper.error("app not found");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String playAudio(JsonObject payload) {
        try {
            String url = payload.has("url") ? payload.get("url").getAsString() : null;
            if (url == null) return JsonHelper.error("url required");

            // Release previous player if any
            releaseCurrentPlayer();

            currentPlayer = new MediaPlayer();
            currentPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            currentPlayer.setDataSource(url);
            currentPlayer.setOnCompletionListener(mp -> releaseCurrentPlayer());
            currentPlayer.setOnErrorListener((mp, what, extra) -> {
                releaseCurrentPlayer();
                return true;
            });
            currentPlayer.prepare();
            currentPlayer.start();

            return JsonHelper.success("playing");
        } catch (Exception e) {
            releaseCurrentPlayer();
            return JsonHelper.error(e.getMessage());
        }
    }

    private void releaseCurrentPlayer() {
        if (currentPlayer != null) {
            try {
                if (currentPlayer.isPlaying()) currentPlayer.stop();
                currentPlayer.release();
            } catch (Exception ignored) {}
            currentPlayer = null;
        }
    }

    private String lockScreen() {
        try {
            android.app.admin.DevicePolicyManager dpm =
                    (android.app.admin.DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (dpm != null) {
                dpm.lockNow();
                return JsonHelper.success("locked");
            }
            return JsonHelper.error("DevicePolicyManager not available");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }
}
