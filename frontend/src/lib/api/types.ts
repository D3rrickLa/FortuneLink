/**
 * Convenience re-exports of all schema types used across the app.
 * Import from here rather than directly from schema.d.ts to keep
 * imports short and to have one place to rename/augment if needed.
 */
import type { components } from "./schema";

// ─── Primitives ──────────────────────────────────────────────────────────────
export type Money           = components["schemas"]["Money"];
export type MoneyResponse   = components["schemas"]["MoneyResponse"];
export type Price           = components["schemas"]["Price"];
export type Quantity        = components["schemas"]["Quantity"];
export type Currency        = components["schemas"]["Currency"];
export type ExchangeRate    = components["schemas"]["ExchangeRate"];
export type Fee             = components["schemas"]["Fee"];
export type FeeRequest      = components["schemas"]["FeeRequest"];
export type PercentageChange = components["schemas"]["PercentageChange"];

// ─── Portfolio ────────────────────────────────────────────────────────────────
export type PortfolioSummaryResponse = components["schemas"]["PortfolioSummaryResponse"];
export type PortfolioResponse        = components["schemas"]["PortfolioResponse"];
export type AccountSummary           = components["schemas"]["AccountSummary"];
export type CreatePortfolioRequest   = components["schemas"]["CreatePortfolioRequest"];
export type UpdatePortfolioRequest   = components["schemas"]["UpdatePortfolioRequest"];

// ─── Account ──────────────────────────────────────────────────────────────────
export type AccountView          = components["schemas"]["AccountView"];
export type CreateAccountRequest = components["schemas"]["CreateAccountRequest"];
export type UpdateAccountRequest = components["schemas"]["UpdateAccountRequest"];
export type PositionView         = components["schemas"]["PositionView"];

export type AccountType  = NonNullable<CreateAccountRequest["accountType"]>;
export type AccountStatus = NonNullable<AccountView["status"]>;
export type CostBasisStrategy = NonNullable<CreateAccountRequest["strategy"]>;

// ─── Transactions ─────────────────────────────────────────────────────────────
export type TransactionView           = components["schemas"]["TransactionView"];
export type TransactionType           = NonNullable<TransactionView["type"]>;
export type PageTransactionView       = components["schemas"]["PageTransactionView"];
export type CsvImportResult           = components["schemas"]["CsvImportResult"];
export type CsvRowError               = components["schemas"]["CsvRowError"];
export type ExcludeTransactionRequest = components["schemas"]["ExcludeTransactionRequest"];

// Transaction request bodies
export type RecordPurchaseRequest        = components["schemas"]["RecordPurchaseRequest"];
export type RecordSaleRequest            = components["schemas"]["RecordSaleRequest"];
export type RecordDepositRequest         = components["schemas"]["RecordDepositRequest"];
export type RecordWithdrawalRequest      = components["schemas"]["RecordWithdrawalRequest"];
export type RecordDividendRequest        = components["schemas"]["RecordDividendRequest"];
export type RecordDRIPRequest            = components["schemas"]["RecordDRIPRequest"];
export type RecordInterestRequest        = components["schemas"]["RecordInterestRequest"];
export type RecordStandaloneFeeRequest   = components["schemas"]["RecordStandaloneFeeRequest"];
export type RecordTransferInRequest      = components["schemas"]["RecordTransferInRequest"];
export type RecordTransferOutRequest     = components["schemas"]["RecordTransferOutRequest"];
export type RecordSplitRequest           = components["schemas"]["RecordSplitRequest"];
export type RecordReturnOfCapitalRequest = components["schemas"]["RecordReturnOfCapitalRequest"];

// Asset type enum (shared across buy and positions)
export type AssetType = NonNullable<PositionView["assetType"]>;

// ─── Realized Gains ───────────────────────────────────────────────────────────
export type RealizedGainsSummaryResponse = components["schemas"]["RealizedGainsSummaryResponse"];
export type RealizedGainItemResponse     = components["schemas"]["RealizedGainItemResponse"];

// ─── Net Worth ────────────────────────────────────────────────────────────────
export type NetWorthResponse         = components["schemas"]["NetWorthResponse"];
export type NetWorthSnapshotResponse = components["schemas"]["NetWorthSnapshotResponse"];

// ─── Market ───────────────────────────────────────────────────────────────────
export type MarketQuoteResponse   = components["schemas"]["MarketQuoteResponse"];
export type AssetInfoResponse     = components["schemas"]["AssetInfoResponse"];
export type SymbolSearchResponse  = components["schemas"]["SymbolSearchResponse"];
export type BatchQuoteRequest     = components["schemas"]["BatchQuoteRequest"];
export type BatchQuoteResponse    = Record<string, MarketQuoteResponse>;

// ─── Exchange Rates ───────────────────────────────────────────────────────────
export type ExchangeRateResponse      = components["schemas"]["ExchangeRateResponse"];
export type SupportedCurrenciesResponse = components["schemas"]["SupportedCurrenciesResponse"];

// ─── Pagination helpers ───────────────────────────────────────────────────────
export type PageAccountView   = components["schemas"]["PageAccountView"];
export type PageableObject    = components["schemas"]["PageableObject"];
export type SortObject        = components["schemas"]["SortObject"];

export interface PaginationParams {
  page?: number;
  size?: number;
}