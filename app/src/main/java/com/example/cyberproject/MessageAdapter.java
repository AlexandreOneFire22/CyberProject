package com.example.cyberproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
        holder.txtMessage.setText(msg.getText());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        holder.txtTime.setText(sdf.format(new Date(msg.getTimestamp())));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMessageClick(msg);
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
