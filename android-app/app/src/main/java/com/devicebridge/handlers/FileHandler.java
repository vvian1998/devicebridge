package com.devicebridge.handlers;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;

public class FileHandler {

    private final Context context;

    public FileHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "list": return listDir(payload);
            case "download": return downloadFile(payload);
            case "delete": return deleteFile(payload);
            case "upload": return uploadFile(payload);
            case "mkdir": return mkdir(payload);
            case "search": return searchFiles(payload);
            default: return JsonHelper.error("Unknown file action: " + action);
        }
    }

    private String listDir(JsonObject payload) {
        String path = payload.has("path") ? payload.get("path").getAsString() : "/sdcard/";
        File dir = new File(path);

        JsonObject result = new JsonObject();
        JsonArray files = new JsonArray();

        if (!dir.exists() || !dir.isDirectory()) {
            result.add("files", files);
            return JsonHelper.success(result);
        }

        File[] list = dir.listFiles();
        if (list != null) {
            Arrays.sort(list, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File f : list) {
                JsonObject fileObj = new JsonObject();
                fileObj.addProperty("name", f.getName());
                fileObj.addProperty("path", f.getAbsolutePath());
                fileObj.addProperty("isDir", f.isDirectory());
                fileObj.addProperty("size", f.isFile() ? f.length() : 0);
                fileObj.addProperty("modified", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                        .format(new java.util.Date(f.lastModified())));
                if (!f.isDirectory()) {
                    String[] meta = detectType(f.getName());
                    fileObj.addProperty("type", meta[0]);
                    fileObj.addProperty("icon", meta[1]);
                }
                files.add(fileObj);
            }
        }

        result.add("files", files);
        return JsonHelper.success(result);
    }

    private String downloadFile(JsonObject payload) {
        String path = payload.has("path") ? payload.get("path").getAsString() : null;
        if (path == null) return JsonHelper.error("path required");

        File file = new File(path);
        if (!file.exists() || !file.isFile()) return JsonHelper.error("file not found");

        long maxSize = 10 * 1024 * 1024L; // 10MB
        if (file.length() > maxSize) {
            return JsonHelper.error("File too large for base64 download (" + (file.length() / (1024*1024)) + "MB). Use /stream/file endpoint.");
        }

        try {
            byte[] data;
            try (FileInputStream fis = new FileInputStream(file)) {
                data = new byte[(int) file.length()];
                int totalRead = 0;
                int bytesRead;
                while (totalRead < data.length && (bytesRead = fis.read(data, totalRead, data.length - totalRead)) != -1) {
                    totalRead += bytesRead;
                }
            }

            JsonObject result = new JsonObject();
            result.addProperty("base64", Base64.getEncoder().encodeToString(data));
            result.addProperty("fileName", file.getName());
            result.addProperty("size", file.length());

            String mime = android.webkit.MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(android.webkit.MimeTypeMap.getFileExtensionFromUrl(path));
            result.addProperty("mimeType", mime != null ? mime : "application/octet-stream");

            return JsonHelper.success(result);
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String deleteFile(JsonObject payload) {
        String path = payload.has("path") ? payload.get("path").getAsString() : null;
        if (path == null) return JsonHelper.error("path required");

        File file = new File(path);
        if (!file.exists()) return JsonHelper.error("file not found");

        boolean deleted = deleteRecursive(file);
        return deleted ? JsonHelper.success("deleted") : JsonHelper.error("failed to delete");
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    private String uploadFile(JsonObject payload) {
        String path = payload.has("path") ? payload.get("path").getAsString() : null;
        String base64Data = payload.has("data") ? payload.get("data").getAsString() : null;

        if (path == null || base64Data == null) return JsonHelper.error("path and data required");

        try {
            byte[] data = Base64.getDecoder().decode(base64Data);
            File target = new File(path);
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            FileOutputStream fos = new FileOutputStream(target);
            fos.write(data);
            fos.close();

            return JsonHelper.success("uploaded");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String mkdir(JsonObject payload) {
        String path = payload.has("path") ? payload.get("path").getAsString() : null;
        if (path == null) return JsonHelper.error("path required");

        File dir = new File(path);
        if (dir.exists()) return JsonHelper.error("already exists");

        boolean created = dir.mkdirs();
        return created ? JsonHelper.success("created") : JsonHelper.error("failed to create");
    }

    private String searchFiles(JsonObject payload) {
        String searchPath = payload.has("path") ? payload.get("path").getAsString() : "/sdcard/";
        String query = payload.has("query") ? payload.get("query").getAsString() : "";

        JsonObject result = new JsonObject();
        JsonArray files = new JsonArray();

        File dir = new File(searchPath);
        if (dir.exists() && dir.isDirectory()) {
            searchRecursive(dir, query.toLowerCase(), files, 100);
        }

        result.add("files", files);
        return JsonHelper.success(result);
    }

    private void searchRecursive(File dir, String query, JsonArray results, int maxResults) {
        if (results.size() >= maxResults) return;
        File[] list = dir.listFiles();
        if (list == null) return;

        for (File f : list) {
            if (results.size() >= maxResults) return;
            if (f.getName().toLowerCase().contains(query)) {
                JsonObject fileObj = new JsonObject();
                fileObj.addProperty("name", f.getName());
                fileObj.addProperty("path", f.getAbsolutePath());
                fileObj.addProperty("isDir", f.isDirectory());
                fileObj.addProperty("size", f.isFile() ? f.length() : 0);
                results.add(fileObj);
            }
            if (f.isDirectory()) {
                searchRecursive(f, query, results, maxResults);
            }
        }
    }

    private String[] detectType(String name) {
        String lower = name.toLowerCase();

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")) {
            return new String[]{"image", "🖼"};
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi")
                || lower.endsWith(".3gp") || lower.endsWith(".webm") || lower.endsWith(".mov")) {
            return new String[]{"video", "🎬"};
        }
        if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".aac")
                || lower.endsWith(".flac") || lower.endsWith(".ogg") || lower.endsWith(".m4a")) {
            return new String[]{"audio", "🎵"};
        }
        if (lower.endsWith(".pdf")) return new String[]{"document", "📄"};
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return new String[]{"document", "📝"};
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return new String[]{"document", "📊"};
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return new String[]{"document", "📽"};
        if (lower.endsWith(".apk")) return new String[]{"apk", "📱"};
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z")
                || lower.endsWith(".tar") || lower.endsWith(".gz")) {
            return new String[]{"archive", "📦"};
        }
        if (lower.endsWith(".txt") || lower.endsWith(".log") || lower.endsWith(".md")) {
            return new String[]{"text", "📃"};
        }
        if (lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".csv")) {
            return new String[]{"code", "📋"};
        }
        return new String[]{"file", "📄"};
    }
}
