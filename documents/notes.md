# Project Issue Tracker

| ID | Severity | Description | Status |
|:---|:---|:---|:---|
| 1–3 | - | Original fixes | ✅ Fixed |
| 4 | **Critical** | `deleteAccount` leaks `IllegalStateException` → 500 error | ✅ Fixed |
| 5 | **High** | `RETURN_OF_CAPITAL` drops cash and ignores ACB reduction | 🟠 Open |
| 6 | **Medium** | `TRANSFER_IN` / `TRANSFER_OUT` unrecordable | 🟡 Open |
| 7 | **Medium** | `SPLIT` unrecordable | 🟡 Open |
| 8 | **Low** | `REINVESTED_CAPITAL_GAIN` misleading stub | ⚪ Open |
| 9 | **Critical** | `AuthenticationUserService` methods package-private (infra will not compile) | ✅ Fixed |
| 10 | **High** | `accountId=null` in `GetTransactionHistoryQuery` passes to repo unchecked | ✅ Fixed |
| 11 | **Medium** | `recordBuy` missing pre-account-date validation (inconsistent with `recordSell`) | ✅ Fixed |
| 12 | **Low** | `verifyUserOwnsPortfolio` wrong exception type/message (401 vs 403) | ✅ Fixed |
| 13 | **High** | `counByUserId` counts soft-deleted portfolios | ✅ Fixed |
| 14 | **Medium** | `TransactionPurgeService` silently destroy a user's ability to restore an excluded transaction | ✅ Fixed |
| 15 | **Medium** | `updatePortfolio` allows mutation of soft0deleted portfolio | ✅ Fixed |
| 16 | **Medium** | `RecordWithdrawalCommand.fees` slient drops fees | ✅ Fixed |
| 17 | **High** |  `TransactionService.getPortfolioContext()` missing isDelete() | ✅ Fixed |  
| 18 | **High** | `TransactionApplier.apply()` Return_Of_Captial fall through | ✅ Fixed |  
| 19 | **Medium** | `TransactionRecordingServiceImpl` TransferIn/OUt should throw | ✅ Fixed |
| 20 | **High** | `FifoPositionProjector` unchecked cast | ✅ Fixed | 
| 21 | **Medium** | `AcbPosition.applyReturnOfCapital()` no quantity guard | ✅ Fixed| 
| 22 | **Low** | `recordInterest/recordDividend` null symbol |✅ Fixed |
| 23| **Low** | `applyPositionEffect` ensurePosition on SELL |✅ Fixed | 
| 24| **Critica** | `lDIVIDEND_REINVEST` silently no-ops in `applyPositionEffect` switch |🔴 Open |
| 25 |**Medium** | getPortfolioSummaries may return soft-deleted portfolios| 🟡 Open |
| 26| **Low** | Sort field `"transactionDate"` magic string| 🟡 Open |
|6, 7, 8 |— |Transfer, Split recording, REINVESTED_CAPITAL_GAIN |⚪ Deferred|

—Design GapPerformanceCalculationService not implemented⚪ Post-MVP—Design 
GapAssetAllocationService not implemented⚪ Post-MVP—Design GapLiability/Loan context not started⚪ Post-MVP—LowAcbPosition.firstAcquiredAt not surfaced in mapper⚪ Open

---
NOTE ABOUT AI AND UNIT TESTING: They can test trival things, but for more exploratory issues - IRL
We still need the human 'touch' to ifnd and debug