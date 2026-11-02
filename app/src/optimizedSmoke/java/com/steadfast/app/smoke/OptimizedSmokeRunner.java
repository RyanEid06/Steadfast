package com.steadfast.app.smoke;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.app.Instrumentation.ActivityResult;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Uses only framework/Java APIs; shared app dependencies cannot mask optimized catalog failures. */
public class OptimizedSmokeRunner extends Instrumentation {
    private Activity currentActivity;
    private String mode;
    private String phase = "startup";
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        mode = arguments == null ? null : arguments.getString("mode");
        start();
    }
    @Override public void callActivityOnCreate(Activity activity, Bundle state) {
        super.callActivityOnCreate(activity, state);
        currentActivity = activity;
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            phase = "mode: " + (mode == null ? "entry" : mode);
            if ("widget".equals(mode)) {
                launch();
                awaitDashboard();
                android.appwidget.AppWidgetHost host = new android.appwidget.AppWidgetHost(getTargetContext(), 0x535446);
                int id = host.allocateAppWidgetId();
                try {
                    android.appwidget.AppWidgetManager manager = android.appwidget.AppWidgetManager.getInstance(getTargetContext());
                    android.content.ComponentName provider = new android.content.ComponentName(getTargetContext().getPackageName(), "com.steadfast.app.widget.SteadfastWidgetProvider");
                    check(manager.bindAppWidgetIdIfAllowed(id, provider), "Widget bind permission unavailable");
                    host.startListening();
                    java.util.concurrent.atomic.AtomicReference<android.appwidget.AppWidgetHostView> view = new java.util.concurrent.atomic.AtomicReference<>();
                    runOnMainSync(() -> view.set(host.createView(currentActivity, id, manager.getAppWidgetInfo(id))));
                    getTargetContext().sendBroadcast(new Intent("android.appwidget.action.APPWIDGET_UPDATE").setComponent(provider)
                        .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{id}));
                    long deadline = SystemClock.uptimeMillis() + 20000;
                    boolean published = false;
                    while (SystemClock.uptimeMillis() < deadline && !published) {
                        java.util.concurrent.atomic.AtomicBoolean found = new java.util.concurrent.atomic.AtomicBoolean(false);
                        runOnMainSync(() -> found.set(hasViewText(view.get(), "A stronger you. A closer walk.")));
                        published = found.get();
                        if (!published) SystemClock.sleep(100);
                    }
                    check(published, "Registered widget did not receive neutral RemoteViews");
                    result.putString("stream", "OK registered widget received neutral RemoteViews\n");
                } finally { host.stopListening(); host.deleteAppWidgetId(id); }
                finish(Activity.RESULT_OK, result);
                return;
            }
            if ("inspect".equals(mode)) {
                List<String> before = snapshot();
                String settingsDigestBefore = settingsFileDigest();
                launch();
                awaitDashboard();
                check(before.equals(snapshot()), "Upgrade launch changed persisted rows");
                String settingsDigestAfter = settingsFileDigest();
                check(settingsDigestBefore.equals(settingsDigestAfter), "Upgrade launch changed the DataStore settings file");
                result.putString("stream", "OK upgrade rows SHA256=" + sha256(before.toString().getBytes(StandardCharsets.UTF_8)) +
                    " settings SHA256=" + settingsDigestAfter + "\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            if ("catalogError".equals(mode)) {
                List<String> before = snapshot();
                launch();
                awaitText("Steadfast could not load");
                check(find(getUiAutomation().getRootInActiveWindow(), "Your data has not been changed. Retry loading the app's bundled content.") != null,
                    "Catalog error screen did not explain retry/data preservation");
                check(find(getUiAutomation().getRootInActiveWindow(), "I'm Struggling") == null,
                    "Protected content was shown after optimized catalog load failed");
                click("Retry");
                awaitText("Steadfast could not load");
                check(before.equals(snapshot()), "Failed catalog retry changed persisted rows");
                result.putString("stream", "OK optimized broken-catalog recovery: explicit retry screen, protected content hidden, rows unchanged\n");
                finish(Activity.RESULT_OK, result);
                return;
            }
            if ("verify".equals(mode)) {
                verifyBackupRoundTrips(result);
                return;
            }
            if ("icon".equals(mode)) {
                verifyOptimizedIcon(result);
                return;
            }
            if ("breathing".equals(mode)) {
                verifyBreathingLifecycle(result);
                return;
            }
            check(new JSONObject(asset("recovery_focuses.json")).getJSONArray("items").length() == 18, "Focus count");
            JSONArray books = new JSONObject(asset("bible_books.json")).getJSONArray("books");
            int chapters = 0;
            for (int i = 0; i < books.length(); i++) chapters += books.getJSONObject(i).getInt("chapters");
            check(books.length() == 66 && chapters == 1189, "Bible count");
            check(books.getJSONObject(0).getString("name").equals("Genesis"), "Genesis sentinel");
            check(books.getJSONObject(65).getString("name").equals("Revelation"), "Revelation sentinel");
            check(new JSONObject(asset("milestones.json")).getJSONArray("milestones").length() == 12, "Milestone count");
            check(new JSONObject(asset("verse_reference_catalog_starter.json")).getJSONObject("themes").length() == 8, "Verse themes");
            check(new JSONArray(asset("scale_comparisons.json")).length() == 21, "Comparison count");
            launch();
            click("Begin Your Walk");
            check(!clickableParent(awaitText("Confirm Focus & Continue")).isEnabled(), "Fresh focus was preselected");
            click("Pornography");
            recreate();
            click("Confirm Focus & Continue");
            click("Lock In Start Time");
            click("Save Scripture Preferences");
            click("Next: Product Tour");
            click("Enter Steadfast");
            awaitDashboard();
            try (SQLiteDatabase db = openDb(false)) {
                try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM streak_sessions", null)) {
                    check(c.moveToFirst() && c.getInt(0) == 1, "Duplicate initial session");
                }
                db.execSQL("INSERT INTO journal_entries (createdAtEpochMillis,updatedAtEpochMillis,body,context,streakSessionId) VALUES (1000,1000,'Synthetic acceptance note','URGE',NULL)");
                db.execSQL("INSERT INTO bible_chapter_completions (bookId,chapter) VALUES (1,1)");
            }
            List<String> before = snapshot();
            recreate();
            awaitDashboard();
            check(before.equals(snapshot()), "Recreation changed persisted synthetic rows");
            runOnMainSync(() -> currentActivity.finish());
            launch();
            awaitDashboard();
            check(before.equals(snapshot()), "Relaunch changed persisted synthetic rows");
            result.putString("stream", "OK (1 optimized entry smoke): catalogs, explicit focus, recreation, one session, journal/Bible preservation, relaunch\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            StringBuilder diagnostic = new StringBuilder("FAILED [phase=").append(phase).append("] ")
                .append(failure.getClass().getSimpleName());
            if (failure instanceof AssertionError && failure.getMessage() != null) diagnostic.append(": ").append(failure.getMessage());
            StackTraceElement[] stack = failure.getStackTrace();
            for (int i = 0, shown = 0; i < stack.length && shown < 8; i++) {
                if (!stack[i].getClassName().startsWith("com.steadfast.app.smoke.")) continue;
                diagnostic.append("\n  at ").append(stack[i].getClassName()).append('.')
                    .append(stack[i].getMethodName()).append('(').append(stack[i].getFileName())
                    .append(':').append(stack[i].getLineNumber()).append(')');
                shown++;
            }
            result.putString("stream", diagnostic.append('\n').toString());
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private void launch() {
        startActivitySync(new Intent().setClassName(getTargetContext().getPackageName(), "com.steadfast.app.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void awaitDashboard() {
        int acknowledged = 0;
        long deadline = SystemClock.uptimeMillis() + 60000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            if (find(root, "I'm Struggling") != null) return;
            if (find(root, "Milestone Reached!") != null) {
                check(acknowledged++ < 12, "More than 12 pending milestone dialogs");
                check(find(root, "Settings") == null, "Dashboard action remained accessible under milestone dialog");
                click("Continue Forward in Grace");
                continue;
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("Dashboard did not become accessible after acknowledging " + acknowledged + " milestone dialogs");
    }

    private void verifyBreathingLifecycle(Bundle result) throws Exception {
        List<String> rowsBefore = snapshot();
        launch();
        awaitDashboard();
        JSONObject settingsBefore = liveBackupSnapshot().getJSONObject("settings");
        click("Back");
        awaitDashboard();
        click("I'm Struggling");
        click("Take a Deep Breath (4-4-4-4)");
        BreathSample initial = awaitBreathingSample();
        int phasesSeen = 1 << initial.phase, secondsSeen = 1 << (initial.seconds - 1);
        boolean advanced = false;
        long advanceDeadline = SystemClock.uptimeMillis() + 20000;
        BreathSample advancing = initial;
        while ((phasesSeen != 15 || secondsSeen != 15) && SystemClock.uptimeMillis() < advanceDeadline) {
            SystemClock.sleep(50);
            BreathSample current = breathingSample();
            if (current != null) {
                advanced |= current.progress != initial.progress;
                advancing = current;
                phasesSeen |= 1 << current.phase;
                secondsSeen |= 1 << (current.seconds - 1);
            }
        }
        check(phasesSeen == 15 && secondsSeen == 15, "Breathing did not expose all four phase labels and countdown values 1 through 4");
        check(advanced, "Breathing countdown did not advance while foregrounded");

        Activity backgroundActivity = currentActivity;
        check(backgroundActivity != null, "Breathing activity was not tracked before Home");
        android.view.KeyEvent homeDown = new android.view.KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_HOME, 0);
        android.view.KeyEvent homeUp = new android.view.KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_HOME, 0);
        check(getUiAutomation().injectInputEvent(homeDown, true) && getUiAutomation().injectInputEvent(homeUp, true), "Cannot inject Home key");
        long focusDeadline = SystemClock.uptimeMillis() + 5000;
        boolean lostFocus = false;
        while (SystemClock.uptimeMillis() < focusDeadline) {
            AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            if (root == null || root.getPackageName() == null || !getTargetContext().getPackageName().contentEquals(root.getPackageName())) { lostFocus = true; break; }
            SystemClock.sleep(50);
        }
        check(lostFocus, "Home did not move the app window out of focus");
        SystemClock.sleep(6000);
        long foregroundAt = SystemClock.uptimeMillis();
        getTargetContext().startActivity(new Intent().setClassName(getTargetContext().getPackageName(), "com.steadfast.app.MainActivity")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        long returnDeadline = SystemClock.uptimeMillis() + 20000;
        boolean sameActivityFocused = false;
        while (SystemClock.uptimeMillis() < returnDeadline) {
            java.util.concurrent.atomic.AtomicBoolean focused = new java.util.concurrent.atomic.AtomicBoolean();
            runOnMainSync(() -> focused.set(currentActivity == backgroundActivity && currentActivity.getWindow().getDecorView().hasWindowFocus()));
            if (focused.get()) { sameActivityFocused = true; break; }
            SystemClock.sleep(50);
        }
        check(sameActivityFocused, "Returning from Home did not foreground the same breathing activity");
        waitForIdleSync();
        SystemClock.sleep(100);
        BreathSample resumed = awaitBreathingSample();
        long foregroundSeconds = Math.max(0, (SystemClock.uptimeMillis() - foregroundAt) / 1000);
        int resumedAdvance = forwardProgress(advancing.progress, resumed.progress);
        check(resumedAdvance <= foregroundSeconds + 2 && resumedAdvance < 6,
            "Breathing progressed beyond foreground transition allowance after 6 seconds in background");

        BreathSample beforeRecreate = awaitBreathPhaseAtLeastHold();
        Activity oldActivity = currentActivity;
        runOnMainSync(oldActivity::recreate);
        long recreateDeadline = SystemClock.uptimeMillis() + 20000;
        boolean recreatedFocused = false;
        while (SystemClock.uptimeMillis() < recreateDeadline) {
            java.util.concurrent.atomic.AtomicBoolean ready = new java.util.concurrent.atomic.AtomicBoolean();
            runOnMainSync(() -> ready.set(currentActivity != null && currentActivity != oldActivity && currentActivity.getWindow().getDecorView().hasWindowFocus()));
            if (ready.get()) { recreatedFocused = true; break; }
            SystemClock.sleep(50);
        }
        check(recreatedFocused, "Activity recreation did not replace and refocus MainActivity");
        waitForIdleSync();
        SystemClock.sleep(100);
        BreathSample recreated = awaitBreathingSample();
        check(recreated.phase >= 1 && forwardProgress(beforeRecreate.progress, recreated.progress) <= 2,
            "Activity recreation reset or unexpectedly advanced breathing progress");
        click("Close");
        click("I'm Grounded & Standing Firm Now");
        check(rowsBefore.equals(snapshot()), "Breathing flow changed persisted rows");
        JSONObject settingsAfter = liveBackupSnapshot().getJSONObject("settings");
        checkJsonEquals(settingsBefore, settingsAfter, "Breathing flow changed portable DataStore settings");
        click("Back");
        awaitDashboard();
        result.putString("stream", "OK breathing phases/countdown advanced foregrounded; Home paused progress across 6s background within foreground-transition allowance; Activity recreation retained Hold+ progress; SQL rows and portable settings unchanged; returned to dashboard\n");
        finish(Activity.RESULT_OK, result);
    }

    private static final String[] BREATH_PHASES = {"Breathe In Slowly", "Hold Gently", "Release & Surrender", "Rest in Peace"};
    private static final class BreathSample {
        final int phase, seconds, progress;
        BreathSample(int phase, int seconds) { this.phase = phase; this.seconds = seconds; this.progress = phase * 4 + (4 - seconds); }
    }
    private BreathSample awaitBreathingSample() {
        long deadline = SystemClock.uptimeMillis() + 20000;
        while (SystemClock.uptimeMillis() < deadline) {
            BreathSample sample = breathingSample();
            if (sample != null) return sample;
            SystemClock.sleep(50);
        }
        throw new AssertionError("Breathing phase/countdown did not become accessible");
    }
    private BreathSample breathingSample() {
        AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
        if (root == null || find(root, "CALMING BREATH") == null) return null;
        int phase = -1;
        for (int i = 0; i < BREATH_PHASES.length; i++) if (find(root, BREATH_PHASES[i]) != null) { check(phase == -1, "Multiple breathing phases are accessible"); phase = i; }
        int[] numbers = new int[4];
        countBreathingNumbers(root, numbers);
        int seconds = -1, matches = 0;
        for (int i = 0; i < numbers.length; i++) if (numbers[i] == 1) { seconds = i + 1; matches++; }
        if (phase == -1) return null;
        check(matches <= 1, "Breathing countdown is not a unique numeric value");
        if (matches != 1) return null;
        return new BreathSample(phase, seconds);
    }
    private void countBreathingNumbers(AccessibilityNodeInfo node, int[] counts) {
        if (node == null) return;
        CharSequence text = node.getText();
        if (text != null && text.length() == 1 && text.charAt(0) >= '1' && text.charAt(0) <= '4') counts[text.charAt(0) - '1']++;
        for (int i = 0; i < node.getChildCount(); i++) countBreathingNumbers(node.getChild(i), counts);
    }
    private BreathSample awaitBreathPhaseAtLeastHold() {
        long deadline = SystemClock.uptimeMillis() + 20000;
        while (SystemClock.uptimeMillis() < deadline) {
            BreathSample sample = breathingSample();
            if (sample != null && sample.phase >= 1) return sample;
            SystemClock.sleep(50);
        }
        throw new AssertionError("Breathing did not reach Hold within 20 seconds");
    }
    private int forwardProgress(int from, int to) { return (to - from + 16) % 16; }

    private void verifyOptimizedIcon(Bundle result) throws Exception {
        check(android.os.Build.VERSION.SDK_INT >= 33, "Monochrome adaptive icons require API 33+");
        Drawable loaded = getTargetContext().getPackageManager().getApplicationIcon(getTargetContext().getPackageName());
        check(loaded instanceof AdaptiveIconDrawable, "Optimized package icon is not adaptive");
        AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable) loaded;
        Drawable monochrome = adaptive.getMonochrome();
        check(monochrome != null, "Optimized adaptive icon has no monochrome layer");
        File output = new File(getTargetContext().getExternalFilesDir(null), "optimized-smoke-icon");
        if (!output.exists()) check(output.mkdirs(), "Cannot create icon evidence folder");
        final int size = 256;
        Bitmap normal = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        adaptive.setBounds(0, 0, size, size);
        adaptive.draw(new Canvas(normal));
        writePng(normal, new File(output, "adaptive.png"));

        monochrome.mutate().setTint(Color.WHITE);
        Bitmap circle = drawMaskedIcon(monochrome, size, true);
        Bitmap squircle = drawMaskedIcon(monochrome, size, false);
        check(Color.alpha(circle.getPixel(0, 0)) == 0 && Color.alpha(circle.getPixel(size / 2, size / 2)) > 0,
            "Monochrome circle mask alpha is invalid");
        check(Color.alpha(squircle.getPixel(0, 0)) == 0 && Color.alpha(squircle.getPixel(size / 2, size / 2)) > 0,
            "Monochrome squircle mask alpha is invalid");
        writePng(circle, new File(output, "monochrome-circle.png"));
        writePng(squircle, new File(output, "monochrome-squircle.png"));
        result.putString("stream", "OK optimized adaptive+monochrome icon; evidence=" + output.getAbsolutePath() + "\n");
        finish(Activity.RESULT_OK, result);
    }

    private Bitmap drawMaskedIcon(Drawable drawable, int size, boolean circle) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Path mask = new Path();
        if (circle) mask.addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW);
        else mask.addRoundRect(new RectF(0, 0, size, size), size * 0.24f, size * 0.24f, Path.Direction.CW);
        canvas.clipPath(mask);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return bitmap;
    }

    private void writePng(Bitmap bitmap, File path) throws Exception {
        try (FileOutputStream out = new FileOutputStream(path)) {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out), "Cannot encode icon evidence PNG");
        }
    }

    private void verifyBackupRoundTrips(Bundle result) throws Exception {
        File root = new File(getTargetContext().getCacheDir(), "optimized-backup-smoke");
        if (!root.exists()) check(root.mkdirs(), "Cannot create synthetic backup cache");
        File baseline = new File(root, "untouched.steadfast");
        File current = new File(root, "current.steadfast");
        File v1Short = new File(root, "legacy-short.steadfast");
        File v1Empty = new File(root, "legacy-empty.steadfast");
        File malformed = new File(root, "malformed.steadfast");
        File oversized = new File(root, "oversized.steadfast");
        List<String> untouchedRows = null;
        boolean syntheticMutation = false;
        try {
            launch();
            awaitDashboard();
            untouchedRows = snapshot();
            openSettings();
            verifyUnavailableAppLock();
            verifyProviderStreams();

            startExport(baseline);
            fillPasswordFields("optimized smoke passphrase", true);
            click("Export (.steadfast)");
            awaitFileStable(baseline, 51);
            JSONObject originalBaselineSnapshot = payloadSnapshot(untouchedRows, decryptBackup(baseline, "optimized smoke passphrase"));
            syntheticMutation = true;
            try (SQLiteDatabase db = openDb(false)) {
                db.execSQL("INSERT INTO journal_entries (createdAtEpochMillis,updatedAtEpochMillis,body,context,streakSessionId) VALUES (1000,1000,'Optimized synthetic acceptance note','URGE',NULL)");
                db.execSQL("INSERT OR IGNORE INTO bible_chapter_completions (bookId,chapter) VALUES (1,1)");
            }
            List<String> originalRows = snapshot();
            check(originalRows.stream().anyMatch(row -> row.contains("Optimized synthetic acceptance note")), "Synthetic journal row was not inserted");

            startExport(current);
            fillPasswordFields("optimized smoke passphrase", true);
            click("Export (.steadfast)");
            awaitFileStable(current, 51);
            JSONObject originalPayload = decryptBackup(current, "optimized smoke passphrase");
            JSONObject originalSnapshot = payloadSnapshot(originalRows, originalPayload);
            check(originalRows.equals(snapshot()), "V2 export changed live rows");

            // The failed resolver read must remain recoverable and leave every live row untouched.
            startImport(current);
            fillPasswordFields("incorrect passphrase", false);
            recreate();
            awaitText("Unlock Backup File");
            click("Inspect & Preview");
            awaitText("Could not unlock the backup. Check the passphrase and file.");
            check(originalRows.equals(snapshot()), "Wrong password changed live rows");
            cancelBackupDialog();

            try (FileOutputStream out = new FileOutputStream(malformed)) { out.write(new byte[]{1, 2, 3, 4, 5}); }
            expectImportFailure(malformed, "anything");
            try (FileOutputStream out = new FileOutputStream(oversized)) {
                byte[] block = new byte[64 * 1024];
                for (int i = 0; i < 513; i++) out.write(block);
            }
            expectImportFailure(oversized, "anything");
            File missing = new File(root, "missing-provider-file.steadfast");
            expectImportFailure(missing, "anything");
            expectProviderFailure(providerUri("provider-error-read.steadfast"));

            // Correct-password preview is read-only; applying the baseline also proves optimized replacement.
            startImport(current);
            fillPasswordFields("optimized smoke passphrase", false);
            click("Inspect & Preview");
            awaitText("Confirm Backup Restore");
            check(originalRows.equals(snapshot()), "Preview changed live rows");
            click("Replace All Data & Restore");
            awaitDialogClosed("Confirm Backup Restore");
            ensureSettings();
            check(originalRows.equals(snapshot()), "V2 round-trip changed row snapshot");
            check(jsonEquals(originalSnapshot, liveBackupSnapshot()), "V2 round-trip changed preferences, bookmarks, shown state, or backup rows");

            writeLegacyV1(v1Short, "short");
            writeLegacyV1(v1Empty, "");
            try {
                restoreLegacy(v1Short, "short");
                JSONObject legacyShort = decryptBackup(v1Short, "short");
                checkJsonEquals(legacyState(legacyShort), liveBackupSnapshot(), "V1 short-password restore did not replace rows/settings");
                restoreLegacy(v1Empty, "");
                JSONObject legacyEmpty = decryptBackup(v1Empty, "");
                checkJsonEquals(legacyState(legacyEmpty), liveBackupSnapshot(), "V1 empty-password restore did not replace rows/settings");
                verifyIncompleteFlagHistory(current, originalRows);
            } finally {
                // Keep the synthetic user's data recoverable even when a legacy assertion fails.
                restoreOriginal(baseline, untouchedRows);
            }
            check(untouchedRows.equals(snapshot()), "Final V2 restore failed to return pre-test rows");
            check(jsonEquals(originalBaselineSnapshot, liveBackupSnapshot()),
                "Final V2 restore failed to return pre-test preferences, bookmarks, and shown state");
            result.putString("stream", "OK optimized backup: V2 export/read-only preview/wrong password/restore; malformed, oversized and unavailable-file errors; V1 short+empty password replacements; activity recreation; original synthetic state restored\n");
            finish(Activity.RESULT_OK, result);
        } finally {
            if (syntheticMutation && untouchedRows != null && !untouchedRows.equals(snapshot()) && baseline.isFile()) {
                restoreOriginal(baseline, untouchedRows);
            }
            for (File file : new File[]{baseline, current, v1Short, v1Empty, malformed, oversized}) if (file.exists()) file.delete();
            root.delete();
        }
    }

    private void openSettings() {
        click("Settings");
        awaitText("Export Encrypted Backup");
    }

    /** On an unenrolled emulator, Settings must reject enabling the lock and survive recreation unlocked. */
    private void verifyUnavailableAppLock() {
        AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
        String[] unavailable = {
            "Set up a screen lock or biometrics in Android Settings to enable app lock.",
            "Device authentication is temporarily unavailable. Try again later.",
            "This device has no supported authentication hardware.",
            "Device authentication is not supported on this device."
        };
        boolean cannotAuthenticate = false;
        for (String guidance : unavailable) if (find(root, guidance) != null) cannotAuthenticate = true;
        if (!cannotAuthenticate) return;

        AccessibilityNodeInfo lock = find(root, "Biometric / App Lock");
        check(lock != null && lock.isCheckable() && !lock.isChecked(), "Unavailable device unexpectedly shows app lock enabled");
        click("Biometric / App Lock");
        SystemClock.sleep(500);
        lock = find(getUiAutomation().getRootInActiveWindow(), "Biometric / App Lock");
        check(lock != null && !lock.isChecked(), "Unavailable authentication enabled app lock");
        recreate();
        ensureSettings();
        lock = find(getUiAutomation().getRootInActiveWindow(), "Biometric / App Lock");
        check(lock != null && !lock.isChecked(), "Unavailable app-lock attempt changed state after recreation");
    }

    private void ensureSettings() {
        AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
        if (find(root, "Import & Restore Backup") == null && find(root, "Export Encrypted Backup") == null) {
            awaitDashboard();
            openSettings();
        }
    }

    private void verifyIncompleteFlagHistory(File current, List<String> expectedRows) throws Exception {
        List<String> beforePreview = snapshot();
        JSONObject payload = decryptBackup(current, "optimized smoke passphrase");
        payload.getJSONObject("settings").put("onboardingComplete", false);
        File incomplete = new File(current.getParentFile(), "valid-history-incomplete-flag.steadfast");
        try {
            writeV2(incomplete, payload, "optimized smoke passphrase");
            startImport(incomplete);
            fillPasswordFields("optimized smoke passphrase", false);
            click("Inspect & Preview");
            awaitText("Confirm Backup Restore");
            check(beforePreview.equals(snapshot()), "Incomplete-flag preview changed live history");
            click("Replace All Data & Restore");
            awaitDialogClosed("Confirm Backup Restore");
            runOnMainSync(() -> currentActivity.recreate());
            SystemClock.sleep(1000);
            awaitDashboard();
            check(expectedRows.equals(snapshot()), "Startup reconciliation changed existing history");
            JSONObject reconciled = liveBackupSnapshot();
            check(reconciled.getJSONObject("settings").optBoolean("onboardingComplete"), "Valid history did not reconcile the incomplete onboarding flag");
            click("Back");
            awaitDashboard();
            runOnMainSync(() -> currentActivity.recreate());
            SystemClock.sleep(1000);
            awaitDashboard();
            check(expectedRows.equals(snapshot()), "Activity recreation duplicated or lost valid history");
            runOnMainSync(() -> currentActivity.finish());
            launch();
            awaitDashboard();
            check(expectedRows.equals(snapshot()), "Relaunch duplicated or lost valid history");
        } finally { if (incomplete.exists()) incomplete.delete(); }
    }

    private void startExport(File file) throws Exception {
        ActivityMonitor monitor = fileMonitor(Intent.ACTION_CREATE_DOCUMENT, file);
        addMonitor(monitor);
        click("Export Encrypted Backup");
        awaitText("Encrypt Export File");
        check(monitor.getHits() > 0, "CreateDocument picker was not intercepted");
        removeMonitor(monitor);
    }

    private void startImport(File file) throws Exception {
        ActivityMonitor monitor = fileMonitor(Intent.ACTION_OPEN_DOCUMENT, file);
        addMonitor(monitor);
        click("Import & Restore Backup");
        awaitText("Unlock Backup File");
        check(monitor.getHits() > 0, "OpenDocument picker was not intercepted");
        removeMonitor(monitor);
    }

    private ActivityMonitor fileMonitor(String action, File file) throws Exception {
        return fileMonitor(action, Uri.fromFile(file));
    }

    private ActivityMonitor fileMonitor(String action, Uri uri) throws Exception {
        Intent result = new Intent().setData(uri).addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        IntentFilter filter = new IntentFilter(action);
        filter.addCategory(Intent.CATEGORY_OPENABLE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addDataType("*/*");
        return new ActivityMonitor(filter, new ActivityResult(Activity.RESULT_OK, result), true);
    }

    private Uri providerUri(String name) {
        return Uri.parse("content://com.steadfast.app.smoke.documents/" + name);
    }

    private void verifyProviderStreams() throws Exception {
        Uri uri = providerUri("provider-roundtrip.steadfast");
        byte[] expected = "synthetic provider stream".getBytes(StandardCharsets.UTF_8);
        try {
            try (java.io.OutputStream out = getTargetContext().getContentResolver().openOutputStream(uri, "w")) {
                check(out != null, "Synthetic provider did not open a write stream");
                out.write(expected);
            }
            try (InputStream in = getTargetContext().getContentResolver().openInputStream(uri)) {
                check(in != null && Arrays.equals(expected, in.readAllBytes()), "Synthetic provider read/write round trip failed");
            }
        } finally { getTargetContext().getContentResolver().delete(uri, null, null); }
    }

    private void expectProviderFailure(Uri uri) throws Exception {
        List<String> before = snapshot();
        ActivityMonitor monitor = fileMonitor(Intent.ACTION_OPEN_DOCUMENT, uri);
        addMonitor(monitor);
        click("Import & Restore Backup");
        awaitText("Unlock Backup File");
        check(monitor.getHits() > 0, "OpenDocument picker was not intercepted for provider URI");
        removeMonitor(monitor);
        fillPasswordFields("anything", false);
        click("Inspect & Preview");
        awaitText("Could not unlock the backup. Check the passphrase and file.");
        check(before.equals(snapshot()), "Provider read failure changed live rows");
        cancelBackupDialog();
    }

    private void fillPasswordFields(String passphrase, boolean confirmation) {
        phase = confirmation ? "fill export password fields" : "fill import password field";
        setEditableText(0, passphrase);
        if (confirmation) setEditableText(1, passphrase);
    }

    private void setEditableText(int index, String value) {
        List<AccessibilityNodeInfo> fields = editableNodes(getUiAutomation().getRootInActiveWindow(), new ArrayList<>());
        check(fields.size() > index, "Password field not found");
        setText(fields.get(index), value);
    }

    private void setText(AccessibilityNodeInfo node, String value) {
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value);
        check(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args), "Cannot enter synthetic passphrase");
    }

    private List<AccessibilityNodeInfo> editableNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> found) {
        if (node == null) return found;
        if (node.isEditable()) found.add(node);
        for (int i = 0; i < node.getChildCount(); i++) editableNodes(node.getChild(i), found);
        return found;
    }

    private void expectImportFailure(File file, String password) throws Exception {
        List<String> before = snapshot();
        startImport(file);
        fillPasswordFields(password, false);
        click("Inspect & Preview");
        awaitText("Could not unlock the backup. Check the passphrase and file.");
        check(before.equals(snapshot()), "Invalid/unavailable backup changed live rows");
        cancelBackupDialog();
    }

    private void cancelBackupDialog() {
        click("Cancel");
        waitForIdleSync();
    }

    private void awaitDialogClosed(String title) {
        long deadline = SystemClock.uptimeMillis() + 20000;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            if (find(root, title) == null && find(root, "Restoring Data…") == null) return;
            SystemClock.sleep(100);
        }
        throw new AssertionError("Restore dialog did not close");
    }

    private void restoreLegacy(File file, String password) throws Exception {
        startImport(file);
        fillPasswordFields(password, false);
        click("Inspect & Preview");
        awaitText("Confirm Backup Restore");
        click("Replace All Data & Restore");
        awaitDialogClosed("Confirm Backup Restore");
        ensureSettings();
        assertLegacySession();
    }

    private void assertLegacySession() {
        try (SQLiteDatabase db = openDb(true);
             Cursor cursor = db.rawQuery("SELECT id,recoveryFocusId,startEpochMillis,startZoneId,endEpochMillis,createdAtEpochMillis FROM streak_sessions", null)) {
            check(cursor.getCount() == 1 && cursor.moveToFirst(), "Legacy restore did not replace sessions with its one synthetic session");
            check(cursor.getLong(0) == 9001 && "alcohol".equals(cursor.getString(1)) && "UTC".equals(cursor.getString(3))
                && cursor.isNull(4) && cursor.getLong(2) == cursor.getLong(5), "Legacy active session fields changed");
        }
        try (SQLiteDatabase db = openDb(true)) {
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM journal_entries", null)) { check(c.moveToFirst() && c.getInt(0) == 0, "Legacy restore retained journal rows"); }
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM bible_chapter_completions", null)) { check(c.moveToFirst() && c.getInt(0) == 0, "Legacy restore retained Bible rows"); }
        }
    }

    private void restoreOriginal(File file, List<String> expectedRows) throws Exception {
        if (find(getUiAutomation().getRootInActiveWindow(), "Cancel") != null) click("Cancel");
        ensureSettings();
        startImport(file);
        fillPasswordFields("optimized smoke passphrase", false);
        click("Inspect & Preview");
        awaitText("Confirm Backup Restore");
        click("Replace All Data & Restore");
        awaitDialogClosed("Confirm Backup Restore");
        ensureSettings();
        long deadline = SystemClock.uptimeMillis() + 20000;
        while (SystemClock.uptimeMillis() < deadline && !expectedRows.equals(snapshot())) SystemClock.sleep(100);
        check(expectedRows.equals(snapshot()), "Original state restore did not finish");
    }

    private JSONObject liveBackupSnapshot() throws Exception {
        File path = new File(getTargetContext().getCacheDir(), "optimized-backup-smoke/live-state.steadfast");
        ensureSettings();
        ActivityMonitor monitor = fileMonitor(Intent.ACTION_CREATE_DOCUMENT, path);
        addMonitor(monitor);
        click("Export Encrypted Backup");
        awaitText("Encrypt Export File");
        check(monitor.getHits() > 0, "Live snapshot picker was not intercepted");
        removeMonitor(monitor);
        fillPasswordFields("optimized smoke passphrase", true);
        click("Export (.steadfast)");
        awaitFileStable(path, 51);
        JSONObject value = decryptBackup(path, "optimized smoke passphrase");
        path.delete();
        // Settings remains active after Export.
        return payloadSnapshot(snapshot(), value);
    }

    private JSONObject legacyState(JSONObject payload) {
        check(payload.optInt("schemaVersion") == 1, "Expected V1 backup payload");
        JSONObject settings = payload.optJSONObject("settings");
        check(settings != null, "Legacy payload missing settings");
        JSONObject expectedSettings = new JSONObject();
        try {
            expectedSettings.put("themeMode", "DARK");
            expectedSettings.put("recoveryFocusId", "alcohol");
            expectedSettings.put("readingGoalMinimum", 3);
            expectedSettings.put("oldTestamentCurrent", new JSONObject().put("bookId", 1).put("chapter", 2));
            expectedSettings.put("newTestamentCurrent", new JSONObject().put("bookId", 43).put("chapter", 1));
            expectedSettings.put("appLockEnabled", false);
            expectedSettings.put("shownMilestones", "d1,d7");
            expectedSettings.put("onboardingComplete", true);
            JSONObject expectedPayload = new JSONObject().put("settings", expectedSettings)
                .put("streakSessions", payload.getJSONArray("streakSessions"))
                .put("journalEntries", payload.getJSONArray("journalEntries"))
                .put("bibleCompletedChapters", payload.getJSONArray("bibleCompletedChapters"));
            return payloadSnapshot(snapshot(), expectedPayload);
        } catch (Exception error) { throw new AssertionError(error); }
    }

    private JSONObject payloadSnapshot(List<String> rows, JSONObject payload) throws Exception {
        return new JSONObject().put("rows", new JSONArray(rows))
            .put("settings", payload.getJSONObject("settings"))
            .put("streakSessions", payload.getJSONArray("streakSessions"))
            .put("journalEntries", payload.getJSONArray("journalEntries"))
            .put("bibleCompletedChapters", payload.getJSONArray("bibleCompletedChapters"));
    }

    private boolean jsonEquals(Object left, Object right) {
        if (left instanceof JSONObject && right instanceof JSONObject) {
            JSONObject a = (JSONObject) left, b = (JSONObject) right;
            if (a.length() != b.length()) return false;
            java.util.Iterator<String> keys = a.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!b.has(key)) return false;
                Object av = a.opt(key), bv = b.opt(key);
                if ("shownMilestones".equals(key) && av instanceof String && bv instanceof String) {
                    if (!sameMilestoneSet((String) av, (String) bv)) return false;
                } else if (!jsonEquals(av, bv)) return false;
            }
            return true;
        }
        if (left instanceof JSONArray && right instanceof JSONArray) {
            JSONArray a = (JSONArray) left, b = (JSONArray) right;
            if (a.length() != b.length()) return false;
            for (int i = 0; i < a.length(); i++) if (!jsonEquals(a.opt(i), b.opt(i))) return false;
            return true;
        }
        return left == null ? right == null : left.equals(right);
    }

    private void checkJsonEquals(JSONObject expected, JSONObject actual, String message) {
        String mismatch = jsonMismatchPath(expected, actual, "$", null);
        check(mismatch == null, message + (mismatch == null ? "" : " at " + mismatch));
    }

    private String jsonMismatchPath(Object expected, Object actual, String path, String key) {
        if (key != null && "shownMilestones".equals(key) && expected instanceof String && actual instanceof String) {
            return sameMilestoneSet((String) expected, (String) actual) ? null : path;
        }
        if (expected instanceof JSONObject && actual instanceof JSONObject) {
            JSONObject a = (JSONObject) expected, b = (JSONObject) actual;
            java.util.Iterator<String> keys = a.keys();
            while (keys.hasNext()) {
                String child = keys.next();
                String childPath = jsonMismatchPath(a.opt(child), b.opt(child), path + "." + child, child);
                if (childPath != null) return childPath;
            }
            keys = b.keys();
            while (keys.hasNext()) {
                String child = keys.next();
                if (!a.has(child)) return path + "." + child;
            }
            return null;
        }
        if (expected instanceof JSONArray && actual instanceof JSONArray) {
            JSONArray a = (JSONArray) expected, b = (JSONArray) actual;
            if (a.length() != b.length()) return path + ".length";
            for (int i = 0; i < a.length(); i++) {
                String childPath = jsonMismatchPath(a.opt(i), b.opt(i), path + "[" + i + "]", null);
                if (childPath != null) return childPath;
            }
            return null;
        }
        return jsonEquals(expected, actual) ? null : path;
    }

    private boolean sameMilestoneSet(String left, String right) {
        String[] a = left.isEmpty() ? new String[0] : left.split(",");
        String[] b = right.isEmpty() ? new String[0] : right.split(",");
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    private JSONObject decryptBackup(File file, String passphrase) throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        check(bytes.length >= 51, "Backup file is too short");
        String header = new String(bytes, 0, 7, StandardCharsets.UTF_8);
        int iterations;
        if ("STDF_V2".equals(header)) iterations = 210000;
        else if ("STDF_V1".equals(header)) iterations = 65536;
        else throw new AssertionError("Unsupported backup header");
        byte[] salt = Arrays.copyOfRange(bytes, 7, 23);
        byte[] iv = Arrays.copyOfRange(bytes, 23, 35);
        byte[] key = deriveBackupKey(passphrase, salt, iterations);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] plaintext = cipher.doFinal(Arrays.copyOfRange(bytes, 35, bytes.length));
        return new JSONObject(new String(plaintext, StandardCharsets.UTF_8));
    }

    private void writeV2(File file, JSONObject payload, String passphrase) throws Exception {
        byte[] salt = new byte[16], iv = new byte[12];
        new SecureRandom().nextBytes(salt);
        new SecureRandom().nextBytes(iv);
        PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, 210000, 256);
        byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(payload.toString().getBytes(StandardCharsets.UTF_8));
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("STDF_V2".getBytes(StandardCharsets.UTF_8));
            out.write(salt);
            out.write(iv);
            out.write(encrypted);
        }
    }

    private void writeLegacyV1(File file, String password) throws Exception {
        phase = password.isEmpty() ? "write V1 empty-password fixture" : "write V1 short-password fixture";
        long sessionStart = System.currentTimeMillis() - 3600000;
        JSONArray sessions = new JSONArray().put(new JSONObject().put("id", 9001).put("recoveryFocusId", "alcohol")
            .put("startEpochMillis", sessionStart).put("startZoneId", "UTC").put("endEpochMillis", JSONObject.NULL)
            .put("createdAtEpochMillis", sessionStart));
        JSONObject settings = new JSONObject().put("themeMode", "DARK").put("recoveryFocusId", "alcohol")
            .put("readingGoalMinimum", 3).put("oldTestamentCurrent", new JSONObject().put("bookId", 1).put("chapter", 2))
            .put("newTestamentCurrent", new JSONObject().put("bookId", 43).put("chapter", 1))
            .put("appLockEnabled", false).put("shownMilestones", "d1,d7").put("onboardingComplete", true);
        byte[] plaintext = new JSONObject().put("schemaVersion", 1).put("exportedAtEpochMillis", System.currentTimeMillis())
            .put("appVersion", "1.0.0").put("settings", settings).put("streakSessions", sessions)
            .put("journalEntries", new JSONArray()).put("bibleCompletedChapters", new JSONArray()).toString().getBytes(StandardCharsets.UTF_8);
        byte[] salt = new byte[16], iv = new byte[12];
        new SecureRandom().nextBytes(salt);
        new SecureRandom().nextBytes(iv);
        byte[] key = deriveBackupKey(password, salt, 65536);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("STDF_V1".getBytes(StandardCharsets.UTF_8)); out.write(salt); out.write(iv); out.write(ciphertext);
        }
    }

    private byte[] deriveBackupKey(String passphrase, byte[] salt, int iterations) throws Exception {
        if (!passphrase.isEmpty()) {
            PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        }

        // Android's PBEKeySpec provider rejects empty passwords; PBKDF2 treats the empty HMAC key as zero-padded.
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(new byte[]{0}, "HmacSHA256"));
        byte[] block = Arrays.copyOf(salt, salt.length + 4);
        block[block.length - 1] = 1;
        byte[] u = mac.doFinal(block);
        byte[] derived = u.clone();
        for (int round = 1; round < iterations; round++) {
            u = mac.doFinal(u);
            for (int i = 0; i < derived.length; i++) derived[i] ^= u[i];
        }
        return derived;
    }

    private void awaitFileStable(File file, long minimumBytes) {
        long deadline = SystemClock.uptimeMillis() + 20000;
        long priorLength = -1;
        long stableSince = 0;
        while (SystemClock.uptimeMillis() < deadline) {
            long size = file.isFile() ? file.length() : -1;
            if (size >= minimumBytes && size == priorLength) {
                if (stableSince == 0) stableSince = SystemClock.uptimeMillis();
                if (SystemClock.uptimeMillis() - stableSince >= 1000) break;
            } else stableSince = 0;
            priorLength = size;
            SystemClock.sleep(100);
        }
        waitForIdleSync();
        SystemClock.sleep(500);
        check(file.isFile() && file.length() >= minimumBytes, "Backup export did not create a file");
    }

    private boolean hasViewText(android.view.View view, String expected) {
        if (view instanceof android.widget.TextView && expected.contentEquals(((android.widget.TextView) view).getText())) return true;
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (hasViewText(group.getChildAt(i), expected)) return true;
        }
        return false;
    }
    private void recreate() {
        runOnMainSync(() -> currentActivity.recreate());
        SystemClock.sleep(1000);
        waitForIdleSync();
    }
    private String asset(String name) throws Exception {
        try (InputStream in = getTargetContext().getAssets().open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096];
            int count;
            while ((count = in.read(bytes)) != -1) out.write(bytes, 0, count);
            return out.toString("UTF-8");
        }
    }
    private void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private AccessibilityNodeInfo clickableParent(AccessibilityNodeInfo node) {
        while (!node.isClickable() && node.getParent() != null) node = node.getParent();
        return node;
    }
    private void click(String text) {
        phase = "click: " + text;
        AccessibilityNodeInfo node = awaitClickable(text);
        check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK), "Cannot click " + text);
        waitForIdleSync();
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        if ((node.getText() != null && node.getText().toString().equalsIgnoreCase(text)) ||
            (node.getContentDescription() != null && node.getContentDescription().toString().equalsIgnoreCase(text))) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = find(node.getChild(i), text);
            if (found != null) return found;
        }
        return null;
    }
    private AccessibilityNodeInfo awaitText(String text) {
        long deadline = SystemClock.uptimeMillis() + 20000;
        long lastScroll = 0;
        int scrollAttempts = 0;
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            AccessibilityNodeInfo node = find(root, text);
            if (node != null) return node;
            if (SystemClock.uptimeMillis() - lastScroll > 300) {
                AccessibilityNodeInfo scrollable = findScrollable(root);
                if (scrollable != null) {
                    int direction = ((scrollAttempts / 10) % 2 == 0)
                        ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
                    scrollable.performAction(direction);
                    scrollAttempts++;
                }
                lastScroll = SystemClock.uptimeMillis();
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("Visible control not found: " + text);
    }
    private AccessibilityNodeInfo awaitClickable(String text) {
        long deadline = SystemClock.uptimeMillis() + 20000;
        long lastScroll = 0;
        int scrollAttempts = 0;
        String lastState = "missing";
        while (SystemClock.uptimeMillis() < deadline) {
            AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
            AccessibilityNodeInfo node = find(root, text);
            if (node != null) {
                AccessibilityNodeInfo target = clickableParent(node);
                lastState = "clickable=" + target.isClickable() + ", enabled=" + target.isEnabled();
                if (target.isClickable() && target.isEnabled()) return target;
            } else if (SystemClock.uptimeMillis() - lastScroll > 300) {
                AccessibilityNodeInfo scrollable = findScrollable(root);
                if (scrollable != null) {
                    int direction = ((scrollAttempts / 10) % 2 == 0)
                        ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
                    scrollable.performAction(direction);
                    scrollAttempts++;
                }
                lastScroll = SystemClock.uptimeMillis();
            }
            SystemClock.sleep(100);
        }
        throw new AssertionError("Control did not become clickable and enabled: " + text + " (" + lastState + ")");
    }
    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findScrollable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }
    private SQLiteDatabase openDb(boolean readOnly) {
        return SQLiteDatabase.openDatabase(getTargetContext().getDatabasePath("steadfast.db").getPath(), null,
            readOnly ? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE);
    }
    private List<String> snapshot() {
        List<String> result = new ArrayList<>();
        try (SQLiteDatabase db = openDb(true)) {
            for (String table : new String[]{"streak_sessions", "journal_entries", "bible_chapter_completions"}) {
                try (Cursor c = db.rawQuery("SELECT * FROM " + table + " ORDER BY 1", null)) {
                    while (c.moveToNext()) {
                        StringBuilder row = new StringBuilder(table);
                        for (int i = 0; i < c.getColumnCount(); i++) row.append('|').append(c.getString(i));
                        result.add(row.toString());
                    }
                }
            }
        }
        return result;
    }

    private String settingsFileDigest() throws Exception {
        File file = new File(getTargetContext().getFilesDir(), "datastore/steadfast_settings.preferences_pb");
        check(file.isFile(), "DataStore settings file is missing before upgrade inspection");
        return sha256(java.nio.file.Files.readAllBytes(file.toPath()));
    }

    private String sha256(byte[] bytes) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return hex.toString();
    }
}
