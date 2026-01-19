# 3. Test cache (run twice)
Write-Host "--- Testing Cache Performance ---" -ForegroundColor Cyan
Write-Host "First Run (Should take longer):"
$time1 = Measure-Command { $resp1 = curl.exe -s http://localhost:8080/api/market-data/price/AAPL }
Write-Host "Time: $($time1.TotalMilliseconds) ms"

Write-Host "`nSecond Run (Should be near instant):"
$time2 = Measure-Command { $resp2 = curl.exe -s http://localhost:8080/api/market-data/price/AAPL }
Write-Host "Time: $($time2.TotalMilliseconds) ms"

# 4. Test rate limiting
Write-Host "`n--- Testing Rate Limiting (61 Requests) ---" -ForegroundColor Cyan
for ($i=1; $i -le 61; $i++) {
    $code = curl.exe -s -o $null -w "%{http_code}" http://localhost:8080/api/market-data/price/AAPL
    if ($code -eq "429") {
        # Fixed: Using ${i} to prevent the colon from breaking the variable reference
        Write-Host "Request ${i}: Status $code (Rate Limit Hit!)" -ForegroundColor Red
    } else {
        Write-Host "Request ${i}: Status $code"
    }
}

# 5. Check cache stats
Write-Host "`n--- Cache Stats ---" -ForegroundColor Cyan
curl.exe http://localhost:8080/api/admin/cache/stats

# 6. Check health
Write-Host "`n`n--- System Health ---" -ForegroundColor Cyan
curl.exe http://localhost:8080/actuator/health
Write-Host "`n"