package com.steadfast.app.smoke;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Real install-over regression. Raw framework storage access works against the delivered V1 APK. */
final class UpgradeRegression {
    private static final String CERT = "f362708af345c607f91700a453893cf0c50731a60b38fcc13be85d0e579e3c58";
    private final Instrumentation instrumentation;
    private final Context context;
    private final File fixture, settings;

    UpgradeRegression(Instrumentation instrumentation, Bundle arguments) {
        this.instrumentation = instrumentation;
        context = instrumentation.getTargetContext();
        int user = context.getApplicationInfo().uid / 100000;
        check(user >= 10 && arguments != null && String.valueOf(user).equals(arguments.getString("fixtureUser")),
            "Requires explicit fixtureUser matching an isolated secondary user; owner0 forbidden");
        check("com.steadfast.app".equals(context.getPackageName()), "Wrong package");
        fixture = new File(context.getNoBackupFilesDir(), "v11-upgrade-fixture.json");
        settings = new File(context.getFilesDir(), "datastore/steadfast_settings.preferences_pb");
    }

    String run(String mode) throws Exception {
        PreferenceWire.main(new String[0]);
        switch (mode) {
            case "upgradeSeed": seed(); break;
            case "upgradeCheckV1": verify(false); break;
            case "upgradeVerify": verify(true); break;
            case "upgradeUnlockFixture": unlockFixture(); break;
            case "upgradeFeatures": features(false); break;
            case "upgradeFeaturesRelaunch": features(true); break;
            case "upgradeDataAudit": assertVersion(true); assertBaseline(true); break;
            default: throw new AssertionError("Unknown upgrade mode " + mode);
        }
        return "OK " + mode + ": identity=" + identity() + " schema=1; original rows/settings compared; fixture SHA256=" +
            sha256(Files.readAllBytes(fixture.toPath())) + "\n";
    }

    private void seed() throws Exception {
        check(!fixture.exists(), "Fixture already exists; never overwrite an upgrade baseline");
        assertVersion(false);
        check(context.getDatabasePath("steadfast.db").isFile(), "Launch V1 once, then force-stop this user before seeding");
        try (SQLiteDatabase db = database(false)) {
            check(db.getVersion() == 1, "Expected Room schema1");
            for (String table : new String[]{"streak_sessions", "journal_entries", "bible_chapter_completions"}) {
                try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
                    check(cursor.moveToFirst() && cursor.getLong(0) == 0, "Seed refuses existing user rows in " + table);
                }
            }
            long now = System.currentTimeMillis(), day = 86400000L;
            db.beginTransaction();
            try {
                String sessions = "INSERT INTO streak_sessions (id,recoveryFocusId,startEpochMillis,startZoneId,endEpochMillis,createdAtEpochMillis) VALUES ";
                db.execSQL(sessions + "(101,'alcohol',?,'America/New_York',?,?)", new Object[]{now - 40 * day, now - 12 * day, now - 40 * day});
                db.execSQL(sessions + "(205,'pornography',?,'Asia/Beirut',?,?)", new Object[]{now - 12 * day, now - 3 * day, now - 12 * day});
                db.execSQL(sessions + "(309,'pornography',?,'UTC',NULL,?)", new Object[]{now - 3 * day, now - 3 * day});
                String journals = "INSERT INTO journal_entries (id,createdAtEpochMillis,updatedAtEpochMillis,body,context,streakSessionId) VALUES ";
                db.execSQL(journals + "(401,?,?,?,'GENERAL',101)", new Object[]{now - 30 * day, now - 29 * day, "Synthetic upgrade note — grâce 🙏\nline two | null is not a delimiter"});
                db.execSQL(journals + "(507,?,?,?,'RESTART',205)", new Object[]{now - 12 * day, now - 12 * day, "Synthetic restart reflection"});
                db.execSQL(journals + "(609,?,?,?,'URGE',309)", new Object[]{now - day, now - day, "Synthetic urge: chose a walk and prayer"});
                db.execSQL(journals + "(711,?,?,?,NULL,NULL)", new Object[]{now - 10000, now - 5000, "Synthetic legacy context-free note"});
                for (int[] chapter : new int[][]{{1,1},{1,2},{19,23},{20,3},{40,5},{43,3},{45,8},{66,22}})
                    db.execSQL("INSERT INTO bible_chapter_completions (bookId,chapter) VALUES (?,?)", new Object[]{chapter[0], chapter[1]});
                // A committed restore leaves this token table; no pending encrypted recovery is fabricated.
                db.execSQL("CREATE TABLE IF NOT EXISTS steadfast_restore_commit (id INTEGER NOT NULL PRIMARY KEY, token TEXT NOT NULL)");
                db.execSQL("INSERT OR REPLACE INTO steadfast_restore_commit VALUES (1,'8bc49759-76cf-4bd6-a44b-e963356711f7')");
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
        TreeMap<String, PreferenceWire.Value> prefs = preferences();
        prefs.put("onboarding_complete", PreferenceWire.number(8, 1));
        prefs.put("theme_mode", PreferenceWire.string("DARK"));
        prefs.put("app_lock_enabled", PreferenceWire.number(8, 1));
        prefs.put("recovery_focus_id", PreferenceWire.string("pornography"));
        prefs.put("reading_goal_min", PreferenceWire.number(24, 4));
        prefs.put("ot_current_book_id", PreferenceWire.number(24, 19));
        prefs.put("ot_current_chapter", PreferenceWire.number(24, 24));
        prefs.put("nt_current_book_id", PreferenceWire.number(24, 45));
        prefs.put("nt_current_chapter", PreferenceWire.number(24, 9));
        prefs.put("shown_milestones", PreferenceWire.string("d1,d3,d7,d14,d21,d30,d60,d90,m3,m6,m9,y1"));
        prefs.put("last_background_epoch", PreferenceWire.number(32, System.currentTimeMillis() - 60000));
        writePreferences(prefs);
        JSONObject baseline = new JSONObject().put("format", 1).put("identity", identity())
            .put("database", databaseSnapshot()).put("preferences", preferenceSnapshot())
            .put("recoveryFiles", recoveryFiles()).put("seededAt", System.currentTimeMillis());
        write(fixture, baseline);
        assertBaseline(false);
    }

    private void verify(boolean updated) throws Exception {
        assertVersion(updated);
        assertBaseline(false);
        launch();
        await("Steadfast is Locked");
        check(find(root(), "I'm Struggling") == null && find(root(), "Scripture Progress") == null &&
            find(root(), "Synthetic restart reflection") == null, "Protected content exposed while AppLock enabled");
        check(find(root(), "Unlock App") != null, "AppLock retry action absent");
        assertBaseline(false);
        JSONObject proof = new JSONObject().put("identity", identity()).put("database", databaseSnapshot())
            .put("preferences", preferenceSnapshot()).put("lockVisible", true);
        write(proof(updated ? "locked-v11" : "locked-v1"), proof);
    }

    private void unlockFixture() throws Exception {
        assertVersion(true);
        check(proof("locked-v11").isFile(), "Must prove preserved AppLock on V1.1 before synthetic-only disable");
        assertBaseline(false);
        // Separate force-stopped instrumentation mode: never edit a live DataStore instance.
        TreeMap<String, PreferenceWire.Value> prefs = preferences();
        prefs.put("app_lock_enabled", PreferenceWire.number(8, 0));
        writePreferences(prefs);
        write(proof("synthetic-unlocked"), new JSONObject().put("uid", context.getApplicationInfo().uid));
        assertBaseline(true);
    }

    private void features(boolean relaunch) throws Exception {
        assertVersion(true);
        check(proof("synthetic-unlocked").isFile(), "Synthetic-only unlock marker missing");
        assertBaseline(true);
        TreeMap<String, JSONObject> catalog = catalog();
        Set<String> usedBefore = used();
        String previous = preferenceString("verse_deck_last");
        if (relaunch) {
            JSONObject saved = read(proof("features"));
            equal(saved.getJSONObject("preferences"), preferenceSnapshot(), "Relaunch lost persisted settings/deck", true);
            check(preference("bible_testament_filter").equals("integer:2"), "Bible filter lost after process stop");
            check(preference("journey_tab").equals("integer:2") && preference("journal_filter").equals("string:URGE"), "Journey choices lost after process stop");
        }
        launch();
        await("I'm Struggling");
        click("Bible");
        await("Scripture Progress");
        if (!relaunch) click("New Testament (27)");
        else {
            AccessibilityNodeInfo chip = await("New Testament (27)");
            boolean selected = false;
            for (AccessibilityNodeInfo node = chip; node != null; node = node.getParent()) selected |= node.isSelected() || node.isChecked();
            check(selected, "Relaunch UI did not restore NT selection");
        }
        awaitPreference("bible_testament_filter", "integer:2");
        click("Journey");
        if (!relaunch) { click("Journal"); click("URGE"); }
        else {
            await("Synthetic urge: chose a walk and prayer");
            check(find(root(), "Synthetic restart reflection") == null && find(root(), "Synthetic legacy context-free note") == null,
                "Relaunch UI did not restore the journal context filter");
        }
        awaitPreference("journey_tab", "integer:2");
        awaitPreference("journal_filter", "string:URGE");
        click("Dashboard");
        click("I'm Struggling");
        String first = awaitNewDraw(previous, usedBefore, catalog);
        Set<String> afterFirst = used();
        click("Give Me Another Verse");
        awaitNewDraw(first, afterFirst, catalog);
        assertBaseline(true);
        write(proof(relaunch ? "features-relaunch" : "features"), new JSONObject()
            .put("preferences", preferenceSnapshot()).put("catalogCount", catalog.size()).put("database", databaseSnapshot()));
    }

    private String awaitNewDraw(String previous, Set<String> before, Map<String, JSONObject> catalog) throws Exception {
        long end = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < end) {
            String id = preferenceString("verse_deck_last");
            if (!id.isEmpty() && !id.equals(previous)) {
                check(catalog.containsKey(id), "Deck ID absent from catalog");
                Set<String> now = used();
                check(!before.contains(id) && now.containsAll(before) && now.size() == before.size() + 1 && now.contains(id), "Draw repeated/lost persisted deck entries");
                JSONObject passage = catalog.get(id);
                await(passage.getString("reference") + " · WEB");
                await("“" + passage.getString("text") + "”");
                return id;
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("New deck draw was not persisted");
    }

    private TreeMap<String, JSONObject> catalog() throws Exception {
        JSONArray entries;
        try (java.io.InputStream stream = context.getAssets().open("scripture_catalog.json")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            entries = new JSONArray(bytes.toString("UTF-8"));
        }
        check(entries.length() == 500, "Optimized APK must contain exactly 500 passages");
        TreeMap<String, JSONObject> byId = new TreeMap<>();
        Set<String> references = new HashSet<>(), testaments = new HashSet<>();
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            check(byId.put(entry.getString("id"), entry) == null && references.add(entry.getString("reference")), "Duplicate catalog passage");
            check(!entry.getString("text").trim().isEmpty() && !entry.getString("text").startsWith("Open your Bible"), "Catalog text missing");
            testaments.add(entry.getString("testament"));
        }
        check(testaments.equals(new HashSet<>(Arrays.asList("OT", "NT"))), "Both testaments required");
        return byId;
    }

    private void assertBaseline(boolean syntheticUnlocked) throws Exception {
        JSONObject baseline = read(fixture), currentIdentity = identity(), oldIdentity = baseline.getJSONObject("identity");
        for (String field : new String[]{"package", "uid", "certificates"})
            check(oldIdentity.get(field).toString().equals(currentIdentity.get(field).toString()), "Install changed identity " + field);
        equal(baseline.getJSONObject("database"), databaseSnapshot(), "Upgrade changed original SQLite schema/rows", false);
        equal(baseline.getJSONObject("recoveryFiles"), recoveryFiles(), "Upgrade changed existing backup recovery state", false);
        JSONObject expected = new JSONObject(baseline.getJSONObject("preferences").toString());
        if (syntheticUnlocked) expected.put("app_lock_enabled", "boolean:false");
        JSONObject actual = preferenceSnapshot();
        for (Iterator<String> keys = expected.keys(); keys.hasNext();) {
            String key = keys.next();
            check(actual.has(key), "Lost V1 preference " + key);
            if (!key.equals("last_background_epoch")) check(expected.get(key).equals(actual.get(key)), "Changed V1 preference " + key);
            else checkBackground(expected.getString(key), actual.getString(key));
        }
    }

    private void assertVersion(boolean updated) throws Exception {
        JSONObject id = identity();
        check(id.getLong("versionCode") == (updated ? 2 : 1) && id.getString("versionName").equals(updated ? "1.1.0" : "1.0"), "Wrong APK version for phase");
        JSONArray certs = id.getJSONArray("certificates");
        check(certs.length() == 1 && CERT.equals(certs.getString(0)), "APK differs from delivered private V1 certificate");
    }

    private JSONObject identity() throws Exception {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
        ArrayList<String> certificates = new ArrayList<>();
        for (Signature signature : info.signingInfo.getApkContentsSigners()) certificates.add(sha256(signature.toByteArray()));
        java.util.Collections.sort(certificates);
        return new JSONObject().put("package", info.packageName).put("uid", context.getApplicationInfo().uid)
            .put("versionCode", info.getLongVersionCode()).put("versionName", info.versionName).put("certificates", new JSONArray(certificates));
    }

    private JSONObject databaseSnapshot() throws Exception {
        JSONObject result = new JSONObject(), tables = new JSONObject();
        try (SQLiteDatabase db = database(true)) {
            result.put("userVersion", db.getVersion());
            try (Cursor schema = db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name", null)) { result.put("schema", cursorRows(schema)); }
            try (Cursor names = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null)) {
                while (names.moveToNext()) {
                    String table = names.getString(0), quoted = "\"" + table.replace("\"", "\"\"") + "\"";
                    try (Cursor columns = db.rawQuery("SELECT * FROM " + quoted + " LIMIT 0", null)) {
                        StringBuilder order = new StringBuilder();
                        for (int i = 0; i < columns.getColumnCount(); i++) { if (i > 0) order.append(','); order.append(i + 1); }
                        try (Cursor rows = db.rawQuery("SELECT * FROM " + quoted + " ORDER BY " + order, null)) {
                            tables.put(table, new JSONObject().put("columns", new JSONArray(Arrays.asList(rows.getColumnNames()))).put("rows", cursorRows(rows)));
                        }
                    }
                }
            }
        }
        return result.put("tables", tables);
    }

    private JSONArray cursorRows(Cursor cursor) throws Exception {
        JSONArray rows = new JSONArray();
        while (cursor.moveToNext()) {
            JSONArray row = new JSONArray();
            for (int column = 0; column < cursor.getColumnCount(); column++) {
                int type = cursor.getType(column);
                String value = type == Cursor.FIELD_TYPE_NULL ? "" : type == Cursor.FIELD_TYPE_BLOB ?
                    android.util.Base64.encodeToString(cursor.getBlob(column), android.util.Base64.NO_WRAP) : cursor.getString(column);
                row.put(new JSONObject().put("type", type).put("value", value));
            }
            rows.put(row);
        }
        return rows;
    }

    private JSONObject recoveryFiles() throws Exception {
        JSONObject files = new JSONObject();
        for (String name : new String[]{"restore-recovery.enc", "restore-recovery.enc.bak", "restore-recovery.enc.new"}) {
            File file = new File(context.getNoBackupFilesDir(), name);
            files.put(name, file.isFile() ? sha256(Files.readAllBytes(file.toPath())) : JSONObject.NULL);
        }
        File legacy = new File(context.getFilesDir(), "pre_restore_snapshot.json");
        files.put("pre_restore_snapshot.json", legacy.isFile() ? sha256(Files.readAllBytes(legacy.toPath())) : JSONObject.NULL);
        return files;
    }

    private SQLiteDatabase database(boolean readOnly) {
        return SQLiteDatabase.openDatabase(context.getDatabasePath("steadfast.db").getPath(), null,
            readOnly ? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE);
    }
    private TreeMap<String, PreferenceWire.Value> preferences() throws Exception {
        return settings.exists() ? PreferenceWire.read(Files.readAllBytes(settings.toPath())) : new TreeMap<>();
    }
    private JSONObject preferenceSnapshot() throws Exception {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, PreferenceWire.Value> entry : preferences().entrySet()) result.put(entry.getKey(), entry.getValue().canonical);
        return result;
    }
    private String preference(String key) throws Exception {
        PreferenceWire.Value value = preferences().get(key); return value == null ? "" : value.canonical;
    }
    private String preferenceString(String key) throws Exception {
        String value = preference(key); check(value.isEmpty() || value.startsWith("string:"), "Wrong preference type " + key);
        return value.isEmpty() ? "" : value.substring(7);
    }
    private Set<String> used() throws Exception {
        String value = preferenceString("verse_deck_used");
        return value.isEmpty() ? new HashSet<>() : new HashSet<>(Arrays.asList(value.split(",")));
    }
    private void writePreferences(TreeMap<String, PreferenceWire.Value> prefs) throws Exception {
        check(settings.getParentFile().isDirectory() || settings.getParentFile().mkdirs(), "Cannot create private DataStore directory");
        Files.write(settings.toPath(), PreferenceWire.write(prefs));
        equal(new JSONObject(canonical(prefs)), preferenceSnapshot(), "Preference write verification failed", false);
    }
    private Map<String, String> canonical(Map<String, PreferenceWire.Value> prefs) {
        TreeMap<String, String> result = new TreeMap<>();
        for (Map.Entry<String, PreferenceWire.Value> entry : prefs.entrySet()) result.put(entry.getKey(), entry.getValue().canonical);
        return result;
    }

    private void launch() { instrumentation.startActivitySync(new Intent().setClassName(context.getPackageName(), "com.steadfast.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    private AccessibilityNodeInfo root() { return instrumentation.getUiAutomation().getRootInActiveWindow(); }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        if (text.contentEquals(node.getText() == null ? "" : node.getText()) || text.contentEquals(node.getContentDescription() == null ? "" : node.getContentDescription())) return node;
        for (int i = 0; i < node.getChildCount(); i++) { AccessibilityNodeInfo found = find(node.getChild(i), text); if (found != null) return found; }
        return null;
    }
    private AccessibilityNodeInfo scrollable(AccessibilityNodeInfo node) {
        if (node == null || node.isScrollable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) { AccessibilityNodeInfo found = scrollable(node.getChild(i)); if (found != null) return found; }
        return null;
    }
    private AccessibilityNodeInfo await(String text) {
        long end = SystemClock.uptimeMillis() + 20000, lastScroll = 0;
        int attempts = 0;
        while (SystemClock.uptimeMillis() < end) {
            AccessibilityNodeInfo window = root(), found = find(window, text);
            if (found != null) return found;
            if (SystemClock.uptimeMillis() - lastScroll > 800 && attempts < 10) {
                AccessibilityNodeInfo list = scrollable(window);
                if (list != null) list.performAction(attempts++ < 5 ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                lastScroll = SystemClock.uptimeMillis();
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("UI text absent: " + text);
    }
    private void click(String text) {
        await(text);
        long end = SystemClock.uptimeMillis() + 5000;
        do {
            // Navigation can replace the accessibility node between finding it and clicking it.
            AccessibilityNodeInfo node = find(root(), text);
            while (node != null && !node.isClickable()) node = node.getParent();
            if (node != null && node.isEnabled() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < end);
        throw new AssertionError("Cannot click " + text);
    }
    private void awaitPreference(String key, String expected) throws Exception {
        long end = SystemClock.uptimeMillis() + 10000;
        while (SystemClock.uptimeMillis() < end) { if (preference(key).equals(expected)) return; SystemClock.sleep(100); }
        throw new AssertionError("Preference not persisted: " + key + " expected " + expected + " actual " + preference(key));
    }

    private void checkBackground(String before, String after) {
        check(before.startsWith("long:") && after.startsWith("long:"), "Background epoch type changed");
        long old = Long.parseLong(before.substring(5)), now = Long.parseLong(after.substring(5));
        check(now >= old && now <= System.currentTimeMillis() + 60000, "Invalid background bookkeeping change");
    }
    private void equal(Object before, Object after, String message, boolean allowBackground) throws Exception {
        if (before instanceof JSONObject && after instanceof JSONObject) {
            JSONObject left = (JSONObject) before, right = (JSONObject) after;
            check(left.length() == right.length(), message + " (key count)");
            for (Iterator<String> keys = left.keys(); keys.hasNext();) {
                String key = keys.next(); check(right.has(key), message + " missing " + key);
                if (allowBackground && key.equals("last_background_epoch")) checkBackground(left.getString(key), right.getString(key));
                else equal(left.get(key), right.get(key), message + "/" + key, false);
            }
        } else if (before instanceof JSONArray && after instanceof JSONArray) {
            JSONArray left = (JSONArray) before, right = (JSONArray) after;
            check(left.length() == right.length(), message + " (row count)");
            for (int i = 0; i < left.length(); i++) equal(left.get(i), right.get(i), message + "/" + i, false);
        } else check(before.equals(after), message);
    }
    private File proof(String phase) { return new File(context.getNoBackupFilesDir(), "v11-upgrade-" + phase + ".json"); }
    private JSONObject read(File file) throws Exception { check(file.isFile(), "Missing fixture/proof " + file.getName()); return new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)); }
    private void write(File file, JSONObject value) throws Exception { Files.write(file.toPath(), value.toString(2).getBytes(StandardCharsets.UTF_8)); }
    private String sha256(byte[] bytes) throws Exception {
        StringBuilder out = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) out.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return out.toString();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
