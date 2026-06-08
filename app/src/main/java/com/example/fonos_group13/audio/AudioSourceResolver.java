package com.example.fonos_group13.audio;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.example.fonos_group13.data.DownloadedAudioRepository;
import com.example.fonos_group13.model.Book;

public class AudioSourceResolver {
    private final DownloadedAudioRepository downloadedAudioRepository;

    public AudioSourceResolver(Context context) {
        downloadedAudioRepository = new DownloadedAudioRepository(context.getApplicationContext());
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

        return null;
    }

    private String trimToNull(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
