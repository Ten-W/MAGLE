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
            require((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0, "Expected non-debuggable package");
            require("0.8.61".equals(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName), "Wrong version");
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
            require(expected == null || expected.equals(hash(new File(context.getApplicationInfo().dataDir, "shared_prefs/remote-libraries.xml"))), "Library connections changed");
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
            waitForIdleSync();
            require(!activity.isFinishing(), "Activity failed to start");
            result.putString("result", "PASS: optimized startup, persisted indexes/connections, StAX and SVG");
            result.putInt("indexFiles", files.length);
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("result", "FAIL: " + error.getClass().getSimpleName() + ": " + error.getMessage());
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
