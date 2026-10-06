package com.tai.oeviewer;

import android.content.Context;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class TokenStore {
    private static final String ALIAS = "oe-viewer-refresh-token";
    private static final String PREFS = "secure-auth";

    static void save(Context context, String token) throws Exception {
        saveSecret(context, "token", token);
    }

    static void saveSecret(Context context, String name, String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        String ivName = name.equals("token") ? "iv" : name + "-iv";
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(name, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(ivName, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .apply();
    }

    static String load(Context context) {
        return loadSecret(context, "token");
    }

    static String loadSecret(Context context, String name) {
        try {
            var prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String token = prefs.getString(name, null);
            String iv = prefs.getString(name.equals("token") ? "iv" : name + "-iv", null);
            if (token == null || iv == null) return null;
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(token, Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    static void removeSecret(Context context, String name) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(name).remove(name.equals("token") ? "iv" : name + "-iv").apply();
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT |
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            generator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();
    }

    private TokenStore() {}
}
