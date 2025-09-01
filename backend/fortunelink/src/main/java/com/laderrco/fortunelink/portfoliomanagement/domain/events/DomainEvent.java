package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

public interface DomainEvent {
    public Instant occuredAt();
}
