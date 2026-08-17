package com.hashibridge.master;

import android.content.Context;
import com.google.gson.JsonObject;
import com.hashibridge.master.handlers.*;
import com.hashibridge.master.utils.JsonHelper;
import java.util.HashMap;
import java.util.Map;

public class RequestRouter {

    private final Context context;
    private final LocalServer localServer;
    private final Map<String, Object> handlers = new HashMap<>();
    private RelayClient relayClient;

    public RequestRouter(Context context, LocalServer localServer) {
        this.context = context;
        this.localServer = localServer;
    }

    public void setRelayClient(RelayClient relayClient) {
        this.relayClient = relayClient;
    }

    /**
     * Lazily creates and caches handler instances on first use.
     */
    private Object getHandler(String key) {
        Object handler = handlers.get(key);
        if (handler != null) return handler;

        switch (key) {
            case "system": handler = new SystemHandler(context); break;
            case "file": handler = new FileHandler(context); break;
            case "media": handler = new MediaHandler(context); break;
            case "contacts": handler = new ContactHandler(context); break;
            case "terminal": handler = createTerminalHandler(); break;
            case "screenshot": handler = new ScreenshotHandler(context); break;
            case "notifications": handler = new NotificationHandler(context); break;
            case "clipboard": handler = new ClipboardHandler(context); break;
            case "location": handler = new LocationHandler(context); break;
            case "camera": handler = new CameraHandler(context); break;
            case "applist": handler = new AppListHandler(context); break;
            case "devicecontrol": handler = new DeviceControlHandler(context); break;
            case "sms": handler = new SmsHandler(context); break;
            case "proxy": handler = new ProxyHandler(context, relayClient); break;
            default: return null;
        }

        handlers.put(key, handler);
        return handler;
    }

    private TerminalHandler createTerminalHandler() {
        TerminalHandler th = new TerminalHandler();
        // Wire up terminal output callback to relay client via BridgeService
        th.setOutputCallback((terminalId, data) -> {
            BridgeService instance = BridgeService.isRunning() ? BridgeService.getInstance() : null;
            if (instance != null) {
                JsonObject eventData = new JsonObject();
                eventData.addProperty("terminalId", terminalId);
                eventData.addProperty("data", data);
                instance.sendRelayEvent("terminal_output", eventData.toString());
            }
        });
        return th;
    }

    public String route(String method, String path, Map<String, String> params, String jsonBody) {
        // Parse JSON body into a JsonObject payload for the handler
        JsonObject payload = null;
        if (jsonBody != null && !jsonBody.isEmpty()) {
            try {
                payload = com.google.gson.JsonParser.parseString(jsonBody).getAsJsonObject();
            } catch (Exception ignored) {}
        }
        // Also inject query params into the payload if not already present
        if (params != null && !params.isEmpty()) {
            if (payload == null) payload = new JsonObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!payload.has(entry.getKey())) {
                    payload.addProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        return _route(method, path, payload, null);
    }

    public String route(String method, String type, JsonObject payload, String requestId) {
        return _route(method, type, payload, requestId);
    }

    private String _route(String method, String handlerKey, JsonObject payload, String requestId) {
        Object handler = getHandler(handlerKey);
        if (handler == null) {
            return JsonHelper.error("unknown handler: " + handlerKey);
        }

        try {
            String action = payload != null && payload.has("action") ? payload.get("action").getAsString() : "default";

            switch (handlerKey) {
                case "system": return ((SystemHandler) handler).handle(action, payload);
                case "file": return ((FileHandler) handler).handle(action, payload);
                case "media": return ((MediaHandler) handler).handle(action, payload);
                case "contacts": return ((ContactHandler) handler).handle(action, payload);
                case "terminal": return ((TerminalHandler) handler).handle(action, payload);
                case "screenshot": return ((ScreenshotHandler) handler).handle(action, payload);
                case "notifications": return ((NotificationHandler) handler).handle(action, payload);
                case "clipboard": return ((ClipboardHandler) handler).handle(action, payload);
                case "location": return ((LocationHandler) handler).handle(action, payload);
                case "camera": return ((CameraHandler) handler).handle(action, payload);
                case "applist": return ((AppListHandler) handler).handle(action, payload);
                case "devicecontrol": return ((DeviceControlHandler) handler).handle(action, payload);
                case "sms": return ((SmsHandler) handler).handle(action, payload);
                case "proxy": return ((ProxyHandler) handler).handle(payload, requestId);
                default: return JsonHelper.error("unknown handler: " + handlerKey);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return JsonHelper.error(e.getMessage());
        }
    }
}
