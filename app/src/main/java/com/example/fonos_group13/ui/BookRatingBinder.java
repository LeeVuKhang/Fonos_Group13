package com.example.fonos_group13.ui;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.fonos_group13.model.Book;

import java.util.ArrayList;
import java.util.List;

public final class BookRatingBinder {
    public static final String VIEW_TAG = "fonos_book_rating";

    private BookRatingBinder() {
    }

    public static void bind(View root, Book book) {
        if (root == null || book == null) return;
        TextView rating = root.findViewWithTag(VIEW_TAG);
        if (rating == null) {
            List<TextView> textViews = new ArrayList<>();
            collect(root, textViews);
            if (textViews.size() < 2 || !(textViews.get(1).getParent() instanceof LinearLayout)) return;
            LinearLayout parent = (LinearLayout) textViews.get(1).getParent();
            rating = new TextView(root.getContext());
            rating.setTag(VIEW_TAG);
            rating.setTextSize(12);
            rating.setTypeface(null, Typeface.BOLD);
            rating.setTextColor(textViews.get(1).getCurrentTextColor());
            int index = parent.indexOfChild(textViews.get(1));
            parent.addView(rating, Math.min(index + 1, parent.getChildCount()),
                    new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        rating.setText(BookRatingFormatter.format(book));
    }

    private static void collect(View view, List<TextView> output) {
        if (view instanceof TextView) {
            if (!VIEW_TAG.equals(view.getTag())) output.add((TextView) view);
            return;
        }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int index = 0; index < group.getChildCount(); index++) collect(group.getChildAt(index), output);
    }
}
