package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;

public interface Projector<P> {
    P project(List<Transaction> transactions);
}
