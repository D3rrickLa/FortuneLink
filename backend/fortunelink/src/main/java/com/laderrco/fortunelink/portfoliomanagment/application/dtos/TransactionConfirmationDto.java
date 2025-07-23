package com.laderrco.fortunelink.portfoliomanagment.application.dtos;

import java.util.UUID;

public record TransactionConfirmationDto(
    UUID transactionId,
    String message
) {
    
}
