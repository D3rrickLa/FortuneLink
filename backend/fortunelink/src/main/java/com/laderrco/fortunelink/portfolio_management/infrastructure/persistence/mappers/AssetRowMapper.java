package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AssetRowMapper implements RowMapper<Asset> {
    private final ObjectMapper objectMapper;

    @Override
    @Nullable
    public Asset mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
        String type = rs.getString("identifier_type");
        String primaryId = rs.getString("primary_id");

        // 1. Rebuild the polymorphic Identifier
        AssetIdentifier identifier = switch (type) {
            case "MARKET" -> new MarketIdentifier(
                primaryId, 
                mapJson(rs.getString("secondar_ids")), 
                AssetType.valueOf(rs.getString("asset_type")), 
                rs.getString("name"), 
                rs.getString("unit_of_trade"), 
                mapJson(rs.getString("metadat"))
            );
            case "CASH" -> new CashIdentifier(
                primaryId, 
                ValidatedCurrency.of(primaryId) // We decided primary_id stores "USD"
            );
            case "CRYPTO" -> new CryptoIdentifier(
                primaryId,
                rs.getString("name"),
                AssetType.valueOf(rs.getString("asset_type")),
                rs.getString("unit_of_trade"),
                mapJson(rs.getString("metadata"))
            );
            default -> throw new IllegalStateException("Unknown asset type: " + type);
        };

        // 2. Call the Reconstitution method
        return Asset.reconstitute(
            rs.getObject("id", AssetId.class),
            identifier,
            rs.getString("cost_basis_currency"), // the asset's listed currency
            rs.getBigDecimal("quantity"),
            rs.getBigDecimal("cost_basis_amount"),
            rs.getString("cost_basis_currency"),
            rs.getTimestamp("acquired_date").toInstant(),
            rs.getTimestamp("last_system_interaction").toInstant(),
            rs.getInt("version")
        );
    }

    private Map<String, String> mapJson(String json) {
        // Use Jackson to turn the JSONB string back into a Map
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        }
        catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
