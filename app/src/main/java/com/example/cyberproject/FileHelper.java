package com.example.cyberproject;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class FileHelper {

    public static Uri writeTempFile(Context ctx, byte[] bytes, String mime) throws Exception {
        String ext = guessExt(mime);
        File outFile = new File(ctx.getCacheDir(), "dec_" + System.currentTimeMillis() + ext);

        FileOutputStream fos = new FileOutputStream(outFile);
        fos.write(bytes);
        fos.close();

        return FileProvider.getUriForFile(
                ctx,
                ctx.getPackageName() + ".provider",
                outFile
        );
    }

    private static String guessExt(String mime) {
        if (mime == null) return ".bin";
        if (mime.startsWith("image/")) return ".jpg";
        if (mime.startsWith("video/")) return ".mp4";
        if (mime.startsWith("audio/")) return ".mp3";
        return ".bin";
    }
}
