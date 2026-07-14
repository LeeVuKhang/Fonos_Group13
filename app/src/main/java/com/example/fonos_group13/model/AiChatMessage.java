package com.example.fonos_group13.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiChatMessage implements Serializable {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private final String role;
    private final String text;
    private final List<AiCitation> citations;

    public AiChatMessage(String role, String text) {
        this(role, text, Collections.emptyList());
    }

    public AiChatMessage(String role, String text, List<AiCitation> citations) {
        this.role = role;
        this.text = text;
        this.citations = citations == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(citations));
    }

    public String getRole() { return role; }
    public String getText() { return text; }
    public List<AiCitation> getCitations() { return citations; }
}
