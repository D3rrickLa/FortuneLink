package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.util.List;

// clean way for your domain objects to publish events without knowing how they will be delivered - message bus or an in-memory event handler
public interface DomainEventPublisher {
    public void publish(Object event);
    public void publish(List<Object> events);
}
