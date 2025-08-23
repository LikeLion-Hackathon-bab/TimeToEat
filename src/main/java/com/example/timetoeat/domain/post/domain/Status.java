package com.example.timetoeat.domain.post.domain;

public enum Status {
    OPEN, CLOSED, COMPLETED;

    public boolean canApply() {
        return this == OPEN;
    }
}
