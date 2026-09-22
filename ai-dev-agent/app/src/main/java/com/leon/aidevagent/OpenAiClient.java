package com.leon.aidevagent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class OpenAiClient {
    private final Models.Config config;

    OpenAiClient(Models.Config config) { this.config = config; }

    String chat(String system, String user, double temperature) throws Exception {
        String base = config.endpoint.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        URL url = new URL(base + "/chat/completions");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(30000);
        con.setReadTimeout(120000);
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + config.apiKey);
        con.setRequestProperty("User-Agent", "AI-Dev-Agent/0.1 Android");

        JSONObject root = new JSONObject();
        root.put("model", config.model);
        root.put("temperature", temperature);
        root.put("max_tokens", 8192);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", system));
        messages.put(new JSONObject().put("role", "user").put("content", user));
        root.put("messages", messages);

        try (OutputStream os = con.getOutputStream()) {
            os.write(root.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = con.getResponseCode();
        String response = readAll(code >= 200 && code < 300 ? con.getInputStream() : con.getErrorStream());
        if (code < 200 || code >= 300) throw new IllegalStateException("LLM HTTP " + code + ": " + abbreviate(response, 1800));

        JSONObject obj = new JSONObject(response);
        JSONArray choices = obj.optJSONArray("choices");
        if (choices == null || choices.length() == 0) throw new IllegalStateException("LLM-Antwort enthält keine choices");
        JSONObject msg = choices.getJSONObject(0).optJSONObject("message");
        if (msg == null) throw new IllegalStateException("LLM-Antwort enthält keine message");
        Object content = msg.opt("content");
        if (content instanceof String) return (String) content;
        if (content instanceof JSONArray) {
            StringBuilder sb = new StringBuilder();
            JSONArray arr = (JSONArray) content;
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.get(i);
                if (item instanceof JSONObject) sb.append(((JSONObject) item).optString("text", ""));
                else sb.append(String.valueOf(item));
            }
            return sb.toString();
        }
        return String.valueOf(content);
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
