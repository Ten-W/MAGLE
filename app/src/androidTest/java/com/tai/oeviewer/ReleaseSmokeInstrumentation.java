package com.tai.oeviewer;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;

/** Read-only check of the actual optimized APK; no account or cloud mutations. */
public class ReleaseSmokeInstrumentation extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args) { super.onCreate(args); arguments = args == null ? new Bundle() : args; start(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static String hash(File file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
        StringBuilder output = new StringBuilder();
        for (byte value : digest) output.append(String.format("%02x", value));
        return output.toString();
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Context context = getTargetContext();
            if ("true".equals(arguments.getString("diagnoseUpdates"))) {
                android.content.SharedPreferences prefs = context.getSharedPreferences("app-download", Context.MODE_PRIVATE);
                long id = prefs.getLong("id", -1);
                result.putLong("downloadId", id);
                sendStatus(1, result);
                android.app.DownloadManager manager = context.getSystemService(android.app.DownloadManager.class);
                try (android.database.Cursor cursor = manager.query(new android.app.DownloadManager.Query().setFilterById(id))) {
                    if (cursor.moveToFirst()) {
                        for (String column : new String[]{android.app.DownloadManager.COLUMN_STATUS, android.app.DownloadManager.COLUMN_REASON,
                                android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR, android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES}) {
                            result.putLong(column, cursor.getLong(cursor.getColumnIndexOrThrow(column)));
                        }
                    }
                }
                sendStatus(2, result);
                try (android.database.Cursor cursor = context.getContentResolver().query(android.net.Uri.parse("content://downloads/my_downloads/" + id),
                        new String[]{"status", "errorMsg"}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        result.putInt("rawStatus", cursor.getInt(0));
                        result.putString("downloadError", cursor.getString(1));
                    }
                } catch (Exception error) { result.putString("rawQuery", error.getClass().getSimpleName()); }
                String[] urls = {"https://api.github.com/repos/Ten-W/MAGLE/releases?per_page=100", prefs.getString("url", "")};
                for (int i = 0; i < urls.length; i++) {
                    if (urls[i].isEmpty()) continue;
                    java.net.HttpURLConnection connection = null;
                    try {
                        connection = (java.net.HttpURLConnection) new java.net.URL(urls[i]).openConnection();
                        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
                        connection.setInstanceFollowRedirects(false);
                        connection.setRequestMethod(i == 0 ? "GET" : "HEAD");
                        connection.setRequestProperty("User-Agent", "MAGLE-update-diagnostic");
                        int code = connection.getResponseCode();
                        result.putInt(i == 0 ? "apiHttp" : "apkHttp", code);
                        if (i == 1 && code >= 300 && code < 400) {
                            java.net.URL destination = new java.net.URL(connection.getHeaderField("Location"));
                            if ("https".equals(destination.getProtocol()) && "release-assets.githubusercontent.com".equals(destination.getHost())) {
                                java.net.HttpURLConnection asset = (java.net.HttpURLConnection) destination.openConnection();
                                try {
                                    asset.setConnectTimeout(10000); asset.setReadTimeout(10000); asset.setRequestMethod("HEAD");
                                    result.putInt("assetHttp", asset.getResponseCode());
                                    result.putLong("assetBytes", asset.getContentLengthLong());
                                } finally { asset.disconnect(); }
                            }
                        }
                        if (i == 0) {
                            result.putString("rateRemaining", connection.getHeaderField("X-RateLimit-Remaining"));
                            if (code != 200 && connection.getErrorStream() != null) {
                                byte[] buffer = new byte[2048];
                                try (java.io.InputStream input = connection.getErrorStream()) {
                                    int count = input.read(buffer);
                                    if (count > 0) {
                                        String message = new JSONObject(new String(buffer, 0, count, StandardCharsets.UTF_8)).optString("message");
                                        result.putString("apiError", message.startsWith("API rate limit exceeded") ? "API rate limit exceeded" : message);
                                    }
                                }
                            }
                        }
                    } catch (Exception error) { result.putString(i == 0 ? "apiError" : "apkError", error.getClass().getSimpleName()); }
                    finally { if (connection != null) connection.disconnect(); }
                    sendStatus(3 + i, result);
                }
                finish(Activity.RESULT_OK, result); return;
            }
            require((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0, "Expected non-debuggable package");
            require("0.8.73".equals(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName), "Wrong version");
            if ("true".equals(arguments.getString("testAppDownload"))) {
                startActivitySync(new Intent().setClassName(context.getPackageName(), context.getPackageName() + ".MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                waitForIdleSync();
                ClassLoader loader = context.getClassLoader();
                // Use the live optimized downloader, in an isolated cache directory. Never install the test APK.
                Class<?> serviceClass = loader.loadClass("com.tai.oeviewer.AppDownloadService");
                Object service = serviceClass.getConstructor().newInstance();
                java.lang.reflect.Field clientField = serviceClass.getDeclaredField("client");
                clientField.setAccessible(true);
                Object client = clientField.get(service);
                Class<?> releaseClass = loader.loadClass("com.tai.oeviewer.AppRelease");
                java.lang.reflect.Constructor<?> constructor = releaseClass.getDeclaredConstructor(String.class, String.class, String.class, String.class, long.class, String.class);
                constructor.setAccessible(true);
                Object release = constructor.newInstance("v0.8.61", "", "https://github.com/Ten-W/MAGLE/releases/tag/v0.8.61",
                    "https://github.com/Ten-W/MAGLE/releases/download/v0.8.61/MAGLE-v0.8.61-release.apk", 7391350L,
                    "sha256:fa47e78bee0a24e829f891ed4e203893bcc08486ebe74b17859564b9119f96df");
                Class<?> helpers = loader.loadClass("com.tai.oeviewer.TokenStore");
                java.lang.reflect.Method download = null;
                for (java.lang.reflect.Method method : helpers.getDeclaredMethods()) if (method.getName().equals("downloadAppFile")) download = method;
                require(download != null, "Optimized download helper missing");
                download.setAccessible(true);
                File directory = new File(context.getCacheDir(), "update-transfer-smoke");
                directory.mkdirs();
                File target = new File(directory, "test.apk");
                File partial = new File(target.getPath() + ".part");
                Thread progress = new Thread(() -> {
                    try {
                        for (int i = 0; i < 24; i++) {
                            Bundle status = new Bundle(); status.putLong("partialBytes", partial.length()); status.putLong("completeBytes", target.length());
                            sendStatus(12, status); Thread.sleep(5000);
                        }
                        Object dispatcher = client.getClass().getMethod("dispatcher").invoke(client);
                        dispatcher.getClass().getMethod("cancelAll").invoke(dispatcher);
                    } catch (Exception ignored) { }
                });
                progress.start();
                try {
                    Class<?>[] types = download.getParameterTypes();
                    Object[] values = new Object[types.length];
                    for (int i = 0; i < types.length; i++) values[i] = types[i] == File.class ? target : types[i] == releaseClass ? release : client;
                    sendStatus(10, result);
                    download.invoke(null, values);
                    require(target.length() == 7391350L, "Incomplete APK");
                    String digest = hash(target);
                    require(digest.equals("fa47e78bee0a24e829f891ed4e203893bcc08486ebe74b17859564b9119f96df"), "Wrong APK digest");
                    // Prepare a genuine prefix then exercise HTTP Range using exactly the same helper.
                    try (java.io.InputStream input = new java.io.FileInputStream(target); java.io.OutputStream output = new java.io.FileOutputStream(partial)) {
                        byte[] buffer = new byte[65536]; int remaining = 1048576;
                        while (remaining > 0) { int count = input.read(buffer, 0, Math.min(buffer.length, remaining)); require(count > 0, "No prefix"); output.write(buffer, 0, count); remaining -= count; }
                    }
                    require(target.delete(), "Could not prepare resume test");
                    sendStatus(11, result);
                    download.invoke(null, values);
                    require(hash(target).equals(digest) && !partial.exists(), "Resume failed");
                    result.putString("result", "PASS: application-process APK download, SHA-256 and 1 MiB resume; no installation");
                    finish(Activity.RESULT_OK, result); return;
                } finally { progress.interrupt(); target.delete(); partial.delete(); directory.delete(); }
            }
            File[] files = context.getFilesDir().listFiles((dir, name) -> name.startsWith("eagle-index-") && name.endsWith(".json"));
            ArrayList<String> hashes = new ArrayList<>();
            for (File file : files) {
                hashes.add(hash(file));
                new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getJSONObject("items");
            }
            Collections.sort(hashes);
            String expected = arguments.getString("expectedIndexes");
            require(expected == null || expected.equals(String.join(",", hashes)), "Index changed during update");
            expected = arguments.getString("expectedLibraries");
            require(expected == null || expected.equals(hash(new File(context.getApplicationInfo().dataDir, "shared_prefs/library.xml"))), "Library list changed");
            ClassLoader loader = context.getClassLoader();
            require(loader.loadClass("com.ctc.wstx.stax.WstxInputFactory").getConstructor().newInstance() != null, "StAX provider missing");
            Class<?> svg = loader.loadClass("com.tai.oeviewer.SvgPreview");
            byte[] data = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"4\" height=\"4\"><rect width=\"4\" height=\"4\"/></svg>".getBytes(StandardCharsets.UTF_8);
            // R8 may staticize/reorder this live helper; test its residual signature, not an unused SDK API.
            java.lang.reflect.Method render = null;
            for (java.lang.reflect.Method method : svg.getDeclaredMethods()) if (method.getName().equals("render")) render = method;
            require(render != null, "SVG preview helper missing");
            render.setAccessible(true);
            Class<?>[] types = render.getParameterTypes();
            Object[] values = new Object[types.length];
            for (int i = 0; i < types.length; i++) values[i] = types[i] == byte[].class ? data : 64;
            Object receiver = java.lang.reflect.Modifier.isStatic(render.getModifiers()) ? null : svg.getField("INSTANCE").get(null);
            android.graphics.Bitmap bitmap = (android.graphics.Bitmap) render.invoke(receiver, values);
            require(bitmap.getWidth() == 64 && bitmap.getHeight() == 64, "SVG rendering failed");
            bitmap.recycle();
            Activity activity = startActivitySync(new Intent().setClassName(context.getPackageName(), context.getPackageName() + ".MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            if ("true".equals(arguments.getString("testFolderBack"))) Thread.sleep(1000); else waitForIdleSync();
            require(!activity.isFinishing(), "Activity failed to start");
            if ("true".equals(arguments.getString("testFolderBack"))) {
                testFolderBack(activity);
                result.putString("result", "PASS: folder and tag hierarchy, root exit confirmation timeout");
                finish(Activity.RESULT_OK, result); return;
            }
            if ("true".equals(arguments.getString("testIndexExport"))) {
                java.lang.reflect.Method current = activity.getClass().getDeclaredMethod("currentLibrary");
                current.setAccessible(true);
                Object saved = current.invoke(activity);
                java.lang.reflect.Method writer = null;
                for (java.lang.reflect.Method method : activity.getClass().getDeclaredMethods()) if (method.getName().equals("writeLibraryIndex")) writer = method;
                require(writer != null, "Backup writer missing"); writer.setAccessible(true);
                File directory = new File(context.getCacheDir(), "external-files/backup-smoke"); directory.mkdirs();
                for (int i = 0; i < 2; i++) {
                    File zip = new File(directory, "backup-" + i + ".zip");
                    try {
                        android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".files", zip);
                        writer.invoke(activity, saved, uri);
                        try (java.util.zip.ZipFile archive = new java.util.zip.ZipFile(zip)) {
                            require(archive.getEntry("backup.json") != null && archive.getEntry("index.json") != null && archive.getEntry("folders.json") != null, "Missing backup entries");
                        }
                    } finally { zip.delete(); }
                }
                directory.delete();
                result.putString("backup", "PASS: shared index export writer creates two independent compatible ZIPs");
            }
            result.putString("result", "PASS: optimized startup, persisted indexes/connections, StAX and SVG");
            result.putInt("indexFiles", files.length);
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            if (error instanceof java.lang.reflect.InvocationTargetException && error.getCause() != null) error = error.getCause();
            result.putString("result", "FAIL: " + error.getClass().getSimpleName() + ": " + error.getMessage());
            finish(Activity.RESULT_CANCELED, result);
        }
    }

    private Object invoke(Object receiver, String name, Object... args) throws Exception {
        for (java.lang.reflect.Method method : receiver.getClass().getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                method.setAccessible(true); return method.invoke(receiver, args);
            }
        }
        // Optimized getters can be inlined into direct field reads.
        if (name.startsWith("get") && args.length == 0) {
            String fieldName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            java.lang.reflect.Field field = receiver.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); return field.get(receiver);
        }
        throw new NoSuchMethodException(name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void testFolderBack(Activity activity) throws Exception {
        Object original = invoke(activity, "currentBrowse");
        Class<?> section = activity.getClassLoader().loadClass("com.tai.oeviewer.LibrarySection");
        Object folderSection = Enum.valueOf((Class) section, "FOLDER");
        java.util.List<?> roots = (java.util.List<?>) invoke(activity, "getFolderTree");
        Object parent = null, child = null;
        for (Object node : roots) {
            java.util.List<?> children = (java.util.List<?>) invoke(node, "getChildren");
            if (!children.isEmpty()) { parent = node; child = children.get(0); break; }
        }
        require(child != null, "Need a real nested test folder");
        String parentId = (String) invoke(parent, "getId"), childId = (String) invoke(child, "getId");
        String parentName = (String) invoke(parent, "getName"), childName = (String) invoke(child, "getName");
        try {
            navigationStatus("navigate");
            navigateTest(activity, folderSection, null, "文件夹");
            navigateTest(activity, folderSection, parentId, parentName);
            navigateTest(activity, folderSection, childId, childName);
            runOnMainSync(activity::onBackPressed);
            Thread.sleep(500);
            require(parentId.equals(invoke(invoke(activity, "currentBrowse"), "getFolderId")), "Commit did not reach parent");
            navigationStatus("root and exit confirmation");
            runOnMainSync(activity::onBackPressed);
            Thread.sleep(500);
            Object root = invoke(activity, "currentBrowse");
            require(invoke(root, "getFolderId") == null && folderSection.equals(invoke(root, "getSection")), "Skipped folder root");
            navigationStatus("tag hierarchy");
            java.util.List<JSONObject> groups = (java.util.List<JSONObject>) invoke(activity, "availableTagGroups");
            JSONObject group = null;
            for (JSONObject candidate : groups) if (candidate.getJSONArray("tags").length() > 0) { group = candidate; break; }
            require(group != null, "Need a real tag group");
            final String groupId = group.getString("id"), groupName = group.getString("name"), tagName = group.getJSONArray("tags").getString(0);
            Object tagRoot = Enum.valueOf((Class) section, "TAGGROUPS"), tagGroup = Enum.valueOf((Class) section, "TAGGROUP"), tagSection = Enum.valueOf((Class) section, "TAG");
            navigateTest(activity, tagRoot, null, "标签管理");
            runOnMainSync(() -> { try { invoke(activity, "navigateBrowse", tagSection, null, tagName, tagName, null); }
                catch (Exception error) { throw new RuntimeException(error); } });
            Thread.sleep(500);
            runOnMainSync(activity::onBackPressed); Thread.sleep(500);
            require(tagRoot.equals(invoke(invoke(activity, "currentBrowse"), "getSection")), "Total-list tag invented a parent group");
            runOnMainSync(() -> { try { invoke(activity, "navigateBrowse", tagGroup, null, groupId, groupName, null); }
                catch (Exception error) { throw new RuntimeException(error); } });
            Thread.sleep(500);
            runOnMainSync(() -> { try { invoke(activity, "navigateBrowse", tagSection, null, tagName, tagName, groupId); }
                catch (Exception error) { throw new RuntimeException(error); } });
            Thread.sleep(500);
            runOnMainSync(activity::onBackPressed); Thread.sleep(500);
            Object returnedGroup = invoke(activity, "currentBrowse");
            require(tagGroup.equals(invoke(returnedGroup, "getSection")) && groupId.equals(invoke(returnedGroup, "getTag")), "Tag skipped its group");
            runOnMainSync(activity::onBackPressed); Thread.sleep(500);
            require(tagRoot.equals(invoke(invoke(activity, "currentBrowse"), "getSection")), "Group skipped tag management");
            runOnMainSync(activity::onBackPressed);
            Thread.sleep(500);
            require(!activity.isFinishing(), "First root back exited");
            Thread.sleep(2100);
            runOnMainSync(activity::onBackPressed);
            Thread.sleep(300);
            require(!activity.isFinishing(), "Exit confirmation did not expire");
            runOnMainSync(activity::onBackPressed);
            Thread.sleep(600);
            require(activity.isFinishing() || !activity.hasWindowFocus(), "Second root back did not return to desktop");
        } finally {
            runOnMainSync(() -> {
                try { invoke(activity, "restoreBrowse", original); }
                catch (Exception error) { throw new RuntimeException(error); }
            });
        }
    }

    private void navigationStatus(String step) {
        Bundle status = new Bundle(); status.putString("navigation", step); sendStatus(20, status);
    }

    private void navigateTest(Activity activity, Object section, String id, String name) {
        runOnMainSync(() -> {
            try { invoke(activity, "navigateBrowse", section, id, null, name, null); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
        android.os.SystemClock.sleep(500);
    }
}
