package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.ExchangeRateUnavailableException;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateProvider;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ExchangeRateMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ProviderExchangeRate;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

    @Mock
    private ExchangeRateMapper mapper;

    @Mock
    private ExchangeRateProvider provider;

    @InjectMocks
    private ExchangeRateServiceImpl service;

    private ValidatedCurrency usd;
    private ValidatedCurrency cad;
    private Instant now;

    @BeforeEach
    void setUp() {
        usd = ValidatedCurrency.of("USD");
        cad = ValidatedCurrency.of("CAD");
        now = Instant.now();
    }

    @Test
    void getExchangeRate_ShouldReturnMappedRate_WhenCurrenciesAreDifferent() {
        // Arrange
        ProviderExchangeRate mockProviderRate = mock(ProviderExchangeRate.class);
        ExchangeRate mockExchangeRate = mock(ExchangeRate.class);

        when(provider.getExchangeRate(usd, cad, null)).thenReturn(mockProviderRate);
        when(mapper.toExchangeRate(mockProviderRate)).thenReturn(mockExchangeRate);

        // Act
        Optional<ExchangeRate> result = service.getExchangeRate(usd, cad);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(mockExchangeRate, result.get());
        verify(provider).getExchangeRate(usd, cad, null);
    }

    @Test
    void getExchangeRate_ShouldReturnOneToONe_WhenCurrenciesAreSame() {
        // Act & Assert
        Optional<ExchangeRate> rate = service.getExchangeRate(usd, usd);

        assertEquals(rate.get().from(), usd);
        assertEquals(rate.get().to(), usd);


    }

    @Test
    void convert_ShouldReturnConvertedAmount_WhenCurrenciesAreDifferent() {
        // Arrange
        Money inputAmount = new Money(new BigDecimal("100"), usd);
        ProviderExchangeRate mockRate = mock(ProviderExchangeRate.class);
        Money expectedMoney = new Money(new BigDecimal("135"), cad);

        when(provider.getExchangeRate(usd, cad, now)).thenReturn(mockRate);
        when(mapper.toMoney(inputAmount, mockRate)).thenReturn(expectedMoney);

        // Act
        Money result = service.convert(inputAmount, cad, now);

        // Assert
        assertEquals(expectedMoney, result);
        verify(provider).getExchangeRate(usd, cad, now);
    }

    @Test
    void convert_ShouldReturnConvertedAmount_WhenCurrenciesAreDifferentAndLatest() {
        // Arrange
        Money inputAmount = new Money(new BigDecimal("100"), usd);
        ProviderExchangeRate mockRate = mock(ProviderExchangeRate.class);
        Money expectedMoney = new Money(new BigDecimal("135"), cad);

        when(provider.getExchangeRate(usd, cad, null)).thenReturn(mockRate);
        when(mapper.toMoney(inputAmount, mockRate)).thenReturn(expectedMoney);

        // Act
        Money result = service.convert(inputAmount, cad);

        // Assert
        assertEquals(expectedMoney, result);
        verify(provider).getExchangeRate(usd, cad, null);
    }

    @Test
    void convert_ShouldReturnOriginalAmount_WithoutCallingProvider_WhenSameCurrency() {
        // Arrange
        Money amount = new Money(new BigDecimal("100"), usd);

        // Act
        Money result = service.convert(amount, usd, now);

        // Assert
        assertEquals(amount, result);
        verifyNoInteractions(provider);
    }

    @Test
    void convert_ShouldBubbleUpException_WhenProviderFails() {
        // Arrange
        Money amount = new Money(new BigDecimal("100"), usd);
        when(provider.getExchangeRate(any(), any(), any()))
                .thenThrow(new ExchangeRateUnavailableException("BOC Down"));

        // Act & Assert
        assertThrows(ExchangeRateUnavailableException.class, () -> {
            service.convert(amount, cad, now);
        });
    }
}