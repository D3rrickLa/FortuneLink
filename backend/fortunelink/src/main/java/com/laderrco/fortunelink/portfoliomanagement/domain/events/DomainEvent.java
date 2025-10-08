package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

public interface DomainEvent {
    public Instant occuredOn();
    public String eventType();
    public String aggregateId();
}
