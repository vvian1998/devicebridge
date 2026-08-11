package com.devicebridge.utils;

import android.graphics.Bitmap;
import android.util.LruCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ThumbnailCache {

    private static final int MEM_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8);
    private static final LruCache<String, Bitmap> memCache = new LruCache<String, Bitmap>(MEM_CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    private static File diskCacheDir;
    private static boolean diskCacheReady = false;

    public static void init(File cacheDir) {
        diskCacheDir = new File(cacheDir, "thumbnails");
        if (!diskCacheDir.exists()) {
            diskCacheReady = diskCacheDir.mkdirs();
        } else {
            diskCacheReady = true;
        }
    }

    public static Bitmap get(String key) {
        Bitmap cached = memCache.get(key);
        if (cached != null) return cached;

        if (diskCacheReady) {
            File file = new File(diskCacheDir, hashKey(key));
            if (file.exists()) {
                try {
                    FileInputStream fis = new FileInputStream(file);
                    Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(fis);
                    fis.close();
                    if (bitmap != null) {
                        memCache.put(key, bitmap);
                    }
                    return bitmap;
                } catch (IOException e) {
                    android.util.Log.w("ThumbnailCache", "Failed to read disk cache", e);
                }
            }
        }
        return null;
    }

    public static void put(String key, Bitmap bitmap) {
        if (bitmap == null) return;
        memCache.put(key, bitmap);
        if (diskCacheReady) {
            try {
                File file = new File(diskCacheDir, hashKey(key));
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
                fos.close();
            } catch (IOException e) {
                android.util.Log.w("ThumbnailCache", "Failed to write disk cache", e);
            }
        }
    }

    public static void clear() {
        memCache.evictAll();
        if (diskCacheDir != null && diskCacheDir.exists()) {
            File[] files = diskCacheDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
    }

    private static String hashKey(String key) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(digest.length, 16); i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString() + ".jpg";
        } catch (Exception e) {
            // Fallback to hashCode if SHA-256 unavailable
            return Integer.toHexString(key.hashCode()) + ".jpg";
        }
    }
}
