package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import java.math.BigDecimal;
import java.util.UUID;

public class AssetHolding {
    private UUID assetId;
    private UUID portfolioId; // ref to aggregate root ID
    private BigDecimal quantity;
    private Money money;


}
