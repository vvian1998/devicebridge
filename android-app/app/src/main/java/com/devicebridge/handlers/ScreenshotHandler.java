package com.devicebridge.handlers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ScreenshotHandler {

    private final Context context;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;

    public ScreenshotHandler(Context context) {
        this.context = context;
        this.projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "capture": return captureScreenshot();
            default: return JsonHelper.error("Unknown screenshot action: " + action);
        }
    }

    private String captureScreenshot() {
        try {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;

            if (projectionManager == null) {
                return JsonHelper.error("MediaProjectionManager not available");
            }

            Intent intent = projectionManager.createScreenCaptureIntent();

            if (mediaProjection == null) {
                return JsonHelper.error("Screen capture permission required. Call from activity with result.");
            }

            final CountDownLatch latch = new CountDownLatch(1);
            final Bitmap[] result = new Bitmap[1];

            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(() -> {
                try {
                    ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
                    VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                            "ScreenshotCapture", width, height, density,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                            imageReader.getSurface(), null, null
                    );

                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * width;

                        Bitmap bitmap = Bitmap.createBitmap(
                                width + rowPadding / pixelStride, height,
                                Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);
                        result[0] = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                        bitmap.recycle();
                        image.close();
                    }
                    imageReader.close();
                    virtualDisplay.release();
                } catch (Exception e) {
                    android.util.Log.e("ScreenshotHandler", "Capture failed", e);
                }
                latch.countDown();
            });

            latch.await(3, TimeUnit.SECONDS);

            if (result[0] != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                result[0].compress(Bitmap.CompressFormat.PNG, 80, baos);
                result[0].recycle();
                byte[] bytes = baos.toByteArray();

                JsonObject resp = new JsonObject();
                resp.addProperty("base64", Base64.getEncoder().encodeToString(bytes));
                resp.addProperty("width", width);
                resp.addProperty("height", height);
                return JsonHelper.success(resp);
            }

            return fallbackScreencap();

        } catch (Exception e) {
            return fallbackScreencap();
        }
    }

    private String fallbackScreencap() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"screencap", "-p"});
            java.io.InputStream is = process.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            process.waitFor();

            JsonObject resp = new JsonObject();
            resp.addProperty("base64", Base64.getEncoder().encodeToString(baos.toByteArray()));
            return JsonHelper.success(resp);
        } catch (Exception e) {
            return JsonHelper.error("Screenshot failed: " + e.getMessage());
        }
    }
}
