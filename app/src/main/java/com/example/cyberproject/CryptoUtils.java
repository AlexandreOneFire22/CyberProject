package com.example.cyberproject;

public class CryptoUtils {

    // RSA paramètres imposés
    private static final int N = 2773;
    private static final int E = 17;
    private static final int D = 157;

    /**
     * Alphabet FIXE et STABLE.
     * 95 caractères ASCII imprimables (32 → 126)
     * Cela permet d'accepter absolument tout :
     * lettres, chiffres, majuscules, minuscules,
     * accents supprimés proprement,
     * ponctuation, symboles, emojis (remplacés proprement).
     */
    private static final String ALPHABET;

    static {
        StringBuilder sb = new StringBuilder();
        // ASCII imprimables
        for (int i = 32; i <= 126; i++) {
            sb.append((char) i);
        }
        ALPHABET = sb.toString();   // 95 caractères → codes 00 à 94
    }

    // ------------------------------------------------------------
    // ---------------------- PUBLIC API ---------------------------
    // ------------------------------------------------------------

    /** Chiffre un message en blocs RSA "2297 0170 0813 ..." */
    public static String encrypt(String message) {
        if (message == null) return "";

        String numeric = textToNumeric(message);

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < numeric.length(); i += 4) {
            int end = Math.min(i + 4, numeric.length());
            int x = Integer.parseInt(numeric.substring(i, end));

            int y = modPow(x, E, N);

            if (sb.length() > 0) sb.append(" ");
            sb.append(String.format("%04d", y));
        }

        return sb.toString();
    }

    /** Déchiffre un ciphertext RSA en texte clair. */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return "";

        StringBuilder numeric = new StringBuilder();
        String[] parts = cipherText.trim().split("\\s+");

        for (String block : parts) {
            int y = Integer.parseInt(block);
            int x = modPow(y, D, N);

            numeric.append(String.format("%04d", x));
        }

        return numericToText(numeric.toString());
    }

    // ------------------------------------------------------------
    // ------------------- INTERNAL FUNCTIONS ---------------------
    // ------------------------------------------------------------

    /** Texte → chaîne numérique (paires de 2 chiffres) */
    private static String textToNumeric(String msg) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);

            // Pour caractères hors ASCII imprimables (ex: emoji) → '?'
            if (c < 32 || c > 126) c = '?';

            int idx = ALPHABET.indexOf(c);

            sb.append(String.format("%02d", idx));
        }

        return sb.toString();
    }

    /** Chaîne numérique → texte clair */
    private static String numericToText(String numeric) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i + 2 <= numeric.length(); i += 2) {
            int idx = Integer.parseInt(numeric.substring(i, i + 2));

            if (idx >= 0 && idx < ALPHABET.length())
                sb.append(ALPHABET.charAt(idx));
            else
                sb.append('?');
        }

        return sb.toString();
    }

    /** Exponentiation modulaire rapide (base^exp mod mod) */
    private static int modPow(int base, int exp, int mod) {
        long result = 1;
        long b = base % mod;

        while (exp > 0) {
            if ((exp & 1) == 1)
                result = (result * b) % mod;

            b = (b * b) % mod;
            exp >>= 1;
        }
        return (int) result;
    }
}
