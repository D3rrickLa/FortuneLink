package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.*;
import com.laderrco.fortunelink.portfolio.application.exceptions.*;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency.CAD;
import static com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency.USD;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioLifecycleServiceTest {
    private final UserId userId = UserId.random();
    private final PortfolioId portfolioId = PortfolioId.newId();
    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private PortfolioViewMapper portfolioViewMapper;
    @Mock
    private MarketDataService marketDataService;
    @Mock
    private PortfolioValuationService portfolioValuationService;
    @Mock
    private PortfolioLifecycleCommandValidator validator;
    @Mock
    private AccountViewBuilder accountViewBuilder;
    @InjectMocks
    private PortfolioLifecycleService service;


    @Nested
    @DisplayName("Create Portfolio Tests")
    class CreatePortfolioTests {

        @BeforeEach
        void setup() {
            // Default behavior for validator to avoid stubbing in every success test
            lenient().when(validator.validate((CreatePortfolioCommand) any())).thenReturn(ValidationResult.success());
        }

        @Test
        @DisplayName("createPortfolio_Success_ValidCommand")
        void createPortfolio_Success_NewPortfolioWithDefaultAccount() {
            var command = new CreatePortfolioCommand(userId, "My Wealth", "Desc", USD, true, PositionStrategy.FIFO);
            when(portfolioRepository.countByUserId(userId)).thenReturn(0L);
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArgument(0));

            service.createPortfolio(command);

            verify(portfolioRepository).save(any(Portfolio.class));
            verify(portfolioViewMapper).toNewPortfolioView(any());
        }

        @Test
        @DisplayName("createPortfolio_Success_ValidCommand")
        void createPortfolio_Success_NewPortfolioWithoutDefaultAccount() {
            var command = new CreatePortfolioCommand(userId, "My Wealth", "Desc", USD, false, PositionStrategy.FIFO);
            when(portfolioRepository.countByUserId(userId)).thenReturn(0L);
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArgument(0));

            service.createPortfolio(command);

            verify(portfolioRepository).save(any(Portfolio.class));
            verify(portfolioViewMapper).toNewPortfolioView(any());
        }

        @Test
        @DisplayName("createPortfolio_Failure_ValidationFails")
        void createPortfolio_Failure_ValidationFails() {
            var command = new CreatePortfolioCommand(userId, "", "Desc", USD, false, null);
            // Mock the specific validation failure
            when(validator.validate(command)).thenReturn(ValidationResult.failure(List.of("Portfolio name is required")));

            assertThatThrownBy(() -> service.createPortfolio(command))
                    .isInstanceOf(InvalidCommandException.class)
                    .hasMessageContaining("Invalid createPortfolio command");
        }

        @Test
        @DisplayName("createPortfolio_Failure_LimitReached")
        void createPortfolio_Failure_WhenUserAlreadyHasPortfolio() {
            var command = new CreatePortfolioCommand(userId, "My Wealth", "Desc", USD, false, null);
            when(portfolioRepository.countByUserId(userId)).thenReturn(1L);

            assertThatThrownBy(() -> service.createPortfolio(command))
                    .isInstanceOf(PortfolioLimitReachedException.class);
        }

        @Test
        @DisplayName("createPortfolio_Failure_ValidationFailed")
        void createPortfolio_Failure_InvalidCommand() {
            var command = new CreatePortfolioCommand(userId, "", null, USD, false, null);
            when(validator.validate(command)).thenReturn(ValidationResult.failure(Collections.singletonList("Name required")));

            assertThatThrownBy(() -> service.createPortfolio(command))
                    .isInstanceOf(InvalidCommandException.class);
        }
    }

    @Nested
    @DisplayName("Update Portfolio Tests")
    class UpdatePortfolioTests {

        @BeforeEach
        void setup() {
            // Default behavior for validator to avoid stubbing in every success test
            lenient().when(validator.validate((UpdatePortfolioCommand) any())).thenReturn(ValidationResult.success());
        }

        @Test
        @DisplayName("updatePortfolio_Success_UpdatesAllFields")
        void updatePortfolio_Success_ValidIdAndCommand() {
            var command = new UpdatePortfolioCommand(portfolioId, userId, "New Name", "New Desc", CAD);
            Portfolio portfolio = mock(Portfolio.class);
            when(validator.validate(any(UpdatePortfolioCommand.class))).thenReturn(ValidationResult.success());
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(portfolio)).thenReturn(portfolio);

            service.updatePortfolio(command);

            verify(portfolio).updateDetails("New Name", "New Desc");
            verify(portfolio).updateDisplayCurrency(CAD);
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("updatePortfolio_Failure_NotFound")
        void updatePortfolio_Failure_PortfolioDoesNotExist() {
            var command = new UpdatePortfolioCommand(portfolioId, userId, "Name", "Desc", USD);
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePortfolio(command))
                    .isInstanceOf(PortfolioNotFoundException.class);
        }

        @Test
        @DisplayName("updatePortfolio_Failure_NotFound")
        void updatePortfolio_Failure_WhenPortfolioDoesNotExist() {
            var command = new UpdatePortfolioCommand(portfolioId, userId, "Name", "Desc", USD);
            // Ensure the repository returns empty to trigger the orElseThrow in getPortfolio()
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.empty());
            when(validator.validate(any(UpdatePortfolioCommand.class))).thenReturn(ValidationResult.success());

            assertThatThrownBy(() -> service.updatePortfolio(command))
                    .isInstanceOf(PortfolioNotFoundException.class)
                    .hasMessageContaining("Portfolio not found or access denied");
        }
    }

    @Nested
    @DisplayName("Delete Portfolio Tests")
    class DeletePortfolioTests {

        @BeforeEach
        void setup() {
            // Default behavior for validator to avoid stubbing in every success test
            lenient().when(validator.validate((DeletePortfolioCommand) any())).thenReturn(ValidationResult.success());
        }

        @Test
        @DisplayName("deletePortfolio_Success_HardDelete")
        void deletePortfolio_Success_HardDeleteWhenConfirmed() {
            var command = new DeletePortfolioCommand(portfolioId, userId, true, false);

            Portfolio portfolio = mock(Portfolio.class);
            when(portfolioRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(portfolio));
            service.deletePortfolio(command);

            verify(portfolioRepository).delete(portfolioId);
            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("deletePortfolio_Success_SoftDelete")
        void deletePortfolio_Success_SoftDeleteWhenConfirmed() {
            var command = new DeletePortfolioCommand(portfolioId, userId, true, true);
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));

            service.deletePortfolio(command);

            verify(portfolio).markAsDeleted(userId);
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("deletePortfolio_Failure_NoConfirmation")
        void deletePortfolio_Failure_ThrowsWhenNotConfirmed() {
            var command = new DeletePortfolioCommand(portfolioId, userId, false, true);

            assertThatThrownBy(() -> service.deletePortfolio(command))
                    .isInstanceOf(PortfolioDeletionRequiresConfirmationException.class);
        }

        @Test
        @DisplayName("deletePortfolio_Failure_PortfolioAlreadyDeleted")
        void deletePortfolio_Failure_WhenAlreadyDeleted() {
            var command = new DeletePortfolioCommand(portfolioId, userId, true, true);
            Portfolio portfolio = mock(Portfolio.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));

            // Correct syntax for void methods
            doThrow(new PortfolioAlreadyDeletedException("Already gone"))
                    .when(portfolio).markAsDeleted(userId);

            assertThatThrownBy(() -> service.deletePortfolio(command))
                    .isInstanceOf(PortfolioDeletionException.class)
                    .hasMessageContaining("Portfolio already deleted");
        }

        @Test
        @DisplayName("deletePortfolio_Failure_PortfolioNotEmpty")
        void deletePortfolio_Failure_WhenPortfolioIsNotEmpty() {
            var command = new DeletePortfolioCommand(portfolioId, userId, true, true);
            Portfolio portfolio = mock(Portfolio.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));

            doThrow(new PortfolioNotEmptyException("Cannot delete non-empty portfolio"))
                    .when(portfolio).markAsDeleted(userId);

            assertThatThrownBy(() -> service.deletePortfolio(command))
                    .isInstanceOf(PortfolioNotEmptyException.class)
                    .hasMessageContaining("Cannot delete non-empty portfolio");
        }

        @Test
        @DisplayName("deletePortfolio_Failure_UnexpectedIllegalState")
        void deletePortfolio_Failure_WhenIllegalStateOccurs() {
            var command = new DeletePortfolioCommand(portfolioId, userId, true, true);
            Portfolio portfolio = mock(Portfolio.class);

            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));

            // Covers the "Catch-all" branch
            doThrow(new IllegalStateException("Unknown state conflict"))
                    .when(portfolio).markAsDeleted(userId);

            assertThatThrownBy(() -> service.deletePortfolio(command))
                    .isInstanceOf(PortfolioDeletionException.class)
                    .hasMessageContaining("Cannot delete portfolio: Unknown state conflict");
        }
    }

    @Nested
    @DisplayName("Account Management Tests")
    class AccountManagementTests {

        @Test
        @DisplayName("createAccount_Success_AddsToPortfolio")
        void createAccount_Success_ValidCommand() {
            lenient().when(validator.validate((CreateAccountCommand) any())).thenReturn(ValidationResult.success());
            var command = new CreateAccountCommand(portfolioId, userId, "Savings", null, null, USD);
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));

            service.createAccount(command);

            verify(portfolio).createAccount(any(), any(), any(), any());
            verify(portfolioRepository).save(portfolio);
        }

        @Test
        @DisplayName("updateAccount_Success_UpdatesAccountInfo")
        void updateAccount_Success_Updates() {
            lenient().when(validator.validate((UpdateAccountCommand) any())).thenReturn(ValidationResult.success());
            var accountId = AccountId.newId();

            var command = new UpdateAccountCommand(portfolioId, userId, accountId, "new name");
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));

            service.updateAccount(command);

            verify(portfolio).renameAccount(any(), anyString());
        }

        @Test
        @DisplayName("deleteAccount_Success_SoftDeletes")
        void deleteAccount_Success_ValidIds() {
            lenient().when(validator.validate((DeleteAccountCommand) any())).thenReturn(ValidationResult.success());
            var accountId = AccountId.newId();
            var command = new DeleteAccountCommand(portfolioId, userId, accountId);
            Portfolio portfolio = mock(Portfolio.class);
            when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(Optional.of(portfolio));

            service.deleteAccount(command);

            verify(portfolio).closeAccount(accountId);
            verify(portfolioRepository).save(portfolio);
        }
    }
}