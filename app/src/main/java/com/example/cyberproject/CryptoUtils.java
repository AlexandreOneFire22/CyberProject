package com.example.cyberproject;

public class CryptoUtils {

    private static final int N = 2773;
    private static final int E = 17;
    private static final int D = 157;

    // -------------------------------------------------------------
    // PUBLIC API
    // -------------------------------------------------------------

    /** Chiffre un message Unicode complet */
    public static String encrypt(String message) {
        if (message == null) return "";

        // UTF-16 → blocs 4 chiffres
        String numeric = unicodeToNumeric(message);

        StringBuilder sb = new StringBuilder();

        // RSA nécessite blocs < N → on découpe 1 à 1
        for (int i = 0; i < numeric.length(); i += 4) {
            int end = Math.min(i + 4, numeric.length());
            int x = Integer.parseInt(numeric.substring(i, end));

            // sécurité si > N
            if (x >= N) x = x % N;

            int y = modPow(x, E, N);

            if (sb.length() > 0) sb.append(" ");
            sb.append(String.format("%04d", y));
        }

        return sb.toString();
    }

    /** Déchiffre un ciphertext RSA → texte Unicode */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return "";

        String[] parts = cipherText.trim().split("\\s+");
        StringBuilder numeric = new StringBuilder();

        for (String p : parts) {
            int y = Integer.parseInt(p);
            int x = modPow(y, D, N);

            numeric.append(String.format("%04d", x));
        }

        return numericToUnicode(numeric.toString());
    }

    // -------------------------------------------------------------
    // INTERNAL UNICODE ENCODING
    // -------------------------------------------------------------

    /** Convertit un texte Unicode → suite de blocs 4 chiffres */
    private static String unicodeToNumeric(String msg) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < msg.length(); i++) {
            int codePoint = msg.codePointAt(i);

            // UTF-16 supplementary characters (emoji, etc.)
            if (Character.isSupplementaryCodePoint(codePoint)) {

                // CORRECTION ICI
                char[] parts = Character.toChars(codePoint);

                for (char c : parts) {
                    sb.append(String.format("%04d", (int) c));
                }

                i++; // sauter la 2e moitié du surrogate pair
            } else {
                sb.append(String.format("%04d", codePoint));
            }
        }

        return sb.toString();
    }

    /** Reconstruit Unicode depuis blocs de 4 chiffres */
    private static String numericToUnicode(String numeric) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i + 4 <= numeric.length(); i += 4) {
            int code = Integer.parseInt(numeric.substring(i, i + 4));
            sb.append((char) code);
        }

        return sb.toString();
    }

    // -------------------------------------------------------------
    // MODULAR EXPONENTIATION
    // -------------------------------------------------------------
    private static int modPow(int base, int exp, int mod) {
        long result = 1;
        long b = base % mod;

        while (exp > 0) {
            if ((exp & 1) == 1) result = (result * b) % mod;
            b = (b * b) % mod;
            exp >>= 1;
        }

        return (int) result;
    }
}