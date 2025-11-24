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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private EditText etMessage;
    private Button btnSend, btnLogout;
    private ImageButton btnPlus; // ✅ AJOUT
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private ArrayList<Message> messages = new ArrayList<>();

    private DatabaseReference chatRef;
    private StorageReference storageRef; // ✅ AJOUT
    private String currentUser;

    private ActivityResultLauncher<String[]> pickMediaLauncher; // ✅ AJOUT

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force la compatibilité pour le redimensionnement clavier
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnLogout = findViewById(R.id.btnLogout);
        rvMessages = findViewById(R.id.rvMessages);
        btnPlus = findViewById(R.id.btnPlus); // ✅ AJOUT (bouton + dans ton XML)

        // Récupération du compte choisi
        currentUser = getIntent().getStringExtra("userId");
        if (currentUser == null) currentUser = "compte1"; // fallback sécurité

        // comptePIRATE : lecture seule + cache boutons
        if (currentUser.equals("comptePIRATE")) {
            etMessage.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
            if (btnPlus != null) btnPlus.setVisibility(View.GONE); // ✅ AJOUT
        }

        // RecyclerView setup
        adapter = new MessageAdapter(this, messages, currentUser);
        rvMessages.setAdapter(adapter);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        // Référence Firebase
        chatRef = FirebaseDatabase.getInstance().getReference("chat_global");
        storageRef = FirebaseStorage.getInstance().getReference("media_global"); // ✅ AJOUT

        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();

            // Si plus de 200px ont disparu → le clavier est ouvert
            if (heightDiff > 200) {
                rvMessages.postDelayed(() ->
                                rvMessages.scrollToPosition(messages.size() - 1),
                        100
                );
            }
        });

        // Bouton d’envoi TEXTE (inchangé)
        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (msg.isEmpty()) return;

            // Chiffrement RSA
            String cipher = CryptoUtils.encrypt(msg);

            Map<String, Object> data = new HashMap<>();
            data.put("type", "text");          // ✅ AJOUT (type)
            data.put("cipher", cipher);
            data.put("sender", currentUser);
            data.put("timestamp", ServerValue.TIMESTAMP);

            chatRef.push().setValue(data);
            etMessage.setText("");

            // Masquer clavier après envoi
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
        });

        // ✅ AJOUT : Picker galerie/son/vidéo
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) sendMediaMessage(uri);
                }
        );

        // ✅ AJOUT : clic sur +
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                pickMediaLauncher.launch(new String[]{"image/*", "video/*", "audio/*"});
            });
        }

        // Bouton déconnexion (inchangé)
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SelectUserActivity.class));
            finish();
        });

        listenForMessages();
    }

    // ✅ AJOUT : envoi média AES + RSA(key/iv)
    private void sendMediaMessage(Uri uri) {
        try {
            byte[] plainBytes = readAllBytes(uri);

            // AES encrypt
            AesMediaUtils.AesPack pack = AesMediaUtils.encryptBytes(plainBytes);

            // base64 key & iv
            String keyB64 = Base64.encodeToString(pack.key, Base64.NO_WRAP);
            String ivB64  = Base64.encodeToString(pack.iv, Base64.NO_WRAP);

            // RSA encrypt key & iv
            String keyCipher = CryptoUtils.encrypt(keyB64);
            String ivCipher  = CryptoUtils.encrypt(ivB64);

            // MIME final pour lambda
            String detectedMime = getContentResolver().getType(uri);
            if (detectedMime == null) detectedMime = "application/octet-stream";
            final String mime = detectedMime;

            // upload bytes chiffrés vers Storage
            String fileName = UUID.randomUUID().toString();
            StorageReference fileRef = storageRef.child(fileName);

            UploadTask uploadTask = fileRef.putBytes(pack.cipherBytes);
            uploadTask.addOnSuccessListener(taskSnapshot ->
                    fileRef.getDownloadUrl().addOnSuccessListener(downloadUrl -> {

                        Map<String, Object> data = new HashMap<>();
                        data.put("type", "media");
                        data.put("mediaUrl", downloadUrl.toString());
                        data.put("mediaMime", mime);
                        data.put("mediaKeyCipher", keyCipher);
                        data.put("mediaIvCipher", ivCipher);
                        data.put("sender", currentUser);
                        data.put("timestamp", ServerValue.TIMESTAMP);

                        chatRef.push().setValue(data);
                    })
            ).addOnFailureListener(e ->
                    Toast.makeText(this, "Upload échoué: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );

        } catch (Exception e) {
            Toast.makeText(this, "Erreur media: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ✅ AJOUT : lecture bytes desde Uri
    private byte[] readAllBytes(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        if (is == null) return new byte[0];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int n;
        while ((n = is.read(data)) != -1) buffer.write(data, 0, n);
        is.close();
        return buffer.toByteArray();
    }

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
                    }

                    // ✅ AJOUT : afficher placeholder média sans casser ton adapter actuel
                    else if (type.equals("media")) {
                        String url = child.child("mediaUrl").getValue(String.class);
                        String mime = child.child("mediaMime").getValue(String.class);
                        String keyCipher = child.child("mediaKeyCipher").getValue(String.class);
                        String ivCipher  = child.child("mediaIvCipher").getValue(String.class);

                        if (url == null || mime == null || keyCipher == null || ivCipher == null) continue;

                        String displayText;
                        if (currentUser.equals("comptePIRATE")) {
                            displayText = "[MEDIA CHIFFRÉ] " + mime;
                        } else {
                            displayText = "[MEDIA] " + mime + " (tap pour ouvrir)";
                        }

                        messages.add(new Message(displayText, sender, time));
                        // ⚠️ pour ouvrir vraiment le média il faudra adapter Message/Adapter
                    }
                }

                adapter.notifyDataSetChanged();
                if (!messages.isEmpty())
                    rvMessages.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}