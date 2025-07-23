package com.laderrco.fortunelink.portfoliomanagment.application.dtos;

import java.util.UUID;

public record LiabilityCreatedDto(
    UUID liabilityId,
    String description
) {
    
}
