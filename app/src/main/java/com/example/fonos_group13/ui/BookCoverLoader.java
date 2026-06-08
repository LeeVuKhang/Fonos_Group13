package com.example.fonos_group13.ui;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.fonos_group13.R;
import com.example.fonos_group13.model.Book;

public final class BookCoverLoader {
    private BookCoverLoader() {
    }

    public static void load(ImageView imageView, Book book) {
        if (imageView == null) {
            return;
        }

        if (book == null || TextUtils.isEmpty(book.getCoverUrl())) {
            Glide.with(imageView).clear(imageView);
            imageView.setImageResource(R.drawable.bg_cover_placeholder);
            imageView.setContentDescription(null);
            return;
        }

        imageView.setContentDescription(book.getTitle() + " cover");
        Glide.with(imageView)
                .load(book.getCoverUrl())
                .centerCrop()
                .placeholder(R.drawable.bg_cover_placeholder)
                .error(R.drawable.bg_cover_placeholder)
                .into(imageView);
    }

    public static ImageView findCoverView(View root, int preferredId) {
        if (root == null) {
            return null;
        }
        ImageView imageView = root.findViewById(preferredId);
        return imageView == null ? findFirstImageView(root) : imageView;
    }

    private static ImageView findFirstImageView(View view) {
        if (view instanceof ImageView) {
            return (ImageView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            ImageView childImageView = findFirstImageView(group.getChildAt(i));
            if (childImageView != null) {
                return childImageView;
            }
        }
        return null;
    }
}
