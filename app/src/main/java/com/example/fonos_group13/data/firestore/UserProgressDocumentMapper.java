package com.example.fonos_group13.data.firestore;

import com.example.fonos_group13.model.UserProgress;
import com.google.firebase.firestore.DocumentSnapshot;

public final class UserProgressDocumentMapper {
    private UserProgressDocumentMapper() {
    }

    public static UserProgress fromDocument(
            String bookId,
            String chapterId,
            DocumentSnapshot document
    ) {
        String storedChapterId = FirestoreValueReader.string(document, "chapterId");
        return new UserProgress(
                bookId,
                DocumentMapperSupport.optionalString(storedChapterId) == null ? chapterId : storedChapterId,
                FirestoreValueReader.longValue(document, "positionMs"),
                FirestoreValueReader.longValue(document, "durationMs"),
                FirestoreValueReader.booleanValue(document, "completed", false)
        );
    }
}
