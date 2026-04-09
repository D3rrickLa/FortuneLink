package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.UpdatePortfolioRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.responses.NetWorthResponse;
import com.laderrco.fortunelink.portfolio.api.web.dto.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio.api.web.dto.responses.PortfolioSummaryResponse;
import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.queries.GetNetWorthQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio.application.services.PortfolioLifecycleService;
import com.laderrco.fortunelink.portfolio.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios")
public class PortfolioController {
  private final PortfolioLifecycleService lifecycleService;
  private final PortfolioQueryService queryService;
  private final Authentication auth;

  // --- Portfolio Lifecycle ---

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PortfolioResponse createPortfolio(@AuthenticatedUser UserId userId,
      @RequestBody @Valid CreatePortfolioRequest request) {

    var command = new CreatePortfolioCommand(userId, request.name(), request.description(),
        Currency.of(request.currency()), request.createDefaultAccount(),
        request.defaultAccountType(), request.defaultStrategy());

    var view = lifecycleService.createPortfolio(command);

    return PortfolioResponse.fromView(view);
  }

  @PatchMapping("/{portfolioId}")
  public PortfolioResponse updatePortfolio(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @RequestBody @Valid UpdatePortfolioRequest request) {

    var command = new UpdatePortfolioCommand(PortfolioId.fromString(portfolioId), userId,
        request.name(), request.description(),
        request.currency() != null ? Currency.of(request.currency()) : null);

    var view = lifecycleService.updatePortfolio(command);
    return PortfolioResponse.fromView(view);
  }

  @DeleteMapping("/{portfolioId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePortfolio(@PathVariable String portfolioId, @AuthenticatedUser UserId userId,
      @RequestParam(defaultValue = "true") boolean softDelete,
      @RequestParam(defaultValue = "false") boolean recursive) {

    // Force soft-delete unless user is an ADMIN
    boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    boolean finalSoftDelete = isAdmin ? softDelete : true;

    // Allow 'recursive' regardless—it just means "mark all children as deleted too"
    lifecycleService.deletePortfolio(new DeletePortfolioCommand(PortfolioId.fromString(portfolioId),
            userId,finalSoftDelete,recursive));
  }

  // --- Portfolio Queries ---

  @GetMapping
  public List<PortfolioSummaryResponse> getPortfolios(@AuthenticatedUser UserId userId) {
    var summaries = queryService.getPortfolioSummaries(new GetPortfoliosByUserIdQuery(userId));

    return summaries.stream().map(PortfolioSummaryResponse::fromView).toList();
  }

  @GetMapping("/{portfolioId}")
  public PortfolioResponse getPortfolio(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId) {
    var view = queryService.getPortfolioById(
        new GetPortfolioByIdQuery(PortfolioId.fromString(portfolioId), userId));

    return PortfolioResponse.fromView(view);
  }

  @GetMapping("/{portfolioId}/net-worth")
  public NetWorthResponse getNetWorth(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId) {
    var view = queryService.getNetWorth(
        new GetNetWorthQuery(PortfolioId.fromString(portfolioId), userId));

    return NetWorthResponse.fromView(view);
  }
}