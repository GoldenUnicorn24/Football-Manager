package com.leon.aidevagent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class Models {
    private Models() {}

    static final class Config {
        String endpoint;
        String model;
        String apiKey;
        String githubToken;
        String repo;
        String baseBranch;
        String workflow;
        int maxRounds;
        int contextChars;

        Config() {
            endpoint = "https://api.groq.com/openai/v1";
            model = "openai/gpt-oss-120b";
            apiKey = "";
            githubToken = "";
            repo = "";
            baseBranch = "AUTO";
            workflow = "AUTO";
            maxRounds = 6;
            contextChars = 70000;
        }
    }

    static final class PatchFile {
        final String path;
        final String content;
        PatchFile(String path, String content) { this.path = path; this.content = content; }
    }

    static final class PatchBundle {
        final String summary;
        final String commitMessage;
        final List<PatchFile> files;

        PatchBundle(String summary, String commitMessage, List<PatchFile> files) {
            this.summary = summary; this.commitMessage = commitMessage; this.files = files;
        }

        static PatchBundle fromModelText(String text) throws JSONException {
            JSONObject root = new JSONObject(extractJsonObject(text));
            String summary = root.optString("summary", "AI-Agent Änderungen");
            String commit = root.optString("commitMessage", "AI agent changes");
            JSONArray arr = root.getJSONArray("files");
            List<PatchFile> files = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject f = arr.getJSONObject(i);
                String path = f.getString("path").trim();
                String content = f.getString("content");
                if (isSafeRepoPath(path)) files.add(new PatchFile(path, content));
            }
            if (files.isEmpty()) throw new JSONException("Kein sicherer Datei-Patch im Modelloutput gefunden.");
            return new PatchBundle(summary, commit, files);
        }
    }

    static final class BuildResult {
        final long runId;
        final String status;
        final String conclusion;
        final String htmlUrl;
        final String logs;

        BuildResult(long runId, String status, String conclusion, String htmlUrl, String logs) {
            this.runId = runId; this.status = status; this.conclusion = conclusion; this.htmlUrl = htmlUrl; this.logs = logs;
        }

        boolean success() { return "success".equalsIgnoreCase(conclusion); }
        boolean finished() { return conclusion != null && !conclusion.isEmpty() && !"null".equals(conclusion); }
    }

    static String extractJsonObject(String text) throws JSONException {
        if (text == null) throw new JSONException("Leere Modellantwort");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) throw new JSONException("Keine JSON-Struktur in Modellantwort gefunden");
        return text.substring(start, end + 1);
    }

    static boolean isSafeRepoPath(String path) {
        if (path == null || path.isEmpty()) return false;
        if (path.startsWith("/") || path.contains("..") || path.contains("\\")) return false;
        String p = path.toLowerCase();
        return !p.startsWith(".git/") && !p.equals(".git") && !p.contains("/secrets/") && !p.endsWith(".jks") && !p.endsWith(".keystore");
    }
}
