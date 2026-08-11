package com.devicebridge.handlers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;
import java.io.ByteArrayOutputStream;

public class MediaHandler {

    private final Context context;

    public MediaHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "list": return listMedia(payload);
            default: return JsonHelper.error("Unknown media action: " + action);
        }
    }

    private String listMedia(JsonObject payload) {
        String type = payload.has("type") ? payload.get("type").getAsString() : "images";
        int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 40;
        String cursor = payload.has("cursor") ? payload.get("cursor").getAsString() : null;

        JsonObject result = new JsonObject();
        JsonArray items = new JsonArray();

        Uri uri;
        String[] projection;
        String selection = null;

        switch (type) {
            case "videos":
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                projection = new String[]{
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media.SIZE
                };
                break;
            case "all":
                return listAllMedia(payload);
            default:
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                projection = new String[]{
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.DATA,
                        MediaStore.Images.Media.DATE_ADDED,
                        MediaStore.Images.Media.SIZE
                };
                break;
        }

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
        if (cursor != null) {
            selection = MediaStore.Images.Media.DATE_ADDED + " < ?";
        }

        Cursor c = context.getContentResolver().query(uri, projection, selection,
                cursor != null ? new String[]{cursor} : null, sortOrder + " LIMIT " + limit);

        if (c != null) {
            String lastDate = null;
            while (c.moveToNext()) {
                JsonObject item = new JsonObject();
                item.addProperty("id", c.getLong(0));
                item.addProperty("name", c.getString(1));
                item.addProperty("path", c.getString(2));
                item.addProperty("dateAdded", c.getString(3));
                item.addProperty("size", c.getLong(4));
                lastDate = c.getString(3);

                boolean isVideoType = type.equals("videos");
                item.addProperty("thumbnail",
                        "/stream/thumbnail?id=" + c.getLong(0) +
                        (isVideoType ? "&type=videos" : ""));

                items.add(item);
            }
            c.close();

            if (lastDate != null && items.size() == limit) {
                result.addProperty("cursor", lastDate);
            }
        }

        result.add("items", items);
        return JsonHelper.success(result);
    }

    private String listAllMedia(JsonObject payload) {
        int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 40;
        String cursor = payload.has("cursor") ? payload.get("cursor").getAsString() : null;

        JsonObject result = new JsonObject();
        JsonArray items = new JsonArray();

        java.util.List<JsonObject> allItems = new java.util.ArrayList<>();

        for (String type : new String[]{"images", "videos"}) {
            JsonObject mediaPayload = new JsonObject();
            mediaPayload.addProperty("type", type);
            mediaPayload.addProperty("limit", limit);
            if (cursor != null) mediaPayload.addProperty("cursor", cursor);

            JsonObject typeResult = new com.google.gson.JsonParser().parse(listMedia(mediaPayload)).getAsJsonObject();
            JsonArray typeItems = typeResult.getAsJsonArray("items");
            for (int i = 0; i < typeItems.size(); i++) {
                allItems.add(typeItems.get(i).getAsJsonObject());
            }
        }

        allItems.sort((a, b) -> {
            String da = a.has("dateAdded") ? a.get("dateAdded").getAsString() : "0";
            String db = b.has("dateAdded") ? b.get("dateAdded").getAsString() : "0";
            return db.compareTo(da);
        });

        // Trim to requested limit to prevent OOM from unbounded growth
        while (allItems.size() > limit) {
            allItems.remove(allItems.size() - 1);
        }

        for (JsonObject item : allItems) {
            items.add(item);
        }

        result.add("items", items);
        return JsonHelper.success(result);
    }

    public static byte[] getThumbnailBytes(android.content.Context ctx, long mediaId, boolean isVideo) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;

            Bitmap bitmap;
            if (isVideo) {
                bitmap = android.provider.MediaStore.Video.Thumbnails.getThumbnail(
                        ctx.getContentResolver(), mediaId,
                        MediaStore.Video.Thumbnails.MINI_KIND, options);
            } else {
                bitmap = android.provider.MediaStore.Images.Thumbnails.getThumbnail(
                        ctx.getContentResolver(), mediaId,
                        MediaStore.Images.Thumbnails.MINI_KIND, options);
            }

            if (bitmap == null) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] thumbData = baos.toByteArray();
            bitmap.recycle();

            return thumbData;
        } catch (Exception e) {
            return null;
        }
    }
}
