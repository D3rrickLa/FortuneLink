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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Portfolios", description = "Endpoints for managing investment portfolios and viewing net worth analytics.")
public class PortfolioController {
  private final PortfolioLifecycleService lifecycleService;
  private final PortfolioQueryService queryService;

  @Operation(summary = "Create a new portfolio", description = "Initializes a portfolio for the authenticated user. Can optionally create a default account automatically.")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PortfolioResponse createPortfolio(
      @Parameter(hidden = true) @AuthenticatedUser UserId userId,
      @RequestBody @Valid CreatePortfolioRequest request) {

    var command = new CreatePortfolioCommand(userId, request.name(), request.description(),
        Currency.of(request.currency()), request.createDefaultAccount(),
        request.defaultAccountType(), request.defaultStrategy());

    var view = lifecycleService.createPortfolio(command);
    return PortfolioResponse.fromView(view);
  }

  @Operation(summary = "Update portfolio details", description = "Updates the name, description, or base currency of an existing portfolio.")
  @PatchMapping("/{portfolioId}")
  public PortfolioResponse updatePortfolio(@PathVariable String portfolioId,
      @Parameter(hidden = true) @AuthenticatedUser UserId userId,
      @RequestBody @Valid UpdatePortfolioRequest request) {

    var command = new UpdatePortfolioCommand(PortfolioId.fromString(portfolioId), userId,
        request.name(), request.description(),
        request.currency() != null ? Currency.of(request.currency()) : null);

    var view = lifecycleService.updatePortfolio(command);
    return PortfolioResponse.fromView(view);
  }

  @Operation(summary = "Delete a portfolio", description =
      "Removes a portfolio. Standard users are forced to soft-delete. "
          + "Admins may toggle hard-delete. 'Recursive' will affect all child accounts.")
  @ApiResponses({@ApiResponse(responseCode = "204", description = "Portfolio successfully deleted"),
      @ApiResponse(responseCode = "403", description = "Unauthorized access to portfolio")})
  @DeleteMapping("/{portfolioId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePortfolio(@PathVariable String portfolioId,
      @Parameter(hidden = true) @AuthenticatedUser UserId userId,
      @Parameter(description = "If true, data is hidden but not wiped. Forced to true for non-admins.") @RequestParam(defaultValue = "true") boolean softDelete,
      @Parameter(description = "If true, deletes all associated accounts and transactions.") @RequestParam(defaultValue = "false") boolean recursive) {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean isAdmin = auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

    boolean finalSoftDelete = !isAdmin || softDelete;

    lifecycleService.deletePortfolio(
        new DeletePortfolioCommand(PortfolioId.fromString(portfolioId), userId, finalSoftDelete,
            recursive));
  }

  @Operation(summary = "List all portfolios", description = "Returns a summary list of all portfolios owned by the authenticated user.")
  @GetMapping
  public List<PortfolioSummaryResponse> getPortfolios(
      @Parameter(hidden = true) @AuthenticatedUser UserId userId) {

    var summaries = queryService.getPortfolioSummaries(new GetPortfoliosByUserIdQuery(userId));
    return summaries.stream().map(PortfolioSummaryResponse::fromView).toList();
  }

  @Operation(summary = "Get portfolio details", description = "Returns full details for a specific portfolio, including configuration and status.")
  @GetMapping("/{portfolioId}")
  public PortfolioResponse getPortfolio(@PathVariable String portfolioId,
      @Parameter(hidden = true) @AuthenticatedUser UserId userId) {

    var view = queryService.getPortfolioById(
        new GetPortfolioByIdQuery(PortfolioId.fromString(portfolioId), userId));

    return PortfolioResponse.fromView(view);
  }

  @Operation(summary = "Get portfolio net worth", description =
      "Calculates the total valuation of the portfolio in its base currency. "
          + "Triggers real-time valuation of all underlying assets.")
  @GetMapping("/{portfolioId}/net-worth")
  public NetWorthResponse getNetWorth(@PathVariable String portfolioId,
      @Parameter(hidden = true) @AuthenticatedUser UserId userId) {

    var view = queryService.getNetWorth(
        new GetNetWorthQuery(PortfolioId.fromString(portfolioId), userId));

    return NetWorthResponse.fromView(view);
  }
}