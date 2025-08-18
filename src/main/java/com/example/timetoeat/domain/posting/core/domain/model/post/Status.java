package com.example.timetoeat.domain.posting.core.domain.model.post;

public enum Status {
    OPEN, CLOSED, COMPLETED;

    public boolean canApply() {
        return this == OPEN;
    }
}
