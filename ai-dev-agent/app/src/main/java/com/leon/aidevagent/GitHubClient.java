package com.leon.aidevagent;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class GitHubClient {
    interface Logger { void log(String s); }

    private final String token;
    private final String repo;
    private final Logger logger;

    GitHubClient(String token, String repo, Logger logger) {
        this.token = token == null ? "" : token.trim();
        this.repo = repo == null ? "" : repo.trim();
        this.logger = logger;
    }

    void validateRepo() throws Exception {
        JSONObject obj = getJson("/repos/" + repo);
        String full = obj.optString("full_name", "");
        if (full.isEmpty()) throw new IllegalStateException("Repository nicht erreichbar");
    }

    List<String> listRepositories() throws Exception {
        JSONArray arr = getArray("/user/repos?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member");
        List<String> repos = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            String full = arr.getJSONObject(i).optString("full_name", "");
            if (!full.isEmpty()) repos.add(full);
        }
        return repos;
    }

    String getDefaultBranch() throws Exception {
        JSONObject obj = getJson("/repos/" + repo);
        String branch = obj.optString("default_branch", "main");
        return branch.isEmpty() ? "main" : branch;
    }

    String detectAndroidWorkflow(String branch) throws Exception {
        JSONObject root = getJson("/repos/" + repo + "/actions/workflows?per_page=100");
        JSONArray arr = root.optJSONArray("workflows");
        if (arr == null || arr.length() == 0) {
            throw new IllegalStateException("Im Projekt wurde kein GitHub-Actions-Workflow gefunden.");
        }

        List<JSONObject> candidates = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) candidates.add(arr.getJSONObject(i));
        candidates.sort((a, b) -> Integer.compare(workflowScore(b), workflowScore(a)));

        for (JSONObject wf : candidates) {
            String path = wf.optString("path", "");
            if (path.isEmpty()) continue;
            try {
                String yaml = getFileText(path, branch);
                if (yaml == null) continue;
                String low = yaml.toLowerCase(Locale.ROOT);
                if (low.contains("workflow_dispatch") && workflowScore(wf) > 0) {
                    String name = wf.optString("name", path);
                    logger.log("Build-Workflow automatisch erkannt: " + name);
                    long id = wf.optLong("id", 0);
                    return id > 0 ? String.valueOf(id) : path.substring(path.lastIndexOf('/') + 1);
                }
            } catch (Exception ignored) {}
        }

        for (JSONObject wf : candidates) {
            String path = wf.optString("path", "");
            if (path.isEmpty()) continue;
            try {
                String yaml = getFileText(path, branch);
                if (yaml != null && yaml.toLowerCase(Locale.ROOT).contains("workflow_dispatch")) {
                    logger.log("Workflow automatisch erkannt: " + wf.optString("name", path));
                    long id = wf.optLong("id", 0);
                    return id > 0 ? String.valueOf(id) : path.substring(path.lastIndexOf('/') + 1);
                }
            } catch (Exception ignored) {}
        }
        throw new IllegalStateException("Kein Workflow mit workflow_dispatch gefunden. Im Expertenmodus kannst du einen Workflow manuell angeben.");
    }

    private static int workflowScore(JSONObject wf) {
        String t = (wf.optString("name", "") + " " + wf.optString("path", "")).toLowerCase(Locale.ROOT);
        int score = 0;
        if (t.contains("android")) score += 8;
        if (t.contains("apk")) score += 7;
        if (t.contains("gradle")) score += 5;
        if (t.contains("build")) score += 3;
        if (t.contains("release")) score += 2;
        return score;
    }

    String createWorkBranch(String baseBranch) throws Exception {
        JSONObject ref = getJson("/repos/" + repo + "/git/ref/heads/" + encPath(baseBranch));
        String sha = ref.getJSONObject("object").getString("sha");
        String branch = "ai-agent/" + System.currentTimeMillis();
        JSONObject body = new JSONObject();
        body.put("ref", "refs/heads/" + branch);
        body.put("sha", sha);
        requestJson("POST", "/repos/" + repo + "/git/refs", body, 201);
        return branch;
    }

    String collectContext(String branch, int maxChars) throws Exception {
        JSONObject tree = getJson("/repos/" + repo + "/git/trees/" + encQuery(branch) + "?recursive=1");
        JSONArray items = tree.optJSONArray("tree");
        if (items == null) return "";
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (!"blob".equals(item.optString("type"))) continue;
            String path = item.optString("path", "");
            long size = item.optLong("size", 0);
            if (size > 160000) continue;
            if (isRelevant(path)) paths.add(path);
        }
        paths.sort(Comparator.comparingInt(GitHubClient::priority));
        StringBuilder out = new StringBuilder();
        for (String path : paths) {
            if (out.length() >= maxChars) break;
            try {
                String fileContent = getFileText(path, branch);
                if (fileContent == null) continue;
                int room = maxChars - out.length();
                if (room < 500) break;
                String chunk = fileContent.length() > room - 120 ? fileContent.substring(0, Math.max(0, room - 120)) : fileContent;
                out.append("\n\n===== FILE: ").append(path).append(" =====\n").append(chunk);
            } catch (Exception ignored) {}
        }
        return out.toString();
    }

    void applyPatch(String branch, Models.PatchBundle bundle) throws Exception {
        int i = 0;
        for (Models.PatchFile file : bundle.files) {
            i++;
            logger.log("Schreibe " + i + "/" + bundle.files.size() + ": " + file.path);
            putFile(branch, file.path, file.content, bundle.commitMessage + " [" + i + "/" + bundle.files.size() + "]");
        }
    }

    void dispatchWorkflow(String workflowFile, String branch) throws Exception {
        String wf = workflowFile == null ? "" : workflowFile.trim();
        if (wf.contains("/")) wf = wf.substring(wf.lastIndexOf('/') + 1);
        if (wf.isEmpty()) throw new IllegalArgumentException("Workflow-Datei fehlt.");
        JSONObject body = new JSONObject();
        body.put("ref", branch);
        requestJson("POST", "/repos/" + repo + "/actions/workflows/" + encQuery(wf) + "/dispatches", body, 204);
    }

    Models.BuildResult waitForBuild(String branch, long notBeforeMillis, int maxPolls) throws Exception {
        long runId = 0;
        String html = "";
        for (int poll = 0; poll < maxPolls; poll++) {
            JSONObject runs = getJson("/repos/" + repo + "/actions/runs?branch=" + encQuery(branch) + "&per_page=10");
            JSONArray arr = runs.optJSONArray("workflow_runs");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject r = arr.getJSONObject(i);
                    long created = parseIsoMillis(r.optString("created_at", ""));
                    if (created + 120000L < notBeforeMillis) continue;
                    runId = r.optLong("id", 0);
                    html = r.optString("html_url", "");
                    String status = r.optString("status", "");
                    String conclusion = r.optString("conclusion", "");
                    if (runId > 0) {
                        logger.log("Build: " + status + (conclusion.isEmpty() || "null".equals(conclusion) ? "" : " / " + conclusion));
                        if ("completed".equals(status)) {
                            String logs = "success".equalsIgnoreCase(conclusion) ? "" : fetchFailureLogs(runId);
                            return new Models.BuildResult(runId, status, conclusion, html, logs);
                        }
                        break;
                    }
                }
            }
            Thread.sleep(12000L);
        }
        return new Models.BuildResult(runId, "timeout", "", html, "Kein abgeschlossener GitHub-Actions-Build innerhalb des Poll-Limits.");
    }

    Models.BuildResult latestBuild(String branch) throws Exception {
        JSONObject runs = getJson("/repos/" + repo + "/actions/runs?branch=" + encQuery(branch) + "&per_page=5");
        JSONArray arr = runs.optJSONArray("workflow_runs");
        if (arr == null || arr.length() == 0) return new Models.BuildResult(0, "none", "", "", "");
        JSONObject r = arr.getJSONObject(0);
        long id = r.optLong("id", 0);
        String conclusion = r.optString("conclusion", "");
        String logs = "failure".equalsIgnoreCase(conclusion) ? fetchFailureLogs(id) : "";
        return new Models.BuildResult(id, r.optString("status", ""), conclusion, r.optString("html_url", ""), logs);
    }

    Uri downloadFirstApkArtifact(Context context, long runId) throws Exception {
        JSONObject root = getJson("/repos/" + repo + "/actions/runs/" + runId + "/artifacts?per_page=100");
        JSONArray arr = root.optJSONArray("artifacts");
        if (arr == null || arr.length() == 0) throw new IllegalStateException("Kein Build-Artefakt gefunden.");
        JSONObject chosen = null;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject a = arr.getJSONObject(i);
            if (!a.optBoolean("expired", false)) { chosen = a; break; }
        }
        if (chosen == null) throw new IllegalStateException("Alle Build-Artefakte sind abgelaufen.");
        long artifactId = chosen.getLong("id");
        byte[] zip = requestBytes("GET", "/repos/" + repo + "/actions/artifacts/" + artifactId + "/zip");
        byte[] apk = null;
        String fileName = "build.apk";
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (!e.isDirectory() && e.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    copy(zis, bos);
                    apk = bos.toByteArray();
                    String n = new File(e.getName()).getName();
                    if (!n.isEmpty()) fileName = n;
                    break;
                }
            }
        }
        if (apk == null) throw new IllegalStateException("Artefakt enthält keine APK.");
        return saveApk(context, apk, fileName);
    }

    private Uri saveApk(Context context, byte[] apk, String fileName) throws Exception {
        String safe = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, safe);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AI-Dev-Agent");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("Downloads-Ziel konnte nicht erstellt werden.");
            try (OutputStream out = resolver.openOutputStream(uri)) { out.write(apk); }
            ContentValues done = new ContentValues();
            done.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, done, null, null);
            return uri;
        }
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = context.getFilesDir();
        File file = new File(dir, safe);
        try (FileOutputStream fos = new FileOutputStream(file)) { fos.write(apk); }
        return Uri.fromFile(file);
    }

    private String fetchFailureLogs(long runId) {
        StringBuilder logs = new StringBuilder();
        try {
            JSONObject root = getJson("/repos/" + repo + "/actions/runs/" + runId + "/jobs?per_page=100");
            JSONArray jobs = root.optJSONArray("jobs");
            if (jobs == null) return "";
            for (int i = 0; i < jobs.length(); i++) {
                JSONObject job = jobs.getJSONObject(i);
                String conclusion = job.optString("conclusion", "");
                if (!"failure".equalsIgnoreCase(conclusion) && !"cancelled".equalsIgnoreCase(conclusion)) continue;
                long jobId = job.optLong("id", 0);
                logs.append("\n===== JOB ").append(job.optString("name", String.valueOf(jobId))).append(" =====\n");
                byte[] data = requestBytes("GET", "/repos/" + repo + "/actions/jobs/" + jobId + "/logs");
                if (data.length > 3 && data[0] == 'P' && data[1] == 'K') logs.append(unzipText(data, 65000));
                else logs.append(new String(data, StandardCharsets.UTF_8));
                if (logs.length() > 70000) break;
            }
        } catch (Exception e) {
            logs.append("Fehler beim Laden der Build-Logs: ").append(e.getMessage());
        }
        return logs.length() > 70000 ? logs.substring(logs.length() - 70000) : logs.toString();
    }

    private String getFileText(String path, String branch) throws Exception {
        JSONObject obj = getJson("/repos/" + repo + "/contents/" + encPath(path) + "?ref=" + encQuery(branch));
        String encoding = obj.optString("encoding", "");
        if (!"base64".equalsIgnoreCase(encoding)) return null;
        String content = obj.optString("content", "").replace("\n", "");
        return new String(Base64.decode(content, Base64.DEFAULT), StandardCharsets.UTF_8);
    }

    private void putFile(String branch, String path, String content, String message) throws Exception {
        JSONObject body = new JSONObject();
        body.put("message", message);
        body.put("content", Base64.encodeToString(content.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
        body.put("branch", branch);
        try {
            JSONObject current = getJson("/repos/" + repo + "/contents/" + encPath(path) + "?ref=" + encQuery(branch));
            String sha = current.optString("sha", "");
            if (!sha.isEmpty()) body.put("sha", sha);
        } catch (HttpStatusException e) {
            if (e.code != 404) throw e;
        }
        requestJson("PUT", "/repos/" + repo + "/contents/" + encPath(path), body, 200, 201);
    }

    private JSONObject getJson(String path) throws Exception {
        return requestJson("GET", path, null, 200);
    }

    private JSONArray getArray(String path) throws Exception {
        HttpURLConnection con = open("GET", path);
        int code = con.getResponseCode();
        InputStream in = code >= 200 && code < 400 ? con.getInputStream() : con.getErrorStream();
        String text = readText(in, 500000);
        if (code != 200) throw new HttpStatusException(code, text);
        return new JSONArray(text);
    }

    private JSONObject requestJson(String method, String path, JSONObject body, int... expected) throws Exception {
        HttpURLConnection con = open(method, path);
        if (body != null) {
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            try (OutputStream out = con.getOutputStream()) {
                out.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = con.getResponseCode();
        InputStream in = code >= 200 && code < 400 ? con.getInputStream() : con.getErrorStream();
        String text = readText(in, 200000);
        if (!contains(expected, code)) throw new HttpStatusException(code, text);
        return text.trim().isEmpty() ? new JSONObject() : new JSONObject(text);
    }

    private byte[] requestBytes(String method, String path) throws Exception {
        HttpURLConnection con = open(method, path);
        con.setInstanceFollowRedirects(true);
        int code = con.getResponseCode();
        if (code < 200 || code >= 400) {
            throw new HttpStatusException(code, readText(con.getErrorStream(), 5000));
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream in = new BufferedInputStream(con.getInputStream())) { copy(in, bos); }
        return bos.toByteArray();
    }

    private HttpURLConnection open(String method, String path) throws Exception {
        URL url = new URL("https://api.github.com" + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);
        con.setConnectTimeout(30000);
        con.setReadTimeout(120000);
        con.setRequestProperty("Accept", "application/vnd.github+json");
        con.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        con.setRequestProperty("User-Agent", "AI-Dev-Agent/0.2 Android");
        if (!token.isEmpty()) con.setRequestProperty("Authorization", "Bearer " + token);
        return con;
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) if (v == value) return true;
        return false;
    }

    private static String readText(InputStream in, int maxChars) throws Exception {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) >= 0) {
                int room = maxChars - sb.length();
                if (room <= 0) break;
                sb.append(buf, 0, Math.min(room, n));
            }
        }
        return sb.toString();
    }

    private static void copy(InputStream in, OutputStream out) throws Exception {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
    }

    private static String unzipText(byte[] zip, int maxChars) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null && sb.length() < maxChars) {
                if (e.isDirectory()) continue;
                sb.append("\n--- ").append(e.getName()).append(" ---\n");
                BufferedReader br = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                char[] buf = new char[4096];
                int n;
                while ((n = br.read(buf)) >= 0 && sb.length() < maxChars) {
                    int room = maxChars - sb.length();
                    sb.append(buf, 0, Math.min(room, n));
                }
            }
        }
        return sb.toString();
    }

    private static boolean isRelevant(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.contains("/build/") || p.contains("/.gradle/") || p.contains("/node_modules/") || p.endsWith(".lock") || p.endsWith(".png") || p.endsWith(".jpg") || p.endsWith(".webp") || p.endsWith(".apk") || p.endsWith(".zip")) return false;
        return p.endsWith(".kt") || p.endsWith(".java") || p.endsWith(".xml") || p.endsWith(".gradle") || p.endsWith(".kts") || p.endsWith(".toml") || p.endsWith(".json") || p.endsWith(".md") || p.endsWith(".properties") || p.endsWith(".yml") || p.endsWith(".yaml") || p.endsWith(".js") || p.endsWith(".ts") || p.endsWith(".py");
    }

    private static int priority(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith("settings.gradle") || p.endsWith("settings.gradle.kts")) return 0;
        if (p.endsWith("build.gradle") || p.endsWith("build.gradle.kts") || p.endsWith("libs.versions.toml") || p.endsWith("androidmanifest.xml")) return 1;
        if (p.contains("mainactivity") || p.contains("application")) return 2;
        if (p.endsWith(".kt") || p.endsWith(".java")) return 3;
        if (p.endsWith(".xml")) return 4;
        if (p.endsWith(".md")) return 9;
        return 6;
    }

    private static String encQuery(String s) throws Exception {
        return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
    }

    private static String encPath(String s) throws Exception {
        String[] parts = s.split("/");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append('/');
            out.append(URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20"));
        }
        return out.toString();
    }

    private static long parseIsoMillis(String s) {
        try {
            if (Build.VERSION.SDK_INT >= 26) return java.time.Instant.parse(s).toEpochMilli();
        } catch (Exception ignored) {}
        return 0L;
    }

    static final class HttpStatusException extends Exception {
        final int code;
        HttpStatusException(int code, String body) {
            super("GitHub HTTP " + code + ": " + OpenAiClient.abbreviate(body, 1800));
            this.code = code;
        }
    }
}
