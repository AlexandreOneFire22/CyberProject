package com.example.cyberproject;

import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesMediaUtils {

    public static class AesPack {
        public byte[] key;   // 32 bytes
        public byte[] iv;    // 12 bytes (GCM)
        public byte[] cipherBytes;
    }

    /** Génère clé AES-256 */
    public static byte[] randomAesKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey sk = kg.generateKey();
        return sk.getEncoded();
    }

    /** Génère IV 12 bytes */
    public static byte[] randomIv() {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /** Chiffre bytes -> AES/GCM */
    public static AesPack encryptBytes(byte[] plain) throws Exception {
        AesPack pack = new AesPack();
        pack.key = randomAesKey();
        pack.iv = randomIv();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(pack.key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, pack.iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        pack.cipherBytes = cipher.doFinal(plain);
        return pack;
    }

    /** Déchiffre AES/GCM */
    public static byte[] decryptBytes(byte[] cipherBytes, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        return cipher.doFinal(cipherBytes);
    }
}