package com.laderrco.fortunelink.portfoliomanagment.domain.events.interfaces;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
