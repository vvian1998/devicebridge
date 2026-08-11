package com.devicebridge.handlers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public class CameraHandler {

    private final Context context;
    private Camera camera;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    public CameraHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "capture": return capture();
            case "toggle": return toggleCamera();
            default: return JsonHelper.error("Unknown camera action: " + action);
        }
    }

    private synchronized String capture() {
        try {
            releaseCamera();
            camera = Camera.open(currentCameraId);

            Camera.Parameters params = camera.getParameters();
            Camera.Size previewSize = params.getPreviewSize();

            camera.setPreviewTexture(new SurfaceTexture(0));

            final CountDownLatch latch = new CountDownLatch(1);
            final Bitmap[] result = new Bitmap[1];

            camera.setPreviewCallback((data, camera) -> {
                if (data != null) {
                    Camera.Parameters p = camera.getParameters();
                    Camera.Size size = p.getPreviewSize();
                    YuvImage yuv = new YuvImage(data, ImageFormat.NV21,
                            size.width, size.height, null);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    yuv.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, out);
                    byte[] jpegData = out.toByteArray();
                    result[0] = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
                }
                latch.countDown();
            });

            camera.startPreview();

            latch.await(3, TimeUnit.SECONDS);

            camera.stopPreview();
            releaseCamera();

            if (result[0] != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                result[0].compress(Bitmap.CompressFormat.JPEG, 80, baos);
                result[0].recycle();

                JsonObject resp = new JsonObject();
                resp.addProperty("base64", Base64.getEncoder().encodeToString(baos.toByteArray()));
                return JsonHelper.success(resp);
            }

            return JsonHelper.error("No image captured");
        } catch (Exception e) {
            releaseCamera();
            return JsonHelper.error("Camera error: " + e.getMessage());
        }
    }

    private String toggleCamera() {
        int numCameras = Camera.getNumberOfCameras();
        currentCameraId = (currentCameraId + 1) % numCameras;
        return JsonHelper.success("Camera switched to " + currentCameraId);
    }

    private void releaseCamera() {
        if (camera != null) {
            try {
                camera.release();
            } catch (Exception ignored) {}
            camera = null;
        }
    }
}
