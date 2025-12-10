package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse.AllocationDetail;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// converts allocation calculations to response format - format percentgaes and categoreis
public class AllocationMapper {
    public AllocationResponse toResponse(Map<String, Money> allocation, Money totalValue) {
        return null;
    }

    public AllocationDetail toAllocationDetail(String category, Money value, Money total) {
        return null;
    }
}
