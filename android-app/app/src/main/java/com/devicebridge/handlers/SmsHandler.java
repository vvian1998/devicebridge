package com.devicebridge.handlers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SmsHandler {

    private final Context context;

    public SmsHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "list": return listSms(payload);
            default: return JsonHelper.error("Unknown sms action: " + action);
        }
    }

    private String listSms(JsonObject payload) {
        JsonArray messages = new JsonArray();
        int limit = payload.has("limit") ? payload.get("limit").getAsInt() : 50;

        Cursor cursor = context.getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                new String[]{
                        Telephony.Sms._ID,
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE,
                        Telephony.Sms.TYPE
                },
                null, null,
                Telephony.Sms.DEFAULT_SORT_ORDER + " LIMIT " + limit
        );

        if (cursor != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            while (cursor.moveToNext()) {
                JsonObject msg = new JsonObject();
                msg.addProperty("id", cursor.getLong(0));
                msg.addProperty("address", cursor.getString(1));
                msg.addProperty("body", cursor.getString(2));
                msg.addProperty("date", sdf.format(new Date(cursor.getLong(3))));
                
                int type = cursor.getInt(4);
                String typeStr = (type == Telephony.Sms.MESSAGE_TYPE_SENT) ? "sent" : "inbox";
                msg.addProperty("type", typeStr);

                messages.add(msg);
            }
            cursor.close();
        }

        JsonObject result = new JsonObject();
        result.add("messages", messages);
        return JsonHelper.success(result);
    }
}
