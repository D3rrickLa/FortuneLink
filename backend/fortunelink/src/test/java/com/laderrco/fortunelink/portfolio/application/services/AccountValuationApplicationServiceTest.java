package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.repositories.AccountQueryRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountValuationApplicationServiceTest {

  @Mock
  private AccountQueryRepository repository;

  @Mock
  private MarketDataService marketDataService;

  @InjectMocks
  private AccountValuationApplicationService service;

//    @Test
//    void shouldComputeAccountValuation() {
//
//      Account account = TestFixtures.accountWithPositions();
//
//      when(repository.findByIdWithDetails(any(), any(), any()))
//          .thenReturn(Optional.of(account));
//
//      when(marketDataService.getBatchQuotes(any()))
//          .thenReturn(TestFixtures.quotes());
//
//      AccountValuationView result =
//          service.computeAccountValuation(
//              new GetAccountSummaryQuery(
//                  PortfolioId.fromString("p1"),
//                  UserId.fromString("u1"),
//                  AccountId.fromString("a1")
//              )
//          );
//
//      assertThat(result.totalValue()).isNotNull();
//      assertThat(result.currency().getCode()).isEqualTo("CAD");
//    }
}