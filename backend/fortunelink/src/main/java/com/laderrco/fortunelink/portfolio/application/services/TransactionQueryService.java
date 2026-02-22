package com.laderrco.fortunelink.portfolio.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryService {
    private final TransactionService transactionService;

    
}
