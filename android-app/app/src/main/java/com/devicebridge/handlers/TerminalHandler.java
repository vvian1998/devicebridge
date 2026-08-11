package com.devicebridge.handlers;

import com.google.gson.JsonObject;
import com.devicebridge.utils.JsonHelper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TerminalHandler {

    private static final String TAG = "TerminalHandler";

    public interface TerminalOutputCallback {
        void onOutput(String terminalId, String data);
    }

    private final Map<String, Process> shellProcesses = new ConcurrentHashMap<>();
    private final Map<String, Thread> readerThreads = new ConcurrentHashMap<>();
    private TerminalOutputCallback outputCallback;

    public void setOutputCallback(TerminalOutputCallback callback) {
        this.outputCallback = callback;
    }

    public String handle(String action, JsonObject payload) {
        String terminalId = payload.has("terminalId") ? payload.get("terminalId").getAsString() : "default";

        switch (action) {
            case "open": return openShell(terminalId, payload);
            case "input": return sendInput(terminalId, payload);
            case "close": return closeShell(terminalId);
            case "resize": return resize(terminalId, payload);
            default: return JsonHelper.error("Unknown terminal action: " + action);
        }
    }

    private String openShell(String terminalId, JsonObject payload) {
        closeShell(terminalId);

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-"});
            shellProcesses.put(terminalId, process);

            Thread readerThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    char[] buffer = new char[4096];
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        String data = new String(buffer, 0, n);
                        sendTerminalOutput(terminalId, data);
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error reading shell stdout", e);
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();
            readerThreads.put(terminalId, readerThread);

            Thread errorThread = new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()));
                    char[] buffer = new char[4096];
                    int n;
                    while ((n = reader.read(buffer)) != -1) {
                        String data = new String(buffer, 0, n);
                        sendTerminalOutput(terminalId, data);
                    }
                } catch (Exception e) {
                    android.util.Log.e(TAG, "Error reading shell stderr", e);
                }
            });
            errorThread.setDaemon(true);
            errorThread.start();

            return JsonHelper.success("terminal opened");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String sendInput(String terminalId, JsonObject payload) {
        Process process = shellProcesses.get(terminalId);
        if (process == null) return JsonHelper.error("terminal not open");

        try {
            String data = payload.get("data").getAsString();
            process.getOutputStream().write(data.getBytes());
            process.getOutputStream().flush();
            return JsonHelper.success("sent");
        } catch (Exception e) {
            return JsonHelper.error(e.getMessage());
        }
    }

    private String closeShell(String terminalId) {
        Process process = shellProcesses.remove(terminalId);
        if (process != null) {
            process.destroy();
        }

        Thread reader = readerThreads.remove(terminalId);
        if (reader != null) {
            reader.interrupt();
        }

        return JsonHelper.success("closed");
    }

    private String resize(String terminalId, JsonObject payload) {
        return JsonHelper.success("resized");
    }

    private void sendTerminalOutput(String terminalId, String data) {
        if (outputCallback != null) {
            try {
                outputCallback.onOutput(terminalId, data);
            } catch (Exception e) {
                android.util.Log.e(TAG, "Failed to send terminal output", e);
            }
        }
    }
}
