package com.example.cyberproject;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesMediaUtils {

    public static class AesPack {
        public byte[] key;         // 32 octets (AES-256)
        public byte[] iv;          // 12 octets (GCM)
        public byte[] cipherBytes; // données chiffrées
    }

    public static AesPack encryptBytes(byte[] plain) throws Exception {
        AesPack pack = new AesPack();

        // Génération clé AES-256
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey sk = kg.generateKey();
        pack.key = sk.getEncoded();

        // IV 12 octets
        pack.iv = new byte[12];
        new SecureRandom().nextBytes(pack.iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(pack.key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, pack.iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        pack.cipherBytes = cipher.doFinal(plain);

        return pack;
    }

    public static byte[] decryptBytes(byte[] cipherBytes, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        return cipher.doFinal(cipherBytes);
    }
}
