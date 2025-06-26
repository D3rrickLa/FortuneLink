package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.UUID;

public class AssetTransferDetails extends TransactionDetails{
    private final UUID sourceAccountId;
    private final UUID destinationAccountId;
    
    public AssetTransferDetails(UUID sourceAccountId, UUID destinationAccountId) {
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
    }

    
}
