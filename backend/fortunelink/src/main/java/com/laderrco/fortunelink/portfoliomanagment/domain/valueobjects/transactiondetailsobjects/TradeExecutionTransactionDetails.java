package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.time.Instant;
import java.util.List;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionStatus;

public final class TradeExecutionTransactionDetails extends TransactionDetails {

    protected TradeExecutionTransactionDetails(TransactionSource source, String description, List<Fee> fees) {
        super(source, description, fees);
        //TODO Auto-generated constructor stub
    }

    @Override
    public TransactionDetails updateStatus(TransactionStatus newStatus, Instant updatedAt) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateStatus'");
    }
    
    
}
