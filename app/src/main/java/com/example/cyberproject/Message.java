package com.example.cyberproject;

public class Message {
    private String text;
    private String sender;
    private long timestamp;

    // Champs pour les médias
    private boolean media;
    private String mediaPath;       // chemin de l'objet dans Supabase (ex: "uuid.bin")
    private String mediaMime;       // type MIME (image/jpeg, video/mp4, audio/mpeg, ...)
    private String mediaKeyCipher;  // clé AES chiffrée (RSA)
    private String mediaIvCipher;   // IV AES chiffré (RSA)

    public Message() {
        // nécessaire pour Firebase
    }

    // Constructeur pour les messages texte
    public Message(String text, String sender, long timestamp) {
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
        this.media = false;
    }

    // Getters / setters texte
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Getters / setters média
    public boolean isMedia() { return media; }
    public void setMedia(boolean media) { this.media = media; }

    public String getMediaPath() { return mediaPath; }
    public void setMediaPath(String mediaPath) { this.mediaPath = mediaPath; }

    public String getMediaMime() { return mediaMime; }
    public void setMediaMime(String mediaMime) { this.mediaMime = mediaMime; }

    public String getMediaKeyCipher() { return mediaKeyCipher; }
    public void setMediaKeyCipher(String mediaKeyCipher) { this.mediaKeyCipher = mediaKeyCipher; }

    public String getMediaIvCipher() { return mediaIvCipher; }
    public void setMediaIvCipher(String mediaIvCipher) { this.mediaIvCipher = mediaIvCipher; }
}
