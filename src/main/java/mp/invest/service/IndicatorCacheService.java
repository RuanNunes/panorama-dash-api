package mp.invest.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mp.invest.config.ScraperConfig;
import mp.invest.dto.FinancialIndicatorDTO;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory cache for financial indicators.
 * Uses ConcurrentHashMap for thread-safety and supports TTL-based expiration.
 */
@ApplicationScoped
public class IndicatorCacheService {

    private static final Logger LOG = Logger.getLogger(IndicatorCacheService.class);

    private final Map<String, CachedIndicator> cache = new ConcurrentHashMap<>();

    private final ScraperConfig config;

    @Inject
    public IndicatorCacheService(ScraperConfig config) {
        this.config = config;
    }

    @PostConstruct
    void init() {
        LOG.infof("Indicator cache initialized with TTL: %s", config.cacheTtl());
    }

    /**
     * Stores an indicator in the cache.
     *
     * @param indicator the indicator to cache
     */
    public void put(FinancialIndicatorDTO indicator) {
        if (indicator == null || indicator.symbol() == null) {
            LOG.warn("Attempted to cache null indicator or indicator with null symbol");
            return;
        }
        cache.put(indicator.symbol().toUpperCase(), new CachedIndicator(indicator, Instant.now()));
        LOG.debugf("Cached indicator: %s", indicator.symbol());
    }

    /**
     * Retrieves an indicator from the cache by symbol.
     * Returns empty if not found or expired.
     *
     * @param symbol the symbol to look up
     * @return Optional containing the indicator if found and not expired
     */
    public Optional<FinancialIndicatorDTO> get(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }

        CachedIndicator cached = cache.get(symbol.toUpperCase());
        if (cached == null) {
            return Optional.empty();
        }

        if (isExpired(cached)) {
            LOG.debugf("Cached indicator expired: %s", symbol);
            cache.remove(symbol.toUpperCase());
            return Optional.empty();
        }

        return Optional.of(cached.indicator());
    }

    /**
     * Returns all non-expired indicators from the cache.
     *
     * @return collection of all cached indicators
     */
    public Collection<FinancialIndicatorDTO> getAll() {
        evictExpired();
        return cache.values().stream()
                .map(CachedIndicator::indicator)
                .toList();
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        cache.clear();
        LOG.info("Cache cleared");
    }

    /**
     * Returns the current size of the cache (including potentially expired entries).
     *
     * @return number of entries in cache
     */
    public int size() {
        return cache.size();
    }

    /**
     * Evicts all expired entries from the cache.
     */
    public void evictExpired() {
        Instant now = Instant.now();
        Duration ttl = config.cacheTtl();
        cache.entrySet().removeIf(entry -> {
            boolean expired = Duration.between(entry.getValue().cachedAt(), now).compareTo(ttl) > 0;
            if (expired) {
                LOG.debugf("Evicting expired indicator: %s", entry.getKey());
            }
            return expired;
        });
    }

    private boolean isExpired(CachedIndicator cached) {
        return Duration.between(cached.cachedAt(), Instant.now()).compareTo(config.cacheTtl()) > 0;
    }

    /**
     * Internal record to track cache entry with timestamp.
     */
    private record CachedIndicator(FinancialIndicatorDTO indicator, Instant cachedAt) {}
}
