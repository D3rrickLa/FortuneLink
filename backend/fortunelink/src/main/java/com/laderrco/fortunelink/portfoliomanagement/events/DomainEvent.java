package com.laderrco.fortunelink.portfoliomanagement.events;

import java.time.Instant;

public interface DomainEvent {
    public Instant occuredOn();
    public String eventType();
    public String aggregateId();
}
