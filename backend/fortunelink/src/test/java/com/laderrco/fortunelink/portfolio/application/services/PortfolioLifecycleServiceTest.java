package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioDeletionException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioLifecycleService Unit Tests")
class PortfolioLifecycleServiceTest {

	private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
	private final UserId USER_ID = UserId.random();
	private final Currency USD = Currency.USD;

	@Mock
	private PortfolioRepository portfolioRepository;
	@Mock
	private PortfolioViewMapper portfolioViewMapper;
	@Mock
	private PortfolioLifecycleCommandValidator validator;
	@Mock
	private TransactionTemplate transactionTemplate;
	@Mock
	private PortfolioLoader portfolioLoader;

	@InjectMocks
	private PortfolioLifecycleService service;

	@BeforeEach
	void setUp() {
		// Validation passes by default
		lenient().when(validator.validate(any(CreatePortfolioCommand.class))).thenReturn(ValidationResult.success());
		lenient().when(validator.validate(any(UpdatePortfolioCommand.class))).thenReturn(ValidationResult.success());
		lenient().when(validator.validate(any(DeletePortfolioCommand.class))).thenReturn(ValidationResult.success());
	}

	@Nested
	@DisplayName("updatePortfolio Logic")
	class UpdatePortfolioTests {
		@Test
		@DisplayName("updatePortfolio: updates details and returns lightweight view")
		void updatesDetailsAndReturnsView() {
			// GIVEN
			UpdatePortfolioCommand command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "New Name", "New Desc",
					USD);
			Portfolio existingPortfolio = mock(Portfolio.class);
			PortfolioView expectedView = mock(PortfolioView.class);

			// Mock the transaction execution to run the lambda
			when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
				TransactionCallback<Portfolio> callback = invocation.getArgument(0);

				// Inside the lambda:
				when(portfolioLoader.loadUserPortfolioWithGraph(PORTFOLIO_ID, USER_ID)).thenReturn(existingPortfolio);
				when(portfolioRepository.save(existingPortfolio)).thenReturn(existingPortfolio);

				return callback.doInTransaction(null);
			});

			when(portfolioViewMapper.toNewPortfolioView(existingPortfolio)).thenReturn(expectedView);

			// WHEN
			PortfolioView result = service.updatePortfolio(command);

			// THEN
			verify(existingPortfolio).updateDetails("New Name", "New Desc");
			verify(existingPortfolio).updateDisplayCurrency(USD);
			verify(portfolioRepository).save(existingPortfolio);
			assertThat(result).isEqualTo(expectedView);
		}

		@Test
		@DisplayName("updatePortfolio: throws IllegalStateException when transaction returns null")
		void throwsExceptionOnNullTransactionResult() {
			// GIVEN
			when(transactionTemplate.execute(any())).thenReturn(null);
			UpdatePortfolioCommand command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "Name", "Desc", USD);

			// WHEN / THEN
			assertThatThrownBy(() -> service.updatePortfolio(command))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Transaction failed");
		}

		@Test
		@DisplayName("updatePortfolio: ensures validation occurs before transaction")
		void validatesBeforeTransaction() {
			// GIVEN
			UpdatePortfolioCommand command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "Name", "Desc", USD);
			when(validator.validate(command)).thenReturn(ValidationResult.failure("Invalid Name"));

			// WHEN / THEN
			assertThatThrownBy(() -> service.updatePortfolio(command))
					.isInstanceOf(InvalidCommandException.class);

			verifyNoInteractions(transactionTemplate);
		}
	}

	@Nested
	@DisplayName("deletePortfolio Branching logic")
	class DeletePortfolioTests {
		@Test
		@DisplayName("hard delete: calls delete directly on repository")
		void hardDeleteCallsRepository() {
			Portfolio portfolio = mock(Portfolio.class);
			when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(Optional.of(portfolio));

			DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, false, false, false);
			service.deletePortfolio(cmd);

			verify(portfolioRepository).delete(PORTFOLIO_ID);
			verify(portfolioRepository, never()).save(any());
		}

		@Test
		@DisplayName("soft delete: recursive delete fails if accounts have cash")
		void recursiveDeleteFailsWithCash() {
			Portfolio portfolio = mock(Portfolio.class);
			Account account = mock(Account.class);

			when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(Optional.of(portfolio));
			when(portfolio.getAccounts()).thenReturn(List.of(account));
			when(account.isActive()).thenReturn(true);
			when(account.getCashBalance()).thenReturn(new Money(BigDecimal.TEN, USD));

			DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true, true);

			assertThatThrownBy(() -> service.deletePortfolio(cmd))
					.isInstanceOf(PortfolioDeletionException.class)
					.hasMessageContaining("zero positions and zero cash balance");
		}
	}
}