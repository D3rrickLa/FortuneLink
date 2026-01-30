package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.laderrco.fortunelink.portfolio_management.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.RateLimitConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.CacheEvictionService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.MarketDataServiceImpl;
import com.laderrco.fortunelink.portfolio_management.infrastructure.test_env.TestSecurityConfig;

@WebMvcTest(CacheAdminController.class)
@Import({ TestSecurityConfig.class, RateLimitConfig.class })
@ActiveProfiles("test")
class CacheAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CacheManager cacheManager;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private MarketDataServiceImpl marketDataService;

    @MockitoBean
    private AuthenticationUserService authenticationUserService;

    @MockitoBean
    private CacheEvictionService evictionService;

    private UUID mockUserId;

    @BeforeEach
    void init() {
        // Reset the cacheManager so previous test stubs/counts are wiped
        reset(cacheManager);
        mockUserId = UUID.randomUUID();
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("GET /stats should return list of cache names")
    void getCacheStats_ShouldReturnNames() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(List.of("cache1", "cache2"));

        mockMvc.perform(get("/api/admin/cache/stats").with(
                jwt().jwt(j -> j.subject(mockUserId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cacheNames").isArray())
                .andExpect(jsonPath("$.cacheCount").value(2));
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("DELETE /{name} should clear existing cache")
    void clearCache_ShouldSucceedWhenExists() throws Exception {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("test-cache")).thenReturn(mockCache);

        mockMvc.perform(delete("/api/admin/cache/test-cache")
                .with(jwt().jwt(j -> j.subject(mockUserId.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .with(csrf())) // Prevents 403 Forbidden due to CSRF protection
                .andExpect(status().isOk());

        verify(mockCache, times(1)).clear();
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("DELETE /{name} should return 404 when cache missing")
    void clearCache_ShouldReturn404WhenMissing() throws Exception {
        when(cacheManager.getCache("unknown")).thenReturn(null);

        mockMvc.perform(delete("/api/admin/cache/unknown").with(
                jwt().jwt(j -> j.subject(mockUserId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("DELETE / should clear all caches")
    void clearAllCaches_ShouldIterateAll() throws Exception {
        // Use ONE mock for the Cache interface
        Cache mockCache = mock(Cache.class);

        // Stub the manager to return the same mock object for ANY cache name requested
        when(cacheManager.getCacheNames()).thenReturn(List.of("c1", "c2"));
        when(cacheManager.getCache(Objects.requireNonNull(anyString()))).thenReturn(mockCache);

        mockMvc.perform(delete("/api/admin/cache").with(
                jwt().jwt(j -> j.subject(mockUserId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clearedCaches").isArray());

        // Verify that .clear() was called TWICE on the mockCache
        // (once for c1 and once for c2)
        verify(mockCache, times(2)).clear();
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("DELETE /{name}/{key} should evict specific entry")
    void clearCacheEntry_ShouldEvictKey() throws Exception {
        Cache mockCache = mock(Cache.class);
        when(cacheManager.getCache("prices")).thenReturn(mockCache);

        mockMvc.perform(delete("/api/admin/cache/prices/AAPL").with(
                jwt().jwt(j -> j.subject(mockUserId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("AAPL"));

        verify(mockCache).evict("AAPL");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("DELETE /price/{symbol} should call eviction service")
    void evictPriceCache_ShouldUseEvictionService() throws Exception {
        mockMvc.perform(delete("/api/admin/cache/price/TSLA").with(
                jwt().jwt(j -> j.subject(mockUserId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("TSLA"));

        verify(evictionService, times(1)).evictPriceCache(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("POST /warm should call service for all symbols")
    void warmCache_ShouldCallMarketDataService() throws Exception {
        mockMvc.perform(post("/api/admin/cache/warm").with(
                jwt().jwt(j -> j.subject(mockUserId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cache warming completed"))
                .andExpect(jsonPath("$.symbolsWarmed").isNumber());

        // Verify at least one call was made (warm includes AAPL, MSFT, etc.)
        verify(marketDataService, atLeastOnce()).getCurrentPrice(any());
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("DELETE / should handle null caches in the loop")
    void clearAllCaches_ShouldHandleNullCacheEntries() throws Exception {
        // 1. Arrange: Manager returns a name, but for some reason can't retrieve the
        // cache object
        when(cacheManager.getCacheNames()).thenReturn(List.of("ghost-cache"));
        when(cacheManager.getCache("ghost-cache")).thenReturn(null);

        // 2. Act & Assert
        mockMvc.perform(delete("/api/admin/cache").with(
                jwt().jwt(j -> j.subject(mockUserId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()) // The loop handles null, so it still succeeds
                .andExpect(jsonPath("$.message").value("All caches cleared successfully"))
                .andExpect(jsonPath("$.clearedCaches[0]").value("ghost-cache"));

        // Verify that we didn't crash
        verify(cacheManager, times(1)).getCache("ghost-cache");
    }

    @SuppressWarnings("null")
    @Test
    @DisplayName("DELETE /{name}/{key} should return 404 if cache name does not exist")
    void clearCacheEntry_ShouldReturn4xxWhenCacheNotFound() throws Exception {
        // 1. Arrange: Mock the manager to return null for a specific cache name
        String invalidCache = "non-existent-cache";
        when(cacheManager.getCache(invalidCache)).thenReturn(null);

        // 2. Act & Assert
        mockMvc.perform(delete("/api/admin/cache/{name}/{key}", invalidCache, "AAPL").with(
                jwt().jwt(j -> j.subject(mockUserId.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound()); // Matches your cache == null check

        // Verify we never tried to evict anything because the cache was null
        verifyNoInteractions(evictionService);
    }
}