package com.example.timetoeat.domain.posting.domain.model;

public enum Status {
    OPEN, CLOSED, COMPLETED;

    public boolean canApply() {
        return this == OPEN;
    }
}
