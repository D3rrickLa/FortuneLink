package com.laderrco.fortunelink;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import io.micrometer.common.lang.NonNull;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor(force = true) // generate a no-args con even if thera are @NonNull fields, init them to default values
@Data
@Builder
@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private final UUID id;

    @NonNull
    private UUID userId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDateTime;
    private String type; // "BUY", "SELL". "DEPOSIT" - just a string
    private String ticker; // "AAPL"
    private BigDecimal quantity;
    private BigDecimal amount;
}
