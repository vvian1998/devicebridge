package com.devicebridge.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class JsonHelper {

    private static final Gson gson = new GsonBuilder().create();

    public static String success(Object data) {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", "ok");
        if (data instanceof JsonObject) {
            JsonObject dataObj = (JsonObject) data;
            for (String key : dataObj.keySet()) {
                resp.add(key, dataObj.get(key));
            }
        } else if (data instanceof String) {
            resp.addProperty("message", (String) data);
        }
        return gson.toJson(resp);
    }

    public static String success(String message) {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", "ok");
        resp.addProperty("message", message);
        return gson.toJson(resp);
    }

    public static String error(String message) {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", "error");
        resp.addProperty("message", message);
        return gson.toJson(resp);
    }

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
}
