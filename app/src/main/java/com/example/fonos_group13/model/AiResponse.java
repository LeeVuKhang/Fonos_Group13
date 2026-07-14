package com.example.fonos_group13.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiResponse {
    private final String answer;
    private final boolean notFound;
    private final AiScope scope;
    private final String contentVersion;
    private final List<AiCitation> citations;

    public AiResponse(String answer, boolean notFound, AiScope scope, String contentVersion, List<AiCitation> citations) {
        this.answer = answer;
        this.notFound = notFound;
        this.scope = scope;
        this.contentVersion = contentVersion;
        this.citations = citations == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(citations));
    }

    public String getAnswer() { return answer; }
    public boolean isNotFound() { return notFound; }
    public AiScope getScope() { return scope; }
    public String getContentVersion() { return contentVersion; }
    public List<AiCitation> getCitations() { return citations; }
}
