package com.hashibridge.master.handlers;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hashibridge.master.utils.JsonHelper;

public class ContactHandler {

    private final Context context;

    public ContactHandler(Context context) {
        this.context = context;
    }

    public String handle(String action, JsonObject payload) {
        switch (action) {
            case "list": return listContactsFast(null);
            case "search": return listContactsFast(payload.has("query") ? payload.get("query").getAsString() : "");
            default: return JsonHelper.error("Unknown contacts action: " + action);
        }
    }

    private String listContactsFast(String query) {
        JsonArray contacts = new JsonArray();
        String selection = null;
        String[] selectionArgs = null;

        if (query != null && !query.isEmpty()) {
            selection = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%"};
        }

        // Optimize: Only query the display name and ID. Skip the deep phone/email query here 
        // to prevent UI freezing on devices with 1000+ contacts.
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.HAS_PHONE_NUMBER
                },
                selection, selectionArgs,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long contactId = cursor.getLong(0);
                String name = cursor.getString(1);
                int hasPhone = cursor.getInt(2);

                JsonObject contact = new JsonObject();
                contact.addProperty("id", contactId);
                contact.addProperty("name", name != null ? name : "Unknown");
                contact.addProperty("hasPhone", hasPhone > 0);
                
                // Note: Phone numbers should be fetched on-demand by a separate action (e.g. "details")
                // to maintain high performance in v3.

                contacts.add(contact);
            }
            cursor.close();
        }

        JsonObject result = new JsonObject();
        result.add("contacts", contacts);
        return JsonHelper.success(result);
    }
}
