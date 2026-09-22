package com.leon.aidevagent;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String PREF = "ai_dev_agent_cfg";

    private EditText endpoint, model, apiKey, githubToken, repo, baseBranch, task, maxRounds, contextChars;
    private TextView status, log;
    private Button runButton, stopButton, downloadButton;
    private SecretStore secretStore;
    private volatile boolean stopRequested;
    private volatile long lastRunId;
    private volatile String lastWorkBranch = "";
    private Thread worker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        secretStore = new SecretStore(this);
        setContentView(buildUi());
        loadConfig();
        appendLog("AI Dev Agent v0.1 bereit. Änderungen werden nur in einem neuen ai-agent/* Branch vorgenommen.");
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(16, 17, 20));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(text("AI Dev Agent", 28, true, Color.WHITE));
        TextView sub = text("Multi-Agent Entwicklung für Android/GitHub – Manager → Coder → Review → Build → Fix", 14, false, Color.rgb(185, 190, 198));
        LinearLayout.LayoutParams subLp = lp(); subLp.bottomMargin = dp(18); root.addView(sub, subLp);

        root.addView(section("LLM-Provider"));
        endpoint = field("OpenAI-kompatible Base URL", false, false);
        model = field("Modell-ID", false, false);
        apiKey = field("API-Key", true, false);
        root.addView(endpoint, lp()); root.addView(model, lp()); root.addView(apiKey, lp());

        LinearLayout presets = row();
        Button groq = smallButton("Groq"), nvidia = smallButton("NVIDIA NIM"), cerebras = smallButton("Cerebras");
        presets.addView(groq, weightLp()); presets.addView(nvidia, weightLp()); presets.addView(cerebras, weightLp());
        root.addView(presets, lp());
        groq.setOnClickListener(v -> { endpoint.setText("https://api.groq.com/openai/v1"); model.setText("llama-3.3-70b-versatile"); });
        nvidia.setOnClickListener(v -> { endpoint.setText("https://integrate.api.nvidia.com/v1"); model.setText(""); model.requestFocus(); });
        cerebras.setOnClickListener(v -> { endpoint.setText("https://api.cerebras.ai/v1"); model.setText(""); model.requestFocus(); });

        root.addView(section("GitHub-Projekt"));
        githubToken = field("GitHub Token (Contents: write, Actions: read)", true, false);
        repo = field("Repository, z. B. owner/projekt", false, false);
        baseBranch = field("Basis-Branch", false, false);
        root.addView(githubToken, lp()); root.addView(repo, lp()); root.addView(baseBranch, lp());

        root.addView(section("Aufgabe"));
        task = field("Was soll weiterentwickelt/repariert werden?", false, true);
        task.setMinLines(5);
        task.setGravity(Gravity.TOP | Gravity.START);
        root.addView(task, lp());

        LinearLayout limits = row();
        maxRounds = field("Max. Runden", false, false); maxRounds.setInputType(InputType.TYPE_CLASS_NUMBER);
        contextChars = field("Kontext-Zeichen", false, false); contextChars.setInputType(InputType.TYPE_CLASS_NUMBER);
        limits.addView(maxRounds, weightLp()); limits.addView(contextChars, weightLp());
        root.addView(limits, lp());

        LinearLayout actions1 = row();
        Button save = button("Konfiguration speichern"), test = button("LLM testen");
        actions1.addView(save, weightLp()); actions1.addView(test, weightLp()); root.addView(actions1, lp());

        LinearLayout actions2 = row();
        runButton = button("Agentenlauf starten"); stopButton = button("Stop"); stopButton.setEnabled(false);
        actions2.addView(runButton, weightLp()); actions2.addView(stopButton, weightLp()); root.addView(actions2, lp());

        LinearLayout actions3 = row();
        Button check = button("Letzten Build prüfen"); downloadButton = button("APK-Artefakt laden"); downloadButton.setEnabled(false);
        actions3.addView(check, weightLp()); actions3.addView(downloadButton, weightLp()); root.addView(actions3, lp());

        status = text("Status: bereit", 15, true, Color.rgb(116, 214, 146));
        LinearLayout.LayoutParams st = lp(); st.topMargin = dp(16); root.addView(status, st);

        log = text("", 12, false, Color.rgb(220, 224, 230));
        log.setTypeface(Typeface.MONOSPACE);
        log.setTextIsSelectable(true);
        log.setPadding(dp(12), dp(12), dp(12), dp(12));
        log.setBackgroundColor(Color.rgb(25, 27, 32));
        LinearLayout.LayoutParams logLp = lp(); logLp.topMargin = dp(8); root.addView(log, logLp);

        TextView note = text("Sicherheit: Kein automatischer Merge in main/master. API- und GitHub-Keys werden per Android Keystore verschlüsselt gespeichert. Free-Tier-Limits der Provider gelten weiterhin.", 12, false, Color.rgb(155, 160, 169));
        LinearLayout.LayoutParams noteLp = lp(); noteLp.topMargin = dp(12); root.addView(note, noteLp);

        save.setOnClickListener(v -> {
            try { saveConfig(); toast("Gespeichert"); }
            catch (Exception e) { toast("Speichern fehlgeschlagen: " + e.getMessage()); }
        });
        test.setOnClickListener(v -> testLlm());
        runButton.setOnClickListener(v -> startAgents());
        stopButton.setOnClickListener(v -> { stopRequested = true; appendLog("Stop angefordert – der aktuelle Netzaufruf wird noch beendet."); });
        check.setOnClickListener(v -> checkBuild());
        downloadButton.setOnClickListener(v -> downloadArtifact());

        return scroll;
    }

    private void testLlm() {
        if (isBusy()) return;
        final Models.Config c;
        try { c = readConfig(); requireLlm(c); }
        catch (Exception e) { toast(e.getMessage()); return; }

        worker = new Thread(() -> {
            setBusy(true, "LLM-Test läuft");
            try {
                String answer = new OpenAiClient(c).chat("Reply with exactly: OK", "Connection test", 0.0);
                appendLog("LLM-Test: " + OpenAiClient.abbreviate(answer.trim(), 300));
                setStatus("LLM erreichbar", true);
            } catch (Exception e) {
                appendLog("LLM-Test fehlgeschlagen: " + e.getMessage());
                setStatus("LLM-Fehler", false);
            } finally { setBusy(false, null); }
        }, "llm-test");
        worker.start();
    }

    private void startAgents() {
        if (isBusy()) return;
        stopRequested = false; lastRunId = 0; lastWorkBranch = ""; downloadButton.setEnabled(false);

        final Models.Config c;
        final String userTask;
        try {
            c = readConfig();
            requireLlm(c); requireGitHub(c);
            userTask = task.getText().toString().trim();
            if (userTask.length() < 6) throw new IllegalArgumentException("Bitte eine konkrete Entwicklungsaufgabe eingeben.");
            saveConfig();
        } catch (Exception e) { toast(e.getMessage()); return; }

        worker = new Thread(() -> agentRun(c, userTask), "agent-run");
        worker.start();
    }

    private void agentRun(Models.Config c, String userTask) {
        setBusy(true, "Agenten arbeiten");
        try {
            OpenAiClient llm = new OpenAiClient(c);
            GitHubClient gh = new GitHubClient(c.githubToken, c.repo, this::appendLog);

            appendLog("Prüfe Repository " + c.repo + " …");
            gh.validateRepo(); checkStop();

            appendLog("Lade relevanten Projektkontext …");
            String context = gh.collectContext(c.baseBranch, c.contextChars);
            appendLog("Kontext geladen: " + context.length() + " Zeichen."); checkStop();

            appendLog("[Manager] zerlegt die Aufgabe …");
            String manager = llm.chat(
                    "You are the manager of an autonomous software-engineering team. Produce a concise implementation plan, acceptance criteria, risks and a test strategy. Do not write code yet.",
                    "TASK:\n" + userTask + "\n\nREPOSITORY CONTEXT:\n" + context, 0.15);
            appendLog("[Manager] " + OpenAiClient.abbreviate(manager, 1800)); checkStop();

            appendLog("[Architekt] prüft Struktur und betroffene Dateien …");
            String architect = llm.chat(
                    "You are a senior Android/software architect. Based on the task, manager plan and repository context, decide the smallest robust set of changes. Preserve existing behavior. Explicitly identify build risks and files to inspect/change.",
                    "TASK:\n" + userTask + "\n\nMANAGER PLAN:\n" + manager + "\n\nCONTEXT:\n" + context, 0.1);
            appendLog("[Architekt] " + OpenAiClient.abbreviate(architect, 1800)); checkStop();

            appendLog("[Coder] erzeugt Patch-Bundle …");
            String coderText = llm.chat(coderSystem(),
                    "TASK:\n" + userTask + "\n\nMANAGER:\n" + manager + "\n\nARCHITECT:\n" + architect + "\n\nREPOSITORY CONTEXT:\n" + context, 0.05);
            Models.PatchBundle patch = Models.PatchBundle.fromModelText(coderText);
            appendLog("[Coder] " + patch.files.size() + " Datei(en): " + patch.summary); checkStop();

            appendLog("[Reviewer] prüft den Patch vor dem Commit …");
            String review = llm.chat(reviewerSystem(),
                    "TASK:\n" + userTask + "\n\nARCHITECTURE:\n" + architect + "\n\nPATCH:\n" + patchToJson(patch), 0.0);
            appendLog("[Reviewer] " + OpenAiClient.abbreviate(review, 1600));
            if (!isApproved(review)) {
                appendLog("[Fixer] Reviewer fordert Korrekturen …");
                String fixed = llm.chat(coderSystem(),
                        "TASK:\n" + userTask + "\n\nCURRENT PATCH:\n" + patchToJson(patch) + "\n\nREVIEW FEEDBACK:\n" + review + "\n\nReturn a corrected complete patch bundle.", 0.0);
                patch = Models.PatchBundle.fromModelText(fixed);
                appendLog("[Fixer] Patch korrigiert: " + patch.files.size() + " Datei(en).");
            }
            checkStop();

            appendLog("Erstelle isolierten Arbeits-Branch …");
            String workBranch = gh.createWorkBranch(c.baseBranch);
            lastWorkBranch = workBranch;
            appendLog("Branch: " + workBranch);

            for (int round = 1; round <= c.maxRounds; round++) {
                checkStop();
                appendLog("=== Runde " + round + "/" + c.maxRounds + " ===");
                long pushTime = System.currentTimeMillis();
                gh.applyPatch(workBranch, patch);
                appendLog("Patch gepusht. Warte auf GitHub Actions …");
                Models.BuildResult build = gh.waitForBuild(workBranch, pushTime, 30);
                lastRunId = build.runId;

                if (build.success()) {
                    appendLog("BUILD SUCCESS. Run #" + build.runId);
                    if (!build.htmlUrl.isEmpty()) appendLog(build.htmlUrl);
                    setStatus("Erfolgreich – APK-Build bestanden", true);
                    runOnUiThread(() -> downloadButton.setEnabled(lastRunId > 0));
                    return;
                }

                if (!build.finished()) {
                    appendLog("Kein abgeschlossener Build gefunden. Das Ziel-Repository benötigt einen GitHub-Actions Build, der auf dem ai-agent/* Branch läuft.");
                    setStatus("Build nicht abgeschlossen", false);
                    return;
                }

                appendLog("BUILD FAILED. Build-Logs werden an den Fixer gegeben.");
                if (round >= c.maxRounds) {
                    appendLog("Maximale Rundenzahl erreicht. Branch bleibt zur manuellen Prüfung erhalten: " + workBranch);
                    setStatus("Build fehlgeschlagen – Rundengrenze erreicht", false);
                    return;
                }

                String freshContext = gh.collectContext(workBranch, Math.min(c.contextChars, 90000));
                String failure = OpenAiClient.abbreviate(build.logs, 65000);
                String fixed = llm.chat(coderSystem(),
                        "The previous patch failed CI/build. Diagnose the logs and return a corrected COMPLETE patch bundle containing only files that need to be created or replaced now.\n\nTASK:\n" + userTask + "\n\nBUILD LOGS:\n" + failure + "\n\nCURRENT REPOSITORY CONTEXT:\n" + freshContext, 0.0);
                patch = Models.PatchBundle.fromModelText(fixed);
                appendLog("[Fixer] Neuer Patch vorbereitet: " + patch.files.size() + " Datei(en).");
            }
        } catch (StopException e) {
            appendLog("Agentenlauf gestoppt.");
            setStatus("Gestoppt", false);
        } catch (Exception e) {
            appendLog("FEHLER: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            setStatus("Fehler", false);
        } finally { setBusy(false, null); }
    }

    private void checkBuild() {
        if (isBusy()) return;
        final Models.Config c;
        final String branch;
        try {
            c = readConfig(); requireGitHub(c);
            branch = lastWorkBranch.isEmpty() ? c.baseBranch : lastWorkBranch;
        } catch (Exception e) { toast(e.getMessage()); return; }

        worker = new Thread(() -> {
            setBusy(true, "Build wird geprüft");
            try {
                GitHubClient gh = new GitHubClient(c.githubToken, c.repo, this::appendLog);
                Models.BuildResult b = gh.latestBuild(branch);
                lastRunId = b.runId;
                appendLog("Build auf " + branch + ": " + b.status + " / " + b.conclusion + (b.htmlUrl.isEmpty() ? "" : "\n" + b.htmlUrl));
                boolean ok = b.success();
                setStatus(ok ? "Letzter Build erfolgreich" : "Letzter Build nicht erfolgreich", ok);
                runOnUiThread(() -> downloadButton.setEnabled(ok && lastRunId > 0));
            } catch (Exception e) {
                appendLog("Build-Prüfung fehlgeschlagen: " + e.getMessage());
                setStatus("Build-Prüfung fehlgeschlagen", false);
            } finally { setBusy(false, null); }
        }, "build-check");
        worker.start();
    }

    private void downloadArtifact() {
        if (lastRunId <= 0) { toast("Kein erfolgreicher Build ausgewählt."); return; }
        if (isBusy()) return;
        final Models.Config c;
        final long runId = lastRunId;
        try { c = readConfig(); requireGitHub(c); }
        catch (Exception e) { toast(e.getMessage()); return; }

        worker = new Thread(() -> {
            setBusy(true, "APK wird geladen");
            try {
                GitHubClient gh = new GitHubClient(c.githubToken, c.repo, this::appendLog);
                Uri uri = gh.downloadFirstApkArtifact(this, runId);
                appendLog("APK gespeichert: " + uri);
                setStatus("APK in Downloads gespeichert", true);
                runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        toast("APK gespeichert. Öffne sie aus Downloads/AI-Dev-Agent.");
                    }
                });
            } catch (Exception e) {
                appendLog("APK-Download fehlgeschlagen: " + e.getMessage());
                setStatus("APK-Download fehlgeschlagen", false);
            } finally { setBusy(false, null); }
        }, "artifact-download");
        worker.start();
    }

    private boolean isBusy() {
        if (worker != null && worker.isAlive()) { toast("Es läuft bereits ein Vorgang."); return true; }
        return false;
    }

    private Models.Config readConfig() {
        Models.Config c = new Models.Config();
        c.endpoint = endpoint.getText().toString().trim();
        c.model = model.getText().toString().trim();
        c.apiKey = apiKey.getText().toString().trim();
        c.githubToken = githubToken.getText().toString().trim();
        c.repo = normalizeRepo(repo.getText().toString());
        c.baseBranch = baseBranch.getText().toString().trim().isEmpty() ? "main" : baseBranch.getText().toString().trim();
        c.maxRounds = clamp(parseInt(maxRounds.getText().toString(), 4), 1, 20);
        c.contextChars = clamp(parseInt(contextChars.getText().toString(), 70000), 10000, 250000);
        return c;
    }

    private void saveConfig() throws Exception {
        Models.Config c = readConfig();
        getSharedPreferences(PREF, MODE_PRIVATE).edit()
                .putString("endpoint", c.endpoint).putString("model", c.model).putString("repo", c.repo)
                .putString("baseBranch", c.baseBranch).putInt("maxRounds", c.maxRounds).putInt("contextChars", c.contextChars).apply();
        secretStore.put("apiKey", c.apiKey);
        secretStore.put("githubToken", c.githubToken);
    }

    private void loadConfig() {
        SharedPreferences p = getSharedPreferences(PREF, MODE_PRIVATE);
        endpoint.setText(p.getString("endpoint", "https://api.groq.com/openai/v1"));
        model.setText(p.getString("model", "llama-3.3-70b-versatile"));
        repo.setText(p.getString("repo", ""));
        baseBranch.setText(p.getString("baseBranch", "main"));
        maxRounds.setText(String.valueOf(p.getInt("maxRounds", 4)));
        contextChars.setText(String.valueOf(p.getInt("contextChars", 70000)));
        apiKey.setText(secretStore.get("apiKey"));
        githubToken.setText(secretStore.get("githubToken"));
    }

    private void requireLlm(Models.Config c) {
        if (!c.endpoint.startsWith("https://")) throw new IllegalArgumentException("LLM Base URL muss HTTPS verwenden.");
        if (c.model.isEmpty()) throw new IllegalArgumentException("Modell-ID fehlt.");
        if (c.apiKey.isEmpty()) throw new IllegalArgumentException("LLM API-Key fehlt.");
    }

    private void requireGitHub(Models.Config c) {
        if (c.githubToken.isEmpty()) throw new IllegalArgumentException("GitHub Token fehlt.");
        if (!c.repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Repository muss owner/name sein.");
    }

    private static String coderSystem() {
        return "You are the coding/fixing agent in an autonomous team. Return ONLY one valid JSON object, no markdown fences and no prose outside JSON. Schema: {\"summary\":\"short\",\"commitMessage\":\"short\",\"files\":[{\"path\":\"relative/repo/path\",\"content\":\"FULL replacement file content\"}]}. Every content field must contain the full file, never a diff. Make the smallest complete change that builds. Never include secrets, tokens, keystores, generated binaries, build directories, .git files or absolute paths. Do not edit CI merely to hide failing tests. Preserve existing features unless the task explicitly changes them.";
    }

    private static String reviewerSystem() {
        return "You are an independent senior code reviewer. Check correctness, compilation risks, security, regressions and whether the task is actually satisfied. Return ONLY JSON: {\"approved\":true|false,\"notes\":\"concise actionable feedback\"}. Approve only if the patch is plausibly buildable and complete.";
    }

    private static boolean isApproved(String review) {
        try { return new JSONObject(Models.extractJsonObject(review)).optBoolean("approved", false); }
        catch (Exception e) { return false; }
    }

    private static String patchToJson(Models.PatchBundle patch) throws Exception {
        JSONObject root = new JSONObject();
        root.put("summary", patch.summary); root.put("commitMessage", patch.commitMessage);
        org.json.JSONArray arr = new org.json.JSONArray();
        for (Models.PatchFile f : patch.files) arr.put(new JSONObject().put("path", f.path).put("content", f.content));
        root.put("files", arr);
        return root.toString();
    }

    private void checkStop() throws StopException {
        if (stopRequested || Thread.currentThread().isInterrupted()) throw new StopException();
    }

    private void setBusy(boolean busy, String label) {
        runOnUiThread(() -> {
            runButton.setEnabled(!busy); stopButton.setEnabled(busy);
            if (label != null) status.setText("Status: " + label);
        });
    }

    private void setStatus(String s, boolean good) {
        runOnUiThread(() -> {
            status.setText("Status: " + s);
            status.setTextColor(good ? Color.rgb(116, 214, 146) : Color.rgb(255, 148, 148));
        });
    }

    private void appendLog(String s) {
        if (s == null) return;
        runOnUiThread(() -> {
            String current = log.getText().toString();
            String next = current + (current.isEmpty() ? "" : "\n") + s;
            if (next.length() > 90000) next = next.substring(next.length() - 90000);
            log.setText(next);
        });
    }

    private static int parseInt(String s, int fallback) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; } }
    private static int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }

    private static String normalizeRepo(String raw) {
        String r = raw == null ? "" : raw.trim();
        if (r.startsWith("https://github.com/")) r = r.substring("https://github.com/".length());
        if (r.endsWith(".git")) r = r.substring(0, r.length() - 4);
        while (r.endsWith("/")) r = r.substring(0, r.length() - 1);
        return r;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }

    private TextView section(String s) {
        TextView t = text(s, 17, true, Color.rgb(235, 238, 242));
        LinearLayout.LayoutParams x = lp(); x.topMargin = dp(12); x.bottomMargin = dp(6); t.setLayoutParams(x); return t;
    }

    private EditText field(String hint, boolean secret, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setHintTextColor(Color.rgb(130, 136, 145)); e.setTextColor(Color.WHITE); e.setTextSize(14);
        e.setSingleLine(!multiline); e.setPadding(dp(10), dp(8), dp(10), dp(8));
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else if (multiline) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        else e.setInputType(InputType.TYPE_CLASS_TEXT);
        return e;
    }

    private Button button(String s) { Button b = new Button(this); b.setText(s); b.setTextSize(12); b.setAllCaps(false); return b; }
    private Button smallButton(String s) { Button b = button(s); b.setMinHeight(dp(42)); return b; }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }

    private LinearLayout.LayoutParams lp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(7); return p;
    }

    private LinearLayout.LayoutParams weightLp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(2), dp(2), dp(2), dp(2)); return p;
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void toast(String s) { runOnUiThread(() -> Toast.makeText(this, s, Toast.LENGTH_LONG).show()); }
    private static final class StopException extends Exception {}
}
