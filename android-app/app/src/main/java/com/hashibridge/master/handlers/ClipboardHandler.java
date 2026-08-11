package com.hashibridge.master.handlers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import com.google.gson.JsonObject;
import com.hashibridge.master.utils.JsonHelper;

public class ClipboardHandler {

    private final Context context;

    public ClipboardHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "read": 
                return readClipboard();
            default: 
                return JsonHelper.error("Sesuai aturan Read-Only, aksi modifikasi clipboard ('" + action + "') tidak diizinkan.");
        }
    }

    private String readClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) {
                JsonObject resp = new JsonObject();
                resp.addProperty("text", "");
                return JsonHelper.success(resp);
            }

            ClipData clipData = cm.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence text = clipData.getItemAt(0).getText();
                JsonObject resp = new JsonObject();
                resp.addProperty("text", text != null ? text.toString() : "");
                return JsonHelper.success(resp);
            }

            JsonObject resp = new JsonObject();
            resp.addProperty("text", "");
            return JsonHelper.success(resp);

        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }
}
