package com.example.fonos_group13.audio;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.example.fonos_group13.model.Book;

public class AudioSourceResolver {
    private final Context context;

    public AudioSourceResolver(Context context) {
        this.context = context.getApplicationContext();
    }

    public Uri resolve(Book book) {
        if (book == null) {
            return null;
        }
        if (!TextUtils.isEmpty(book.getAudioUrl())) {
            return Uri.parse(book.getAudioUrl());
        }

        int rawId = getRawResourceId(book.getAudioLocalResName());
        if (rawId == 0) {
            return null;
        }
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + rawId);
    }

    public int getRawResourceId(String rawName) {
        if (TextUtils.isEmpty(rawName)) {
            return 0;
        }
        return context.getResources().getIdentifier(rawName, "raw", context.getPackageName());
    }
}
