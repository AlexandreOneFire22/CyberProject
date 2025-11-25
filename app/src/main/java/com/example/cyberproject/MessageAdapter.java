package com.example.cyberproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Paint;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    public interface OnMessageClickListener {
        void onMessageClick(Message message);
    }

    private ArrayList<Message> messages;
    private Context context;
    private String currentUser;
    private OnMessageClickListener listener;

    public MessageAdapter(Context context, ArrayList<Message> messages, String currentUser,
                          OnMessageClickListener listener) {
        this.context = context;
        this.messages = messages;
        this.currentUser = currentUser;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        if (msg.getSender().equals(currentUser)) return TYPE_SENT;
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SENT) {
            view = LayoutInflater.from(context).inflate(R.layout.bubble_right, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.bubble_left, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        Message msg = messages.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        holder.txtTime.setText(sdf.format(new Date(msg.getTimestamp())));

        // -------------------------
        // CAS 1 : MESSAGE TEXTE
        // -------------------------
        if (!msg.isMedia()) {

            // Compte pirate : afficher le texte chiffré brut
            if (currentUser.equals("comptePIRATE")) {
                holder.txtMessage.setText(msg.getText());
                holder.txtMessage.setTextColor(0xFF000000);
                holder.txtMessage.setPaintFlags(holder.txtMessage.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
                holder.itemView.setOnClickListener(null);
                return;
            }

            // Compte normal
            holder.txtMessage.setText(msg.getText());
            holder.txtMessage.setTextColor(0xFF000000);
            holder.txtMessage.setPaintFlags(holder.txtMessage.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            holder.itemView.setClickable(false);
            return;
        }

        // -------------------------
        // CAS 2 : MEDIA POUR COMPTE PIRATE (AFFICHAGE BRUT)
        // -------------------------
        if (currentUser.equals("comptePIRATE")) {

            String supabaseUrl =
                    "https://csxqhbltrwtgfactwkln.supabase.co/storage/v1/object/Media/" +
                            msg.getMediaPath();

            String raw =
                    "type=media\n" +
                            "supabaseUrl=" + supabaseUrl + "\n" +
                            "mediaPath=" + msg.getMediaPath() + "\n" +
                            "mediaMime=" + msg.getMediaMime() + "\n" +
                            "mediaKeyCipher=" + msg.getMediaKeyCipher() + "\n" +
                            "mediaIvCipher=" + msg.getMediaIvCipher() + "\n" +
                            "sender=" + msg.getSender() + "\n" +
                            "timestamp=" + msg.getTimestamp();

            holder.txtMessage.setText(raw);
            holder.txtMessage.setTextColor(0xFF000000); // noir
            holder.txtMessage.setPaintFlags(holder.txtMessage.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            holder.itemView.setOnClickListener(null); // PAS DE CLIC
            return;
        }

        // -------------------------
        // CAS 3 : MEDIA POUR COMPTE NORMAL → lien bleu
        // -------------------------
        String visuallyClearText = "📎 Média (" + msg.getMediaMime() + ")";

        holder.txtMessage.setText(visuallyClearText);
        holder.txtMessage.setTextColor(0xFF1565C0); // bleu style lien
        holder.txtMessage.setPaintFlags(holder.txtMessage.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).openMediaMessage(msg);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
        }
    }
}
