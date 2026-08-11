package com.hashibridge.master.handlers;

import android.content.Context;
import com.hashibridge.master.RelayClient;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyHandler {
    private final Context context;
    private final RelayClient relayClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ProxyHandler(Context context, RelayClient relayClient) {
        this.context = context;
        this.relayClient = relayClient;
    }

    public String handle(JsonObject payload, String requestId) {
        if (payload == null || !payload.has("path")) {
            return "{\"error\":\"path required\"}";
        }

        String path = payload.get("path").getAsString();
        String method = payload.has("method") ? payload.get("method").getAsString() : "GET";
        JsonObject headers = payload.has("headers") ? payload.getAsJsonObject("headers") : new JsonObject();

        executor.submit(() -> performLocalRequest(method, path, headers, requestId));
        
        return "{\"status\":\"proxying\"}";
    }

    private void performLocalRequest(String method, String path, JsonObject reqHeaders, String requestId) {
        HttpURLConnection conn = null;
        try {
            if (!path.startsWith("/")) path = "/" + path;
            URL url = new URL("http://127.0.0.1:8080" + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            
            // Forward relevant headers (like Range for video streaming)
            if (reqHeaders.has("range")) {
                conn.setRequestProperty("Range", reqHeaders.get("range").getAsString());
            }

            int responseCode = conn.getResponseCode();
            
            // Build Proxy Headers
            JsonObject proxyHeaders = new JsonObject();
            for (Map.Entry<String, List<String>> entry : conn.getHeaderFields().entrySet()) {
                if (entry.getKey() != null && entry.getValue().size() > 0) {
                    proxyHeaders.addProperty(entry.getKey(), entry.getValue().get(0));
                }
            }

            InputStream is = (responseCode >= 200 && responseCode < 300) 
                ? conn.getInputStream() 
                : conn.getErrorStream();
                
            if (is == null) {
                sendEof(requestId, responseCode, proxyHeaders);
                return;
            }

            byte[] buffer = new byte[32768]; // 32KB chunks
            int read;
            boolean headersSent = false;

            while ((read = is.read(buffer)) != -1) {
                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);
                
                JsonObject headerObj = new JsonObject();
                headerObj.addProperty("target", "proxy");
                headerObj.addProperty("requestId", requestId);
                headerObj.addProperty("isEof", false);
                if (!headersSent) {
                    headerObj.addProperty("status", responseCode);
                    headerObj.add("proxyHeaders", proxyHeaders);
                    headersSent = true;
                }
                
                relayClient.sendBinaryChunk(headerObj, chunk);
            }
            is.close();
            sendEof(requestId, headersSent ? 0 : responseCode, headersSent ? null : proxyHeaders);

        } catch (Exception e) {
            e.printStackTrace();
            sendEof(requestId, 500, new JsonObject());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    
    private void sendEof(String requestId, int status, JsonObject headers) {
        JsonObject headerObj = new JsonObject();
        headerObj.addProperty("target", "proxy");
        headerObj.addProperty("requestId", requestId);
        headerObj.addProperty("isEof", true);
        if (status > 0) {
            headerObj.addProperty("status", status);
            headerObj.add("proxyHeaders", headers);
        }
        relayClient.sendBinaryChunk(headerObj, new byte[0]);
    }
}
