package com.laderrco.fortunelink.portfolio_management.application.responses;

import java.time.Instant;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

// TODO: add stuff to the response class. will need ot find out what
public record AssetResponse(AssetId assetId, UserId userId, Instant createDate, Instant lastUpdated) {
    
}
