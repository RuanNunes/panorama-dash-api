package mp.invest.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Configuration for the financial indicators scraping service.
 * All properties are configurable via application.yml or environment variables.
 */
@ConfigMapping(prefix = "scraper")
public interface ScraperConfig {

    /**
     * Cron expression or duration for scheduler interval.
     * Default: every 5 minutes
     */
    @WithName("schedule")
    @WithDefault("0 */5 * * * ?")
    String schedule();

    /**
     * Connection timeout for HTTP requests.
     */
    @WithName("connection-timeout")
    @WithDefault("10s")
    Duration connectionTimeout();

    /**
     * Read timeout for HTTP requests.
     */
    @WithName("read-timeout")
    @WithDefault("30s")
    Duration readTimeout();

    /**
     * User agent to use for HTTP requests.
     */
    @WithName("user-agent")
    @WithDefault("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    String userAgent();

    /**
     * Maximum number of retry attempts for failed requests.
     */
    @WithName("max-retries")
    @WithDefault("3")
    int maxRetries();

    /**
     * Initial backoff duration for exponential retry.
     */
    @WithName("retry-backoff")
    @WithDefault("1s")
    Duration retryBackoff();

    /**
     * Cache TTL - time to live for cached indicators.
     */
    @WithName("cache-ttl")
    @WithDefault("10m")
    Duration cacheTtl();

    /**
     * Enable or disable the scraper.
     */
    @WithName("enabled")
    @WithDefault("true")
    boolean enabled();

    /**
     * List of symbols to scrape (e.g., IBOV, USD-BRL, BTC-USD).
     */
    @WithName("symbols")
    Optional<List<String>> symbols();
}
