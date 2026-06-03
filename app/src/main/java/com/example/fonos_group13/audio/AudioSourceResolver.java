package com.example.fonos_group13.audio;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.example.fonos_group13.data.DownloadedAudioRepository;
import com.example.fonos_group13.model.Book;

public class AudioSourceResolver {
    private final Context context;
    private final DownloadedAudioRepository downloadedAudioRepository;

    public AudioSourceResolver(Context context) {
        this.context = context.getApplicationContext();
        downloadedAudioRepository = new DownloadedAudioRepository(this.context);
    }

    public Uri resolve(Book book) {
        if (book == null) {
            return null;
        }
        Uri downloadedUri = downloadedAudioRepository.getDownloadedUri(book.getId());
        if (downloadedUri != null) {
            return downloadedUri;
        }

        String audioUrl = trimToNull(book.getAudioUrl());
        if (audioUrl != null) {
            return Uri.parse(audioUrl);
        }

        int rawId = getRawResourceId(trimToNull(book.getAudioLocalResName()));
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

    private String trimToNull(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
