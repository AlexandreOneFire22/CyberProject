package com.example.cyberproject;

public class CryptoUtils {

    // RSA paramètres imposés
    private static final int N = 2773;
    private static final int E = 17;
    private static final int D = 157;

    // Alphabet EXACT :
    // 00 = espace, 01-26 = a-z, 27 = '
    // + tout le reste (majuscules, chiffres, ponctuation...) à la suite.
    private static final String ALPHABET =
            " abcdefghijklmnopqrstuvwxyz'" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "0123456789" +
                    ".,;:!?()[]{}-_/\\\"@#%&*+=<>|^~";

    // ---- PUBLIC API ----

    /** Chiffre un message texte -> ciphertext sous forme de blocs "2297 0170 0813 ..." */
    public static String encrypt(String message) {
        if (message == null) return "";

        // 1) Convertir message -> chaîne numérique (paquets de 2 chiffres)
        String numeric = textToNumeric(message);

        // 2) Regrouper en blocs de <=4 chiffres et chiffrer chaque bloc avec RSA (e=17)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numeric.length(); i += 4) {
            int end = Math.min(i + 4, numeric.length());
            String blockStr = numeric.substring(i, end);   // ex: "1027", "0109", "2", ...
            int x = Integer.parseInt(blockStr);            // ex: 1027, 109, 2...

            int y = modPow(x, E, N);                       // RSA chiffrement

            if (sb.length() > 0) sb.append(" ");
            sb.append(String.format("%04d", y));           // toujours 4 chiffres : "2297", "0170"...
        }
        return sb.toString();
    }

    /** Déchiffre un ciphertext "2297 0170 0813 ..." -> texte clair. */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) return "";

        // 1) Découper les blocs chiffrés "2297 0170 ..."
        String[] parts = cipherText.trim().split("\\s+");
        StringBuilder numericPlain = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;
            int y = Integer.parseInt(part);
            int x = modPow(y, D, N);                       // RSA déchiffrement

            numericPlain.append(String.format("%04d", x)); // 4 chiffres: "1027", "0002", ...
        }

        // 2) Repasser de la chaîne numérique -> texte (paquets de 2 chiffres)
        return numericToText(numericPlain.toString());
    }

    // ---- OUTILS PRIVES ----

    /** Convertit le texte en chaîne numérique (2 chiffres par caractère). */
    private static String textToNumeric(String message) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            int idx = ALPHABET.indexOf(c);

            if (idx == -1) {
                // Caractère non prévu : pour respecter ta demande "tout doit marcher",
                // on le remplace par un espace plutôt que planter.
                idx = 0; // espace
            }

            sb.append(String.format("%02d", idx)); // toujours 2 chiffres
        }

        return sb.toString();
    }

    /** Convertit la chaîne numérique (paquets de 2 chiffres) en texte clair. */
    private static String numericToText(String numeric) {
        StringBuilder sb = new StringBuilder();

        // Parcours par paquets de 2 chiffres
        for (int i = 0; i + 2 <= numeric.length(); i += 2) {
            String codeStr = numeric.substring(i, i + 2);
            int idx = Integer.parseInt(codeStr);

            if (idx >= 0 && idx < ALPHABET.length()) {
                sb.append(ALPHABET.charAt(idx));
            } else {
                // Code inconnu : on met un � (ou un ? si tu préfères)
                sb.append('�');
            }
        }

        return sb.toString();
    }

    /** Exponentiation modulaire rapide : base^exp mod mod. */
    private static int modPow(int base, int exp, int mod) {
        long result = 1;
        long b = base % mod;

        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = (result * b) % mod;
            }
            b = (b * b) % mod;
            exp >>= 1;
        }
        return (int) result;
    }
}
