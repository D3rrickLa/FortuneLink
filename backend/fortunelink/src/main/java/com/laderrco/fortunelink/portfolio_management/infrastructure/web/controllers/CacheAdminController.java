package com.laderrco.fortunelink.portfolio_management.infrastructure.web.controllers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.SymbolIdentifier;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.CacheEvictionService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.MarketDataServiceImpl;

import lombok.RequiredArgsConstructor;

/**
 * Admin endpoints for cache management.
 * 
 * WARNING: These endpoints should be secured in production!
 * Consider adding @PreAuthorize("hasRole('ADMIN')") or similar.
 * 
 * Endpoints:
 * - GET  /api/admin/cache/stats       - View cache statistics
 * - DELETE /api/admin/cache/{name}    - Clear specific cache
 * - DELETE /api/admin/cache           - Clear all caches
 * - DELETE /api/admin/cache/{name}/{key} - Clear specific cache entry
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')") // TODO: Add security in Phase 4 (Authentication)
public class CacheAdminController {
    
    private final CacheManager cacheManager;
    private final MarketDataServiceImpl marketDataService;
    private final CacheEvictionService evictionService;

    
    /**
     * Get cache statistics.
     * 
     * GET /api/admin/cache/stats
     * 
     * Returns cache names and approximate sizes (if supported by cache implementation).
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        Collection<String> cacheNames = cacheManager.getCacheNames();
        
        stats.put("cacheNames", cacheNames);
        stats.put("cacheCount", cacheNames.size());
        
        // Note: Redis cache doesn't expose size easily
        // This would require custom implementation to track sizes
        stats.put("note", "Cache sizes not available in current implementation");
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Clear a specific cache by name.
     * 
     * DELETE /api/admin/cache/{name}
     * 
     * Example: DELETE /api/admin/cache/current-prices
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        var cache = cacheManager.getCache(Objects.requireNonNull(cacheName));
        
        if (cache == null) {
            return ResponseEntity.notFound().build();
        }
        
        cache.clear();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache cleared successfully");
        response.put("cacheName", cacheName);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Clear all caches.
     * 
     * DELETE /api/admin/cache
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        
        cacheNames.forEach(cacheName -> {
            var cache = cacheManager.getCache(Objects.requireNonNull(cacheName));
            if (cache != null) {
                cache.clear();
            }
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All caches cleared successfully");
        response.put("clearedCaches", cacheNames);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Clear a specific cache entry.
     * 
     * DELETE /api/admin/cache/{name}/{key}
     * 
     * Example: DELETE /api/admin/cache/current-prices/AAPL
     */
    @DeleteMapping("/{cacheName}/{key}")
    public ResponseEntity<Map<String, String>> clearCacheEntry(
            @PathVariable String cacheName,
            @PathVariable String key) {
        
        var cache = cacheManager.getCache(Objects.requireNonNull(cacheName));
        
        if (cache == null) {
            return ResponseEntity.notFound().build();
        }
        
        cache.evict(key);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache entry evicted successfully");
        response.put("cacheName", cacheName);
        response.put("key", key);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Evict price cache for specific symbol.
     * Convenience method that uses domain service.
     * 
     * DELETE /api/admin/cache/price/{symbol}
     */
    @DeleteMapping("/price/{symbol}")
    public ResponseEntity<Map<String, String>> evictPriceCache(@PathVariable String symbol) {
        AssetIdentifier assetId = new SymbolIdentifier(symbol);
        evictionService.evictPriceCache(assetId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Price cache evicted successfully");
        response.put("symbol", symbol);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Warm cache with popular symbols.
     * Pre-loads cache with commonly requested symbols.
     * 
     * POST /api/admin/cache/warm
     */
    @PostMapping("/warm")
    public ResponseEntity<Map<String, Object>> warmCache() {
        // List of popular symbols to pre-cache
        String[] popularSymbols = {
            "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA",
            "SPY", "QQQ", "VOO", "VTI", "BTC-USD"
        };
        
        int warmed = 0;
        for (String symbol : popularSymbols) {
            try {
                AssetIdentifier assetId = new SymbolIdentifier(symbol);
                marketDataService.getCurrentPrice(assetId);
                marketDataService.getAssetInfo(assetId);
                warmed++;
            } catch (Exception e) {
                // Continue on error
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cache warming completed");
        response.put("symbolsWarmed", warmed);
        response.put("total", popularSymbols.length);
        
        return ResponseEntity.ok(response);
    }
}