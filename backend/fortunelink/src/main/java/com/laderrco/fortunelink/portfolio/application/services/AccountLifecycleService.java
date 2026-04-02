package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.ReopenAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeClosedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeReopenedException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import com.laderrco.fortunelink.portfolio.application.validators.AccountLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// moving some of the account stuff from portfolio service to here for 
// beter maintainablilty
@Service
@Transactional
@RequiredArgsConstructor
public class AccountLifecycleService {
  private final PortfolioRepository portfolioRepository;
  private final PortfolioViewMapper portfolioViewMapper;
  private final AccountLifecycleCommandValidator validator;
  private final PortfolioLoader portfolioLoader;

  public AccountView createAccount(CreateAccountCommand command) {
    ValidationUtils.validate(command, validator::validate, "createAccount");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(command.portfolioId(),
        command.userId());
    Account account = portfolio.createAccount(command.accountName(), command.accountType(),
        command.baseCurrency(), command.strategy());

    portfolioRepository.save(portfolio);

    return portfolioViewMapper.toNewAccountView(account);
  }

  public void updateAccount(UpdateAccountCommand command) {
    ValidationUtils.validate(command, validator::validate, "updateAccount");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(command.portfolioId(),
        command.userId());
    portfolio.renameAccount(command.accountId(), command.accountName());

    portfolioRepository.save(portfolio);
  }

  public void reopenAccount(ReopenAccountCommand command) {
    ValidationUtils.validate(command, validator::validate, "reopenAccount");
    Portfolio portfolio = portfolioLoader.loadUserPortfolio(command.portfolioId(),
        command.userId());
    try {
      portfolio.reopenAccount(command.accountId());
      portfolioRepository.save(portfolio);
    } catch (IllegalStateException e) {
      throw new AccountCannotBeReopenedException("Cannot reopen account: " + e.getMessage());
    }
  }

  // always soft deletes
  public void deleteAccount(DeleteAccountCommand command) {
    ValidationUtils.validate(command, validator::validate, "deleteAccount");
    Portfolio portfolio = portfolioLoader.loadUserPortfolio(command.portfolioId(),
        command.userId());
    try {
      portfolio.closeAccount(command.accountId());
      portfolioRepository.save(portfolio);
    } catch (IllegalStateException e) {
      throw new AccountCannotBeClosedException("Cannot close account: " + e.getMessage());
    }
  }
}
