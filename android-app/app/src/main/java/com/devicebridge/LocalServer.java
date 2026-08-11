package com.devicebridge;

import android.content.Context;
import fi.iki.elonen.NanoHTTPD;
import com.devicebridge.utils.JsonHelper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Map;

public class LocalServer extends NanoHTTPD {

    private final Context context;
    private RequestRouter router;
    private boolean started = false;

    public LocalServer(int port, Context context) {
        super(port);
        this.context = context;
    }

    public LocalServer(int port, Context context, int threads) {
        super(port);
        this.context = context;
    }

    public void setRouter(RequestRouter router) {
        this.router = router;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public void start() throws IOException {
        super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        started = true;
    }

    @Override
    public void stop() {
        started = false;
        super.stop();
    }

    public boolean isStarted() {
        return started;
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            String uri = session.getUri();
            Map<String, String> params = session.getParms();

            if (uri.startsWith("/web/") || uri.equals("/web") || uri.equals("/")) {
                return serveStatic(uri);
            }

            if (uri.startsWith("/stream/")) {
                return serveStream(uri.substring("/stream/".length()), params, session);
            }

            if (uri.startsWith("/api/")) {
                if (router == null) {
                    return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE,
                            "application/json", "{\"error\":\"router not ready\"}");
                }

                String path = uri.substring(5);

                String jsonBody = null;
                try {
                    int contentLength = Integer.parseInt(
                            session.getHeaders().getOrDefault("content-length", "-1"));
                    if (contentLength > 0) {
                        byte[] buffer = new byte[contentLength];
                        session.getInputStream().read(buffer);
                        jsonBody = new String(buffer);
                    }
                } catch (NumberFormatException ignored) {}

                String result = router.route(session.getMethod().name(), path, params, jsonBody);
                return newFixedLengthResponse(Response.Status.OK, "application/json", result);
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND,
                    "application/json", JsonHelper.error("not found"));

        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    "application/json", JsonHelper.error(e.getMessage()));
        }
    }

    private Response serveStream(String type, Map<String, String> params, IHTTPSession session) {
        if ("file".equals(type)) {
            return streamFile(params, session);
        }
        if ("thumbnail".equals(type)) {
            return streamThumbnail(params);
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "unknown stream type");
    }

    private Response streamFile(Map<String, String> params, IHTTPSession session) {
        String path = params.get("path");
        if (path == null || path.isEmpty()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "path required");
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "file not found");
        }

        String mime = getMimeType(path);
        long total = file.length();

        long start = 0;
        long end = total - 1;
        boolean partial = false;

        String range = session.getHeaders().get("range");
        if (range != null && range.startsWith("bytes=")) {
            String spec = range.substring("bytes=".length()).split(",")[0].trim();
            try {
                if (spec.startsWith("-")) {
                    long suffix = Long.parseLong(spec.substring(1));
                    start = Math.max(0, total - suffix);
                } else if (spec.endsWith("-")) {
                    start = Long.parseLong(spec.substring(0, spec.length() - 1));
                } else {
                    String[] parts = spec.split("-");
                    start = Long.parseLong(parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        end = Long.parseLong(parts[1]);
                    }
                }
                if (start < 0) start = 0;
                if (end >= total) end = total - 1;
                if (end < start) end = start;
                partial = true;
            } catch (NumberFormatException ignored) {}
        }

        long length = end - start + 1;

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(start);

            Response resp;
            if (partial) {
                resp = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, new RafInputStream(raf, length), length);
                resp.addHeader("Content-Range", "bytes " + start + "-" + end + "/" + total);
            } else {
                resp = newFixedLengthResponse(Response.Status.OK, mime, new RafInputStream(raf, length), length);
            }
            resp.addHeader("Accept-Ranges", "bytes");
            resp.addHeader("Cache-Control", "public, max-age=3600");
            return resp;
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    "text/plain", "stream error: " + e.getMessage());
        }
    }

    private Response streamThumbnail(Map<String, String> params) {
        String idStr = params.get("id");
        if (idStr == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "id required");
        }

        try {
            long mediaId = Long.parseLong(idStr);
            boolean isVideo = "videos".equals(params.get("type"));

            byte[] thumb = com.devicebridge.handlers.MediaHandler.getThumbnailBytes(context, mediaId, isVideo);
            if (thumb == null) {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "no thumbnail");
            }

            return newFixedLengthResponse(Response.Status.OK, "image/jpeg",
                    new ByteArrayInputStream(thumb), thumb.length);
        } catch (NumberFormatException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "bad id");
        }
    }

    private Response serveStatic(String uri) {
        try {
            if (uri.equals("/") || uri.equals("/web")) {
                uri = "/web/index.html";
            } else if (!uri.startsWith("/web/")) {
                uri = "/web" + uri;
            }

            String mimeType = getMimeType(uri);
            String assetPath = uri.replace("/web", "");

            try {
                InputStream is = context.getAssets().open(assetPath);
                byte[] data = new byte[is.available()];
                is.read(data);
                is.close();
                return newFixedLengthResponse(Response.Status.OK, mimeType,
                        new ByteArrayInputStream(data), data.length);
            } catch (Exception e) {
                InputStream is = context.getAssets().open("web/index.html");
                byte[] data = new byte[is.available()];
                is.read(data);
                is.close();
                return newFixedLengthResponse(Response.Status.OK, "text/html",
                        new ByteArrayInputStream(data), data.length);
            }
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found");
        }
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".webp")) return "image/webp";
        if (path.endsWith(".woff2")) return "font/woff2";
        if (path.endsWith(".mp4")) return "video/mp4";
        if (path.endsWith(".mkv")) return "video/x-matroska";
        if (path.endsWith(".mp3")) return "audio/mpeg";
        return "application/octet-stream";
    }

    private static class RafInputStream extends InputStream {
        private final RandomAccessFile raf;
        private long remaining;

        RafInputStream(RandomAccessFile raf, long length) {
            this.raf = raf;
            this.remaining = length;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = raf.read();
            if (b == -1) return -1;
            remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int n = raf.read(b, off, toRead);
            if (n == -1) return -1;
            remaining -= n;
            return n;
        }

        @Override
        public void close() throws IOException {
            raf.close();
        }
    }
}
