package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects;

import java.util.ArrayList;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

// a derived VO through processing all transaction for a given symbol
// if you sold/held this asset today, what is my quantity, cost basis, and unrealized gain?
public record Position(AssetSymbol assetSymbol, AssetType type, List<TaxLot> lots) {}