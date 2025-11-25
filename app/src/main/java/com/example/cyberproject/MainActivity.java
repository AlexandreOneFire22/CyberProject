package com.example.cyberproject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String SUPABASE_URL = "https://csxqhbltrwtgfactwkln.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNzeHFoYmx0cnd0Z2ZhY3R3a2xuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQwMTc2MzMsImV4cCI6MjA3OTU5MzYzM30.7brB0xQF3u5g9iKgpEzIcd2r5ATrTbVgWAECC1_L5mA";
    private static final String SUPABASE_BUCKET = "Media";

    private EditText etMessage;
    private Button btnSend, btnLogout;
    private ImageButton btnPlus;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private ArrayList<Message> messages = new ArrayList<>();

    private DatabaseReference chatRef;
    private String currentUser;

    private ActivityResultLauncher<String[]> pickMediaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // redimensionnement clavier
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnLogout = findViewById(R.id.btnLogout);
        rvMessages = findViewById(R.id.rvMessages);
        btnPlus = findViewById(R.id.btnPlus);

        // Récupération du compte
        currentUser = getIntent().getStringExtra("userId");
        if (currentUser == null) currentUser = "compte1";

        // comptePIRATE : lecture seule
        if (currentUser.equals("comptePIRATE")) {
            etMessage.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
            if (btnPlus != null) btnPlus.setVisibility(View.GONE);
        }

        // RecyclerView
        adapter = new MessageAdapter(this, messages, currentUser, message -> {
            if (message.isMedia() && !currentUser.equals("comptePIRATE")) {
                openMediaMessage(message);
            }
        });
        rvMessages.setAdapter(adapter);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        chatRef = FirebaseDatabase.getInstance().getReference("chat_global");

        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
            if (heightDiff > 200) {
                rvMessages.postDelayed(() ->
                                rvMessages.scrollToPosition(messages.size() - 1),
                        100
                );
            }
        });

        // ENVOI TEXTE
        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (msg.isEmpty()) return;

            String cipher = CryptoUtils.encrypt(msg);

            Map<String, Object> data = new HashMap<>();
            data.put("type", "text");
            data.put("cipher", cipher);
            data.put("sender", currentUser);
            data.put("timestamp", ServerValue.TIMESTAMP);

            chatRef.push().setValue(data);
            etMessage.setText("");

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
        });

        // PICKER MEDIA
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) sendMediaMessage(uri);
                }
        );

        if (btnPlus != null && !currentUser.equals("comptePIRATE")) {
            btnPlus.setOnClickListener(v ->
                    pickMediaLauncher.launch(new String[]{"image/*", "video/*", "audio/*"})
            );
        }

        // Déconnexion
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SelectUserActivity.class));
            finish();
        });

        listenForMessages();
    }

    // ---------------- LISTEN FOR MESSAGES ----------------
    private void listenForMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messages.clear();

                for (DataSnapshot child : snapshot.getChildren()) {

                    String type = child.child("type").getValue(String.class);
                    if (type == null) type = "text";

                    String sender = child.child("sender").getValue(String.class);
                    Long time = child.child("timestamp").getValue(Long.class);
                    if (sender == null || time == null) continue;

                    if (type.equals("text")) {
                        String cipher = child.child("cipher").getValue(String.class);
                        if (cipher == null) continue;

                        String displayText;
                        if (currentUser.equals("comptePIRATE")) {
                            displayText = cipher.replace(" ", "");
                        } else {
                            try {
                                displayText = CryptoUtils.decrypt(cipher);
                            } catch (Exception e) {
                                displayText = "[ERREUR DECHIFFREMENT] " + cipher;
                            }
                        }

                        messages.add(new Message(displayText, sender, time));
                    } else if (type.equals("media")) {

                        String mediaPath = child.child("mediaPath").getValue(String.class);
                        String mediaMime = child.child("mediaMime").getValue(String.class);
                        String keyCipher = child.child("mediaKeyCipher").getValue(String.class);
                        String ivCipher = child.child("mediaIvCipher").getValue(String.class);

                        if (mediaPath == null || mediaMime == null || keyCipher == null || ivCipher == null)
                            continue;

                        String displayText;
                        if (currentUser.equals("comptePIRATE")) {
                            displayText = "[MEDIA CHIFFRÉ] " + mediaMime;
                        } else {
                            displayText = "Ouvrir le média (" + mediaMime + ")";
                        }

                        Message m = new Message(displayText, sender, time);
                        m.setMedia(true);
                        m.setMediaPath(mediaPath);
                        m.setMediaMime(mediaMime);
                        m.setMediaKeyCipher(keyCipher);
                        m.setMediaIvCipher(ivCipher);

                        messages.add(m);
                    }
                }

                adapter.notifyDataSetChanged();
                if (!messages.isEmpty())
                    rvMessages.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    // ---------------- ENVOI MEDIA ----------------
    private void sendMediaMessage(Uri uri) {
        if (currentUser.equals("comptePIRATE")) return;

        new Thread(() -> {
            try {
                byte[] plainBytes = readAllBytes(uri);

                AesMediaUtils.AesPack pack = AesMediaUtils.encryptBytes(plainBytes);

                String keyB64 = Base64.encodeToString(pack.key, Base64.NO_WRAP);
                String ivB64 = Base64.encodeToString(pack.iv, Base64.NO_WRAP);

                String keyCipher = CryptoUtils.encrypt(keyB64);
                String ivCipher = CryptoUtils.encrypt(ivB64);

                String detectedMime = getContentResolver().getType(uri);
                if (detectedMime == null) detectedMime = "application/octet-stream";
                String mimeFinal = detectedMime;

                String objectPath = uploadToSupabase(pack.cipherBytes);

                Map<String, Object> data = new HashMap<>();
                data.put("type", "media");
                data.put("mediaPath", objectPath);
                data.put("mediaMime", mimeFinal);
                data.put("mediaKeyCipher", keyCipher);
                data.put("mediaIvCipher", ivCipher);
                data.put("sender", currentUser);
                data.put("timestamp", ServerValue.TIMESTAMP);

                chatRef.push().setValue(data);

                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Média envoyé", Toast.LENGTH_SHORT).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Erreur média: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    // Upload vers Supabase Storage (bytes déjà chiffrés AES)
    private String uploadToSupabase(byte[] cipherBytes) throws Exception {
        String objectPath = UUID.randomUUID().toString() + ".bin";
        URL url = new URL(SUPABASE_URL + "/storage/v1/object/" + SUPABASE_BUCKET + "/" + objectPath);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);
        conn.setRequestProperty("Content-Type", "application/octet-stream");

        OutputStream os = conn.getOutputStream();
        os.write(cipherBytes);
        os.flush();
        os.close();

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            conn.disconnect();
            return objectPath;
        } else {
            InputStream err = conn.getErrorStream();
            String msg = (err != null) ? readStream(err) : ("HTTP " + code);
            conn.disconnect();
            throw new Exception("Supabase upload error: " + msg);
        }
    }

    // ---------------- OUVERTURE MEDIA ----------------
    public void openMediaMessage(Message m) {
        new Thread(() -> {
            try {
                String keyB64 = CryptoUtils.decrypt(m.getMediaKeyCipher());
                String ivB64 = CryptoUtils.decrypt(m.getMediaIvCipher());

                byte[] key = Base64.decode(keyB64, Base64.NO_WRAP);
                byte[] iv = Base64.decode(ivB64, Base64.NO_WRAP);

                byte[] cipherBytes = downloadFromSupabase(m.getMediaPath());

                byte[] plainBytes = AesMediaUtils.decryptBytes(cipherBytes, key, iv);

                // fichier temporaire + Intent VIEW
                Uri fileUri = FileHelper.writeTempFile(MainActivity.this, plainBytes, m.getMediaMime());

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, m.getMediaMime());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Impossible d'ouvrir le média", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Erreur ouverture média", Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private byte[] downloadFromSupabase(String objectPath) throws Exception {
        URL url = new URL(SUPABASE_URL + "/storage/v1/object/" + SUPABASE_BUCKET + "/" + objectPath);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
        conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_ANON_KEY);

        InputStream is = conn.getInputStream();
        byte[] data = readAllBytes(is);
        conn.disconnect();
        return data;
    }

    // ---------------- UTILITAIRES IO ----------------
    private byte[] readAllBytes(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) return new byte[0];
        byte[] data = readAllBytes(is);
        is.close();
        return data;
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) {
            buffer.write(tmp, 0, n);
        }
        return buffer.toByteArray();
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) {
            buffer.write(tmp, 0, n);
        }
        return buffer.toString("UTF-8");
    }
}
