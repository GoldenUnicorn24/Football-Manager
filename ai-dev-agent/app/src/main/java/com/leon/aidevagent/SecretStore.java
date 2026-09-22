package com.leon.aidevagent;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecretStore {
    private static final String KS = "AndroidKeyStore";
    private static final String ALIAS = "ai_dev_agent_secrets_v1";
    private static final String PREF = "secure_secrets";
    private final SharedPreferences prefs;

    SecretStore(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    void put(String key, String value) throws Exception {
        if (value == null || value.isEmpty()) {
            prefs.edit().remove(key + ".ct").remove(key + ".iv").apply();
            return;
        }
        SecretKey secretKey = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        prefs.edit()
                .putString(key + ".ct", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(key + ".iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .apply();
    }

    String get(String key) {
        try {
            String ct = prefs.getString(key + ".ct", null);
            String iv = prefs.getString(key + ".iv", null);
            if (ct == null || iv == null) return "";
            KeyStore keyStore = KeyStore.getInstance(KS);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(ALIAS, null);
            if (secretKey == null) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(ct, Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KS);
        keyStore.load(null);
        if (keyStore.containsAlias(ALIAS)) return (SecretKey) keyStore.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS);
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }
}
