package com.hashibridge.master;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RelayClient {

    private final Context context;
    private final String relayUrl;
    private final String deviceId;
    private final RequestRouter router;
    private final Gson gson;
    private final Handler mainHandler;

    private WebSocketClient ws;
    private ScheduledExecutorService pingExecutor;
    private volatile boolean shouldReconnect = true;
    private String token = "";
    private int reconnectDelay = 5000;
    private long keepAliveInterval = 30_000;

    public RelayClient(Context context, String relayUrl, String deviceId, RequestRouter router) {
        this.context = context;
        this.relayUrl = relayUrl;
        this.deviceId = deviceId;
        this.router = router;
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void connect() {
        shouldReconnect = true;
        doConnect();
    }

    public void connectPermanent() {
        shouldReconnect = true;
        doConnect();
    }

    public void forceReconnect() {
        if (!shouldReconnect) return;
        if (ws != null && ws.isOpen()) {
            try {
                ws.close();
            } catch (Exception ignored) {}
        }
        reconnectDelay = 1000;
        doConnect();
    }

    public boolean isConnected() {
        return ws != null && ws.isOpen();
    }

    public void setKeepAliveInterval(long intervalMs) {
        this.keepAliveInterval = intervalMs;
    }

    public void setToken(String token) {
        this.token = token != null ? token : "";
    }

    private void doConnect() {
        try {
            String wsUrl = relayUrl.replaceFirst("^http", "ws");
            if (!wsUrl.startsWith("ws://") && !wsUrl.startsWith("wss://")) {
                wsUrl = "ws://" + wsUrl;
            }
            String encodedToken = java.net.URLEncoder.encode(token, "UTF-8");
            URI uri = new URI(wsUrl + "?id=" + deviceId + "&role=device&token=" + encodedToken);

            ws = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    reconnectDelay = 5000;
                    startPing();
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    byte[] data = new byte[bytes.remaining()];
                    bytes.get(data);
                    handleBinaryMessage(data);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    stopPing();
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            ws.connect();

        } catch (Exception e) {
            e.printStackTrace();
            scheduleReconnect();
        }
    }

    private void handleMessage(String message) {
        try {
            JsonObject msg = JsonParser.parseString(message).getAsJsonObject();
            String type = msg.has("type") ? msg.get("type").getAsString() : "";
            String requestId = msg.has("requestId") ? msg.get("requestId").getAsString() : null;
            String from = msg.has("from") ? msg.get("from").getAsString() : "";

            JsonObject payload = msg.has("payload") ? msg.get("payload").getAsJsonObject() : new JsonObject();

            String responseStr = router.route("WS", type, payload, requestId);

            if (ws != null && ws.isOpen()) {
                JsonObject respMsg = new JsonObject();
                respMsg.addProperty("target", from);
                respMsg.addProperty("type", "response");
                respMsg.addProperty("requestId", requestId);
                try {
                    respMsg.add("payload", JsonParser.parseString(responseStr));
                } catch (Exception e) {
                    respMsg.addProperty("payload", responseStr);
                }
                ws.send(gson.toJson(respMsg));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendEvent(String type, String data) {
        if (ws != null && ws.isOpen()) {
            try {
                JsonObject msg = new JsonObject();
                msg.addProperty("broadcast", true);
                msg.addProperty("type", "event");

                JsonObject payload = new JsonObject();
                payload.addProperty("type", type);
                payload.addProperty("data", data);
                msg.add("payload", payload);

                ws.send(gson.toJson(msg));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendBinary(String target, String requestId, byte[] data) {
        if (ws == null || !ws.isOpen()) return;
        try {
            JsonObject header = new JsonObject();
            header.addProperty("target", target);
            header.addProperty("requestId", requestId);
            header.addProperty("binary", true);

            ByteBuffer buffer = ByteBuffer.allocate(header.toString().length() + 1 + data.length);
            buffer.put(header.toString().getBytes());
            buffer.put((byte) 0);
            buffer.put(data);
            buffer.flip();

            ws.send(buffer.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendBinaryChunk(JsonObject headerObj, byte[] chunk) {
        if (ws == null || !ws.isOpen()) return;
        try {
            ByteBuffer buffer = ByteBuffer.allocate(headerObj.toString().length() + 1 + chunk.length);
            buffer.put(headerObj.toString().getBytes());
            buffer.put((byte) 0);
            buffer.put(chunk);
            buffer.flip();

            ws.send(buffer.array());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBinaryMessage(byte[] data) {
        try {
            int sep = -1;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == 0) { sep = i; break; }
            }
            if (sep <= 0) return;

            String headerStr = new String(data, 0, sep, "UTF-8");
            JsonObject header = JsonParser.parseString(headerStr).getAsJsonObject();
            String type = header.has("type") ? header.get("type").getAsString() : "";
            String requestId = header.has("requestId") ? header.get("requestId").getAsString() : null;
            String from = header.has("from") ? header.get("from").getAsString() : "";

            byte[] payload = new byte[data.length - sep - 1];
            System.arraycopy(data, sep + 1, payload, 0, payload.length);

            if ("file_upload".equals(type)) {
                handleBinaryUpload(requestId, from, payload);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBinaryUpload(String requestId, String from, byte[] payload) {
        try {
            String path = new String(payload, "UTF-8");
            if (ws != null && ws.isOpen()) {
                JsonObject respMsg = new JsonObject();
                respMsg.addProperty("target", from);
                respMsg.addProperty("type", "response");
                respMsg.addProperty("requestId", requestId);
                respMsg.addProperty("payload", "{\"status\":\"ok\",\"message\":\"received binary\"}");
                ws.send(gson.toJson(respMsg));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPing() {
        pingExecutor = Executors.newSingleThreadScheduledExecutor();
        pingExecutor.scheduleAtFixedRate(() -> {
            if (ws != null && ws.isOpen()) {
                try {
                    ws.send("{\"type\":\"ping\"}");
                } catch (Exception ignored) {}
            }
        }, keepAliveInterval, keepAliveInterval, TimeUnit.MILLISECONDS);
    }

    private void stopPing() {
        if (pingExecutor != null) {
            pingExecutor.shutdown();
            pingExecutor = null;
        }
    }

    private void scheduleReconnect() {
        if (!shouldReconnect) return;

        mainHandler.postDelayed(() -> {
            if (shouldReconnect) {
                reconnectDelay = Math.min(reconnectDelay * 2, 60000);
                doConnect();
            }
        }, reconnectDelay);
    }

    public void disconnect() {
        shouldReconnect = false;
        stopPing();
        if (ws != null) {
            try {
                ws.close();
            } catch (Exception ignored) {}
            ws = null;
        }
    }
}
