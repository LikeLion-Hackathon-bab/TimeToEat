package com.example.timetoeat.global.common;

import com.example.timetoeat.global.util.DomainEvent;

public interface EventHandler<E extends DomainEvent>{
    boolean supports(DomainEvent event);
    void handle(E event);
}
