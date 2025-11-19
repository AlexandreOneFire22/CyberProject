package com.example.cyberproject;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText messageInput;
    private Button sendButton;
    private ListView messageList;

    private DatabaseReference dbRef;
    private ArrayList<String> messages = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        messageList = findViewById(R.id.messageList);

        // Lien vers ta base Firebase
        dbRef = FirebaseDatabase.getInstance().getReference("messages");

        // Adapter pour afficher la liste des messages
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageList.setAdapter(adapter);

        // Envoi du message
        sendButton.setOnClickListener(v -> {
            String msg = messageInput.getText().toString();
            if (!msg.isEmpty()) {
                dbRef.push().setValue(msg); // Envoie dans Firebase
                messageInput.setText("");
            }
        });

        // Réception en temps réel
        dbRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                messages.clear();
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    messages.add(child.getValue(String.class));
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
            }
        });
    }
}