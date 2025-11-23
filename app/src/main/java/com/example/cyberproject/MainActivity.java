package com.example.cyberproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText etMessage;
    private Button btnSend, btnLogout;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private ArrayList<Message> messages = new ArrayList<>();

    private DatabaseReference chatRef;
    private String currentUser;

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

        // Récupération du compte choisi
        currentUser = getIntent().getStringExtra("userId");
        if (currentUser == null) currentUser = "compte1"; // fallback sécurité

        // comptePIRATE : lecture seule + cache boutons
        if (currentUser.equals("comptePIRATE")) {
            etMessage.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
        }

        // RecyclerView setup
        adapter = new MessageAdapter(this, messages, currentUser);
        rvMessages.setAdapter(adapter);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));

        // Référence Firebase
        chatRef = FirebaseDatabase.getInstance().getReference("chat_global");

        // Bouton d’envoi
        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (msg.isEmpty()) return;

            // Chiffrement RSA
            String cipher = CryptoUtils.encrypt(msg);

            Map<String, Object> data = new HashMap<>();
            data.put("cipher", cipher);
            data.put("sender", currentUser);
            data.put("timestamp", ServerValue.TIMESTAMP);

            chatRef.push().setValue(data);
            etMessage.setText("");

            // Masquer clavier après envoi
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(etMessage.getWindowToken(), 0);
        });

        // Bouton déconnexion
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SelectUserActivity.class));
            finish();
        });

        listenForMessages();
    }

    private void listenForMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messages.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String cipher = child.child("cipher").getValue(String.class);
                    String sender = child.child("sender").getValue(String.class);
                    Long time = child.child("timestamp").getValue(Long.class);

                    if (cipher == null || sender == null || time == null) continue;

                    String displayText;
                    if (currentUser.equals("comptePIRATE")) {
                        // Vue pirate : voir seulement le texte chiffré
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

                adapter.notifyDataSetChanged();
                if (!messages.isEmpty())
                    rvMessages.scrollToPosition(messages.size() - 1);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}
