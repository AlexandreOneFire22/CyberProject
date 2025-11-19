package com.example.cyberproject;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import android.view.View;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private EditText etMessage;
    private Button btnSend;
    private ListView lvMessages;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> items = new ArrayList<>();

    private DatabaseReference chatRef;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        lvMessages = findViewById(R.id.lvMessages);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        lvMessages.setAdapter(adapter);

        // Récupère le compte choisi
        currentUser = getIntent().getStringExtra("userId");
        if (currentUser == null) currentUser = "compte1"; // sécurité

        // comptePIRATE = lecture seule + cache les boutons d'envoi
        if (currentUser.equals("comptePIRATE")) {
            etMessage.setVisibility(View.GONE);
            btnSend.setVisibility(View.GONE);
        }

        chatRef = FirebaseDatabase.getInstance().getReference("chat_global");

        // ENVOI D'UN MESSAGE : on chiffre AVANT d'envoyer dans Firebase
        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (msg.isEmpty()) return;

            // Chiffrement RSA perso
            String cipher = CryptoUtils.encrypt(msg);

            Map<String, Object> data = new HashMap<>();
            data.put("cipher", cipher);                 // seul texte stocké : CHIFFRÉ
            data.put("sender", currentUser);
            data.put("timestamp", ServerValue.TIMESTAMP);

            chatRef.push().setValue(data);
            etMessage.setText("");
        });

        // BOUTON DECONNEXION
        Button btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SelectUserActivity.class));
            finish();
        });

        listenForMessages();
    }

    private void listenForMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                items.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                for (DataSnapshot child : snapshot.getChildren()) {
                    String cipher = child.child("cipher").getValue(String.class);
                    String sender = child.child("sender").getValue(String.class);
                    Long time = child.child("timestamp").getValue(Long.class);

                    if (cipher == null || sender == null || time == null) continue;

                    String when = sdf.format(new Date(time));

                    String displayText;
                    if (currentUser.equals("comptePIRATE")) {
                        // 🔎 Vue "intrus" : il voit le texte CHIFFRÉ, pas le clair
                        displayText = cipher;
                    } else {
                        // 👤 compte1 / compte2 : déchiffrage RSA
                        try {
                            displayText = CryptoUtils.decrypt(cipher);
                        } catch (Exception e) {
                            displayText = "[ERREUR DECHIFFREMENT] " + cipher;
                        }
                    }

                    items.add("[" + when + "] " + sender + ": " + displayText);
                }

                adapter.notifyDataSetChanged();
                lvMessages.setSelection(items.size() - 1);
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}
