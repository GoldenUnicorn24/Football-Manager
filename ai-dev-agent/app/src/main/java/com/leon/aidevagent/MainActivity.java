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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String PREF = "ai_dev_agent_cfg";

    private final List<String> repositories = new ArrayList<>();
    private SecretStore secretStore;
    private SharedPreferences prefs;

    private Spinner projectSpinner;
    private EditText task;
    private TextView status;
    private TextView progress;
    private TextView log;
    private Button runButton;
    private Button stopButton;
    private Button downloadButton;
    private Button detailsButton;

    private volatile boolean stopRequested;
    private volatile long lastRunId;
    private volatile String lastWorkBranch = "";
    private volatile String currentStage = "";
    private Thread worker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        secretStore = new SecretStore(this);
        prefs = getSharedPreferences(PREF, MODE_PRIVATE);

        if (isSetupComplete()) renderHome();
        else renderGithubSetup();
    }

    private boolean isSetupComplete() {
        return prefs.getBoolean("setupDone", false)
                && !secretStore.get("githubToken").isEmpty()
                && !secretStore.get("apiKey").isEmpty();
    }

    private void renderGithubSetup() {
        ScrollView scroll = screen();
        LinearLayout root = root(scroll);

        root.addView(title("AI Dev Agent"));
        root.addView(subtitle("Einmal einrichten. Danach reicht: Projekt wählen, Aufgabe schreiben, Start drücken."));

        TextView step = section("1 von 2 · GitHub verbinden");
        root.addView(step);

        EditText token = field("GitHub Token", true, false);
        token.setText(secretStore.get("githubToken"));
        root.addView(token, lp());

        TextView help = hint("Benötigt Zugriff auf deine Repositories sowie Contents und Actions. Der Token wird verschlüsselt im Android Keystore gespeichert.");
        root.addView(help, lp());

        Button open = button("GitHub Token-Seite öffnen");
        root.addView(open, lp());
        open.setOnClickListener(v -> openUrl("https://github.com/settings/tokens"));

        Button connect = primaryButton("GITHUB VERBINDEN");
        root.addView(connect, bigLp());

        TextView result = hint("");
        root.addView(result, lp());

        connect.setOnClickListener(v -> {
            String value = token.getText().toString().trim();
            if (value.isEmpty()) {
                toast("Bitte zuerst einen GitHub Token einfügen.");
                return;
            }
            if (isBusy()) return;
            worker = new Thread(() -> {
                runOnUiThread(() -> {
                    connect.setEnabled(false);
                    result.setText("Verbindung wird geprüft …");
                });
                try {
                    GitHubClient gh = new GitHubClient(value, "", s -> {});
                    List<String> repos = gh.listRepositories();
                    if (repos.isEmpty()) throw new IllegalStateException("Token funktioniert, aber es wurden keine erreichbaren Repositories gefunden.");
                    secretStore.put("githubToken", value);
                    repositories.clear();
                    repositories.addAll(repos);
                    prefs.edit().putString("repo", repos.get(0)).apply();
                    runOnUiThread(this::renderAiSetup);
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        connect.setEnabled(true);
                        result.setText("Verbindung fehlgeschlagen: " + cleanError(e));
                    });
                }
            }, "setup-github");
            worker.start();
        });

        setContentView(scroll);
    }

    private void renderAiSetup() {
        ScrollView scroll = screen();
        LinearLayout root = root(scroll);

        root.addView(title("AI Dev Agent"));
        root.addView(subtitle("GitHub ist verbunden. Jetzt noch die kostenlose KI verbinden."));

        root.addView(section("2 von 2 · Kostenlose KI"));
        root.addView(text("Groq", 20, true, Color.WHITE), lp());
        root.addView(hint("Standard: GPT-OSS 120B. Du kannst Anbieter und Modell später im Expertenmodus ändern."), lp());

        EditText key = field("Groq API-Key", true, false);
        key.setText(secretStore.get("apiKey"));
        root.addView(key, lp());

        Button open = button("Groq API-Key-Seite öffnen");
        root.addView(open, lp());
        open.setOnClickListener(v -> openUrl("https://console.groq.com/keys"));

        Button connect = primaryButton("KI TESTEN & FERTIG");
        root.addView(connect, bigLp());

        Button back = button("Zurück");
        root.addView(back, lp());
        back.setOnClickListener(v -> renderGithubSetup());

        TextView result = hint("");
        root.addView(result, lp());

        connect.setOnClickListener(v -> {
            String api = key.getText().toString().trim();
            if (api.isEmpty()) {
                toast("Bitte zuerst einen Groq API-Key einfügen.");
                return;
            }
            if (isBusy()) return;
            worker = new Thread(() -> {
                runOnUiThread(() -> {
                    connect.setEnabled(false);
                    result.setText("KI wird getestet …");
                });
                try {
                    Models.Config c = storedConfig();
                    c.endpoint = "https://api.groq.com/openai/v1";
                    c.model = "openai/gpt-oss-120b";
                    c.apiKey = api;
                    String answer = new OpenAiClient(c).chat("Reply with exactly OK.", "Connection test", 0.0);
                    if (answer == null || answer.trim().isEmpty()) throw new IllegalStateException("Leere Antwort vom Modell.");
                    secretStore.put("apiKey", api);
                    prefs.edit()
                            .putString("endpoint", c.endpoint)
                            .putString("model", c.model)
                            .putString("baseBranch", "AUTO")
                            .putString("workflow", "AUTO")
                            .putInt("maxRounds", 6)
                            .putInt("contextChars", 70000)
                            .putBoolean("setupDone", true)
                            .apply();
                    runOnUiThread(this::renderHome);
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        connect.setEnabled(true);
                        result.setText("KI-Test fehlgeschlagen: " + cleanError(e));
                    });
                }
            }, "setup-llm");
            worker.start();
        });

        setContentView(scroll);
    }

    private void renderHome() {
        ScrollView scroll = screen();
        LinearLayout root = root(scroll);

        LinearLayout top = row();
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.addView(title("AI Dev Agent"));
        titleBox.addView(subtitle("Einfach-Modus · v0.2"));
        top.addView(titleBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button expert = smallButton("Experte");
        top.addView(expert);
        root.addView(top, lp());
        expert.setOnClickListener(v -> renderExpert());

        root.addView(section("Projekt"));
        projectSpinner = new Spinner(this);
        root.addView(projectSpinner, lp());

        Button refresh = button("Projekte aktualisieren");
        root.addView(refresh, lp());
        refresh.setOnClickListener(v -> loadRepositories(true));

        root.addView(section("Was soll gemacht werden?"));
        task = field("Zum Beispiel: Repariere das Transfersystem und verbessere das Jugendtraining.", false, true);
        task.setMinLines(6);
        task.setGravity(Gravity.TOP | Gravity.START);
        task.setText(prefs.getString("lastTask", ""));
        root.addView(task, lp());

        runButton = primaryButton("ENTWICKLUNG STARTEN");
        root.addView(runButton, bigLp());

        stopButton = button("STOPPEN");
        stopButton.setEnabled(false);
        root.addView(stopButton, lp());

        status = text("Bereit", 16, true, Color.rgb(116, 214, 146));
        LinearLayout.LayoutParams statusLp = lp();
        statusLp.topMargin = dp(16);
        root.addView(status, statusLp);

        progress = text(progressText(""), 14, false, Color.rgb(215, 220, 228));
        progress.setPadding(dp(12), dp(12), dp(12), dp(12));
        progress.setBackgroundColor(Color.rgb(25, 27, 32));
        root.addView(progress, lp());

        downloadButton = primaryButton("APK INSTALLIEREN");
        downloadButton.setEnabled(lastRunId > 0);
        root.addView(downloadButton, bigLp());

        detailsButton = button("Details anzeigen");
        root.addView(detailsButton, lp());

        log = text("", 12, false, Color.rgb(220, 224, 230));
        log.setTypeface(Typeface.MONOSPACE);
        log.setTextIsSelectable(true);
        log.setPadding(dp(12), dp(12), dp(12), dp(12));
        log.setBackgroundColor(Color.rgb(25, 27, 32));
        log.setVisibility(View.GONE);
        root.addView(log, lp());

        root.addView(hint("Die App arbeitet immer auf einem neuen ai-agent/* Branch. Dein Haupt-Branch wird nicht automatisch überschrieben."), lp());

        runButton.setOnClickListener(v -> startAgents());
        stopButton.setOnClickListener(v -> {
            stopRequested = true;
            appendLog("Stop angefordert. Der aktuelle Netzaufruf wird noch beendet.");
        });
        downloadButton.setOnClickListener(v -> downloadArtifact());
        detailsButton.setOnClickListener(v -> {
            boolean open = log.getVisibility() != View.VISIBLE;
            log.setVisibility(open ? View.VISIBLE : View.GONE);
            detailsButton.setText(open ? "Details ausblenden" : "Details anzeigen");
        });

        setContentView(scroll);
        populateSpinnerFromMemory();
        loadRepositories(false);
    }

    private void renderExpert() {
        ScrollView scroll = screen();
        LinearLayout root = root(scroll);

        LinearLayout top = row();
        root.addView(top, lp());
        TextView t = title("Expertenmodus");
        top.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button back = smallButton("Zurück");
        top.addView(back);
        back.setOnClickListener(v -> renderHome());

        Models.Config c = storedConfig();

        root.addView(section("LLM"));
        EditText endpoint = field("OpenAI-kompatible Base URL", false, false);
        EditText model = field("Modell-ID", false, false);
        EditText api = field("API-Key", true, false);
        endpoint.setText(c.endpoint);
        model.setText(c.model);
        api.setText(c.apiKey);
        root.addView(endpoint, lp());
        root.addView(model, lp());
        root.addView(api, lp());

        root.addView(section("GitHub"));
        EditText ghToken = field("GitHub Token", true, false);
        EditText repo = field("Repository owner/name", false, false);
        EditText base = field("Basis-Branch oder AUTO", false, false);
        EditText workflow = field("Workflow-Datei/ID oder AUTO", false, false);
        ghToken.setText(c.githubToken);
        repo.setText(c.repo);
        base.setText(c.baseBranch);
        workflow.setText(c.workflow);
        root.addView(ghToken, lp());
        root.addView(repo, lp());
        root.addView(base, lp());
        root.addView(workflow, lp());

        root.addView(section("Agenten"));
        EditText rounds = field("Maximale Fix-Runden", false, false);
        EditText context = field("Kontext-Zeichen", false, false);
        rounds.setInputType(InputType.TYPE_CLASS_NUMBER);
        context.setInputType(InputType.TYPE_CLASS_NUMBER);
        rounds.setText(String.valueOf(c.maxRounds));
        context.setText(String.valueOf(c.contextChars));
        root.addView(rounds, lp());
        root.addView(context, lp());

        Button save = primaryButton("EXPERTEN-EINSTELLUNGEN SPEICHERN");
        root.addView(save, bigLp());

        Button test = button("KI-Verbindung testen");
        root.addView(test, lp());

        Button reset = button("Einrichtung neu starten");
        root.addView(reset, lp());

        TextView result = hint("");
        root.addView(result, lp());

        save.setOnClickListener(v -> {
            try {
                Models.Config x = new Models.Config();
                x.endpoint = endpoint.getText().toString().trim();
                x.model = model.getText().toString().trim();
                x.apiKey = api.getText().toString().trim();
                x.githubToken = ghToken.getText().toString().trim();
                x.repo = normalizeRepo(repo.getText().toString());
                x.baseBranch = base.getText().toString().trim().isEmpty() ? "AUTO" : base.getText().toString().trim();
                x.workflow = workflow.getText().toString().trim().isEmpty() ? "AUTO" : workflow.getText().toString().trim();
                x.maxRounds = clamp(parseInt(rounds.getText().toString(), 6), 1, 20);
                x.contextChars = clamp(parseInt(context.getText().toString(), 70000), 10000, 250000);
                requireLlm(x);
                requireGitHub(x);
                saveConfig(x);
                prefs.edit().putBoolean("setupDone", true).apply();
                result.setText("Gespeichert.");
            } catch (Exception e) {
                result.setText("Fehler: " + cleanError(e));
            }
        });

        test.setOnClickListener(v -> {
            Models.Config x = new Models.Config();
            x.endpoint = endpoint.getText().toString().trim();
            x.model = model.getText().toString().trim();
            x.apiKey = api.getText().toString().trim();
            try { requireLlm(x); }
            catch (Exception e) { result.setText(cleanError(e)); return; }

            if (isBusy()) return;
            worker = new Thread(() -> {
                runOnUiThread(() -> result.setText("KI wird getestet …"));
                try {
                    String answer = new OpenAiClient(x).chat("Reply with exactly OK.", "Connection test", 0.0);
                    runOnUiThread(() -> result.setText("KI erreichbar: " + OpenAiClient.abbreviate(answer.trim(), 120)));
                } catch (Exception e) {
                    runOnUiThread(() -> result.setText("Test fehlgeschlagen: " + cleanError(e)));
                }
            }, "expert-test");
            worker.start();
        });

        reset.setOnClickListener(v -> {
            prefs.edit().putBoolean("setupDone", false).apply();
            renderGithubSetup();
        });

        setContentView(scroll);
    }

    private void loadRepositories(boolean announce) {
        if (isBusyQuiet()) {
            if (announce) toast("Gerade läuft bereits ein Vorgang.");
            return;
        }
        String token = secretStore.get("githubToken");
        if (token.isEmpty()) {
            if (announce) toast("GitHub ist noch nicht verbunden.");
            return;
        }

        worker = new Thread(() -> {
            try {
                List<String> repos = new GitHubClient(token, "", s -> {}).listRepositories();
                if (!repos.isEmpty()) {
                    repositories.clear();
                    repositories.addAll(repos);
                    runOnUiThread(() -> {
                        populateSpinnerFromMemory();
                        if (announce) toast(repos.size() + " Projekte geladen.");
                    });
                }
            } catch (Exception e) {
                if (announce) runOnUiThread(() -> toast("Projekte konnten nicht geladen werden: " + cleanError(e)));
            }
        }, "repo-loader");
        worker.start();
    }

    private void populateSpinnerFromMemory() {
        if (projectSpinner == null) return;
        if (repositories.isEmpty()) {
            String saved = prefs.getString("repo", "");
            if (!saved.isEmpty()) repositories.add(saved);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, repositories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        projectSpinner.setAdapter(adapter);

        String selected = prefs.getString("repo", "");
        if (!selected.isEmpty()) {
            int index = repositories.indexOf(selected);
            if (index >= 0) projectSpinner.setSelection(index);
        }
    }

    private void startAgents() {
        if (isBusy()) return;
        stopRequested = false;
        lastRunId = 0;
        lastWorkBranch = "";
        currentStage = "";

        final String userTask = task == null ? "" : task.getText().toString().trim();
        if (userTask.length() < 6) {
            toast("Bitte beschreibe zuerst, was geändert werden soll.");
            return;
        }

        if (projectSpinner == null || projectSpinner.getSelectedItem() == null) {
            toast("Bitte zuerst ein Projekt auswählen.");
            return;
        }

        final Models.Config c = storedConfig();
        c.repo = normalizeRepo(String.valueOf(projectSpinner.getSelectedItem()));
        try {
            requireLlm(c);
            requireGitHub(c);
            saveConfig(c);
            prefs.edit().putString("lastTask", userTask).apply();
        } catch (Exception e) {
            toast(cleanError(e));
            return;
        }

        worker = new Thread(() -> agentRun(c, userTask), "agent-run");
        worker.start();
    }

    private void agentRun(Models.Config c, String userTask) {
        setBusy(true, "Projekt wird vorbereitet …");
        try {
            OpenAiClient llm = new OpenAiClient(c);
            GitHubClient gh = new GitHubClient(c.githubToken, c.repo, this::appendLog);

            appendLog("Projekt: " + c.repo);
            gh.validateRepo();
            checkStop();

            String baseBranch = c.baseBranch;
            if (baseBranch == null || baseBranch.isEmpty() || "AUTO".equalsIgnoreCase(baseBranch)) {
                baseBranch = gh.getDefaultBranch();
                appendLog("Basis-Branch automatisch erkannt: " + baseBranch);
            }

            String workflow = c.workflow;
            if (workflow == null || workflow.isEmpty() || "AUTO".equalsIgnoreCase(workflow)) {
                setStage("Vorbereitung");
                setStatus("Suche passenden APK-Build …", true);
                workflow = gh.detectAndroidWorkflow(baseBranch);
            }

            setStage("Manager");
            setStatus("Manager plant die Aufgabe …", true);
            String context = gh.collectContext(baseBranch, c.contextChars);
            appendLog("Projektkontext: " + context.length() + " Zeichen.");

            String manager = llm.chat(
                    "You are the manager of an autonomous software-engineering team. Produce a concise implementation plan, acceptance criteria, risks and a test strategy. Do not write code yet.",
                    "TASK:\n" + userTask + "\n\nREPOSITORY CONTEXT:\n" + context, 0.15);
            appendLog("[Manager] " + OpenAiClient.abbreviate(manager, 1600));
            checkStop();

            setStage("Architekt");
            setStatus("Architekt prüft das Projekt …", true);
            String architect = llm.chat(
                    "You are a senior Android/software architect. Based on the task, manager plan and repository context, decide the smallest robust set of changes. Preserve existing behavior. Explicitly identify build risks and files to inspect/change.",
                    "TASK:\n" + userTask + "\n\nMANAGER PLAN:\n" + manager + "\n\nCONTEXT:\n" + context, 0.1);
            appendLog("[Architekt] " + OpenAiClient.abbreviate(architect, 1600));
            checkStop();

            setStage("Coder");
            setStatus("Coder programmiert die Änderungen …", true);
            String coderText = llm.chat(coderSystem(),
                    "TASK:\n" + userTask + "\n\nMANAGER:\n" + manager + "\n\nARCHITECT:\n" + architect + "\n\nREPOSITORY CONTEXT:\n" + context, 0.05);
            Models.PatchBundle patch = Models.PatchBundle.fromModelText(coderText);
            appendLog("[Coder] " + patch.files.size() + " Datei(en): " + patch.summary);
            checkStop();

            setStage("Reviewer");
            setStatus("Reviewer kontrolliert den Code …", true);
            String review = llm.chat(reviewerSystem(),
                    "TASK:\n" + userTask + "\n\nARCHITECTURE:\n" + architect + "\n\nPATCH:\n" + patchToJson(patch), 0.0);
            appendLog("[Reviewer] " + OpenAiClient.abbreviate(review, 1600));

            if (!isApproved(review)) {
                setStage("Fixer");
                setStatus("Fixer korrigiert den ersten Review …", true);
                String fixed = llm.chat(coderSystem(),
                        "TASK:\n" + userTask + "\n\nCURRENT PATCH:\n" + patchToJson(patch) + "\n\nREVIEW FEEDBACK:\n" + review + "\n\nReturn a corrected complete patch bundle.", 0.0);
                patch = Models.PatchBundle.fromModelText(fixed);
                appendLog("[Fixer] Patch korrigiert: " + patch.files.size() + " Datei(en).");
            }
            checkStop();

            String workBranch = gh.createWorkBranch(baseBranch);
            lastWorkBranch = workBranch;
            appendLog("Arbeits-Branch: " + workBranch);

            for (int round = 1; round <= c.maxRounds; round++) {
                checkStop();
                setStage("Build");
                setStatus("Build-Runde " + round + "/" + c.maxRounds + " …", true);
                appendLog("=== Build-Runde " + round + "/" + c.maxRounds + " ===");

                gh.applyPatch(workBranch, patch);
                long dispatchTime = System.currentTimeMillis();
                gh.dispatchWorkflow(workflow, workBranch);
                appendLog("GitHub Actions gestartet.");

                Models.BuildResult build = gh.waitForBuild(workBranch, dispatchTime, 40);
                lastRunId = build.runId;

                if (build.success()) {
                    currentStage = "Fertig";
                    setStatus("Fertig – APK erfolgreich gebaut", true);
                    updateProgress();
                    appendLog("BUILD SUCCESS. Run #" + build.runId);
                    runOnUiThread(() -> {
                        if (downloadButton != null) downloadButton.setEnabled(lastRunId > 0);
                    });
                    return;
                }

                if (!build.finished()) {
                    throw new IllegalStateException("Der GitHub-Actions-Build wurde nicht rechtzeitig abgeschlossen.");
                }

                if (round >= c.maxRounds) {
                    setStatus("Build fehlgeschlagen – Rundengrenze erreicht", false);
                    appendLog("Arbeits-Branch bleibt erhalten: " + workBranch);
                    return;
                }

                setStage("Fixer");
                setStatus("Buildfehler gefunden – Fixer arbeitet …", true);
                String freshContext = gh.collectContext(workBranch, Math.min(c.contextChars, 90000));
                String failure = OpenAiClient.abbreviate(build.logs, 65000);
                String fixed = llm.chat(coderSystem(),
                        "The previous patch failed CI/build. Diagnose the logs and return a corrected COMPLETE patch bundle containing only files that need to be created or replaced now.\n\nTASK:\n"
                                + userTask + "\n\nBUILD LOGS:\n" + failure + "\n\nCURRENT REPOSITORY CONTEXT:\n" + freshContext, 0.0);
                patch = Models.PatchBundle.fromModelText(fixed);
                appendLog("[Fixer] Neuer Patch vorbereitet: " + patch.files.size() + " Datei(en).");
            }
        } catch (StopException e) {
            setStatus("Gestoppt", false);
            appendLog("Agentenlauf gestoppt.");
        } catch (Exception e) {
            setStatus("Fehler: " + cleanError(e), false);
            appendLog("FEHLER: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            setBusy(false, null);
        }
    }

    private void downloadArtifact() {
        if (lastRunId <= 0) {
            toast("Es gibt noch keine erfolgreiche APK.");
            return;
        }
        if (isBusy()) return;

        Models.Config c = storedConfig();
        if (projectSpinner != null && projectSpinner.getSelectedItem() != null) {
            c.repo = normalizeRepo(String.valueOf(projectSpinner.getSelectedItem()));
        }

        final long runId = lastRunId;
        worker = new Thread(() -> {
            setBusy(true, "APK wird geladen …");
            try {
                GitHubClient gh = new GitHubClient(c.githubToken, c.repo, this::appendLog);
                Uri uri = gh.downloadFirstApkArtifact(this, runId);
                setStatus("APK gespeichert – Installer wird geöffnet", true);
                runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        toast("APK gespeichert. Öffne Downloads/AI-Dev-Agent.");
                    }
                });
            } catch (Exception e) {
                setStatus("APK-Download fehlgeschlagen", false);
                appendLog("APK-Download fehlgeschlagen: " + e.getMessage());
            } finally {
                setBusy(false, null);
            }
        }, "artifact-download");
        worker.start();
    }

    private Models.Config storedConfig() {
        Models.Config c = new Models.Config();
        c.endpoint = prefs.getString("endpoint", "https://api.groq.com/openai/v1");
        c.model = prefs.getString("model", "openai/gpt-oss-120b");
        c.apiKey = secretStore.get("apiKey");
        c.githubToken = secretStore.get("githubToken");
        c.repo = normalizeRepo(prefs.getString("repo", ""));
        c.baseBranch = prefs.getString("baseBranch", "AUTO");
        c.workflow = prefs.getString("workflow", "AUTO");
        c.maxRounds = clamp(prefs.getInt("maxRounds", 6), 1, 20);
        c.contextChars = clamp(prefs.getInt("contextChars", 70000), 10000, 250000);
        return c;
    }

    private void saveConfig(Models.Config c) throws Exception {
        prefs.edit()
                .putString("endpoint", c.endpoint)
                .putString("model", c.model)
                .putString("repo", c.repo)
                .putString("baseBranch", c.baseBranch)
                .putString("workflow", c.workflow)
                .putInt("maxRounds", c.maxRounds)
                .putInt("contextChars", c.contextChars)
                .apply();
        secretStore.put("apiKey", c.apiKey);
        secretStore.put("githubToken", c.githubToken);
    }

    private void requireLlm(Models.Config c) {
        if (c.endpoint == null || !c.endpoint.startsWith("https://")) throw new IllegalArgumentException("LLM Base URL muss HTTPS verwenden.");
        if (c.model == null || c.model.trim().isEmpty()) throw new IllegalArgumentException("Modell-ID fehlt.");
        if (c.apiKey == null || c.apiKey.trim().isEmpty()) throw new IllegalArgumentException("KI API-Key fehlt.");
    }

    private void requireGitHub(Models.Config c) {
        if (c.githubToken == null || c.githubToken.trim().isEmpty()) throw new IllegalArgumentException("GitHub Token fehlt.");
        if (c.repo == null || !c.repo.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Bitte ein gültiges GitHub-Projekt auswählen.");
    }

    private static String coderSystem() {
        return "You are the coding/fixing agent in an autonomous team. Return ONLY one valid JSON object, no markdown fences and no prose outside JSON. Schema: {\"summary\":\"short\",\"commitMessage\":\"short\",\"files\":[{\"path\":\"relative/repo/path\",\"content\":\"FULL replacement file content\"}]}. Every content field must contain the full file, never a diff. Make the smallest complete change that builds. Never include secrets, tokens, keystores, generated binaries, build directories, .git files or absolute paths. Do not edit CI merely to hide failing tests. Preserve existing features unless the task explicitly changes them.";
    }

    private static String reviewerSystem() {
        return "You are an independent senior code reviewer. Check correctness, compilation risks, security, regressions and whether the task is actually satisfied. Return ONLY JSON: {\"approved\":true|false,\"notes\":\"concise actionable feedback\"}. Approve only if the patch is plausibly buildable and complete.";
    }

    private static boolean isApproved(String review) {
        try {
            return new JSONObject(Models.extractJsonObject(review)).optBoolean("approved", false);
        } catch (Exception e) {
            return false;
        }
    }

    private static String patchToJson(Models.PatchBundle patch) throws Exception {
        JSONObject root = new JSONObject();
        root.put("summary", patch.summary);
        root.put("commitMessage", patch.commitMessage);
        org.json.JSONArray arr = new org.json.JSONArray();
        for (Models.PatchFile f : patch.files) {
            arr.put(new JSONObject().put("path", f.path).put("content", f.content));
        }
        root.put("files", arr);
        return root.toString();
    }

    private void setStage(String stage) {
        currentStage = stage;
        updateProgress();
    }

    private void updateProgress() {
        runOnUiThread(() -> {
            if (progress != null) progress.setText(progressText(currentStage));
        });
    }

    private static String progressText(String active) {
        String[] stages = {"Manager", "Architekt", "Coder", "Reviewer", "Build"};
        int activeIndex = stageIndex(active);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stages.length; i++) {
            String mark;
            if ("Fertig".equals(active) || i < activeIndex) mark = "✓";
            else if (i == activeIndex) mark = "→";
            else mark = "○";
            sb.append(mark).append("  ").append(stages[i]);
            if (i < stages.length - 1) sb.append("\n");
        }
        if ("Fixer".equals(active)) sb.append("\n→  Fixer korrigiert");
        if ("Vorbereitung".equals(active)) sb.insert(0, "→  Vorbereitung\n");
        if ("Fertig".equals(active)) sb.append("\n✓  APK fertig");
        return sb.toString();
    }

    private static int stageIndex(String stage) {
        if ("Manager".equals(stage)) return 0;
        if ("Architekt".equals(stage)) return 1;
        if ("Coder".equals(stage)) return 2;
        if ("Reviewer".equals(stage)) return 3;
        if ("Build".equals(stage) || "Fixer".equals(stage)) return 4;
        return -1;
    }

    private void checkStop() throws StopException {
        if (stopRequested || Thread.currentThread().isInterrupted()) throw new StopException();
    }

    private void setBusy(boolean busy, String label) {
        runOnUiThread(() -> {
            if (runButton != null) runButton.setEnabled(!busy);
            if (stopButton != null) stopButton.setEnabled(busy);
            if (label != null && status != null) status.setText(label);
        });
    }

    private void setStatus(String s, boolean good) {
        runOnUiThread(() -> {
            if (status != null) {
                status.setText(s);
                status.setTextColor(good ? Color.rgb(116, 214, 146) : Color.rgb(255, 148, 148));
            }
        });
    }

    private void appendLog(String s) {
        if (s == null) return;
        runOnUiThread(() -> {
            if (log == null) return;
            String current = log.getText().toString();
            String next = current + (current.isEmpty() ? "" : "\n") + s;
            if (next.length() > 90000) next = next.substring(next.length() - 90000);
            log.setText(next);
        });
    }

    private boolean isBusy() {
        if (worker != null && worker.isAlive()) {
            toast("Es läuft bereits ein Vorgang.");
            return true;
        }
        return false;
    }

    private boolean isBusyQuiet() {
        return worker != null && worker.isAlive();
    }

    private ScrollView screen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(16, 17, 20));
        return scroll;
    }

    private LinearLayout root(ScrollView scroll) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(30));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    private TextView title(String s) {
        return text(s, 28, true, Color.WHITE);
    }

    private TextView subtitle(String s) {
        TextView t = text(s, 14, false, Color.rgb(185, 190, 198));
        t.setPadding(0, 0, 0, dp(10));
        return t;
    }

    private TextView section(String s) {
        TextView t = text(s, 17, true, Color.rgb(235, 238, 242));
        t.setPadding(0, dp(10), 0, dp(4));
        return t;
    }

    private TextView hint(String s) {
        return text(s, 12, false, Color.rgb(155, 160, 169));
    }

    private EditText field(String hint, boolean secret, boolean multiline) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.rgb(130, 136, 145));
        e.setTextColor(Color.WHITE);
        e.setTextSize(14);
        e.setSingleLine(!multiline);
        e.setPadding(dp(10), dp(8), dp(10), dp(8));
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else if (multiline) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        else e.setInputType(InputType.TYPE_CLASS_TEXT);
        return e;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(13);
        b.setAllCaps(false);
        return b;
    }

    private Button primaryButton(String s) {
        Button b = button(s);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(54));
        return b;
    }

    private Button smallButton(String s) {
        Button b = button(s);
        b.setMinHeight(dp(42));
        return b;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(7);
        return p;
    }

    private LinearLayout.LayoutParams bigLp() {
        LinearLayout.LayoutParams p = lp();
        p.topMargin = dp(8);
        p.bottomMargin = dp(10);
        return p;
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast("Link konnte nicht geöffnet werden.");
        }
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return fallback; }
    }

    private static int clamp(int n, int min, int max) {
        return Math.max(min, Math.min(max, n));
    }

    private static String normalizeRepo(String raw) {
        String r = raw == null ? "" : raw.trim();
        if (r.startsWith("https://github.com/")) r = r.substring("https://github.com/".length());
        if (r.endsWith(".git")) r = r.substring(0, r.length() - 4);
        while (r.endsWith("/")) r = r.substring(0, r.length() - 1);
        return r;
    }

    private static String cleanError(Exception e) {
        String m = e.getMessage();
        if (m == null || m.trim().isEmpty()) return e.getClass().getSimpleName();
        return OpenAiClient.abbreviate(m.replace("\n", " "), 500);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        runOnUiThread(() -> Toast.makeText(this, s, Toast.LENGTH_LONG).show());
    }

    private static final class StopException extends Exception {}
}
