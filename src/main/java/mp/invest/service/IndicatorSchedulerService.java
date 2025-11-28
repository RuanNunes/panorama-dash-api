package mp.invest.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mp.invest.config.ScraperConfig;
import mp.invest.dto.FinancialIndicatorDTO;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Scheduled job that periodically scrapes financial indicators and updates the cache.
 * Uses Quarkus Scheduler with configurable cron expression.
 */
@ApplicationScoped
public class IndicatorSchedulerService {

    private static final Logger LOG = Logger.getLogger(IndicatorSchedulerService.class);

    /**
     * Default symbols to scrape if none configured.
     * These are example symbols - adjust based on your needs.
     */
    private static final List<String> DEFAULT_SYMBOLS = List.of(
            "IBOV:INDEXBVMF",
            "USD/BRL",
            "EUR/BRL",
            "BTC/USD",
            "ETH/USD"
    );

    private final ScraperConfig config;
    private final IndicatorScraperService scraperService;
    private final IndicatorCacheService cacheService;

    @Inject
    public IndicatorSchedulerService(ScraperConfig config, 
                                      IndicatorScraperService scraperService,
                                      IndicatorCacheService cacheService) {
        this.config = config;
        this.scraperService = scraperService;
        this.cacheService = cacheService;
    }

    /**
     * Scheduled job that runs according to the configured cron expression.
     * Scrapes financial indicators and updates the cache.
     * 
     * <p>The schedule is configured via the {@code scraper.schedule} property.</p>
     */
    @Scheduled(cron = "${scraper.schedule:0 */5 * * * ?}", 
               concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void scrapeIndicators() {
        if (!config.enabled()) {
            LOG.debug("Scraper is disabled, skipping scheduled execution");
            return;
        }

        LOG.info("Starting scheduled indicator scraping");
        
        List<String> symbols = config.symbols().orElse(DEFAULT_SYMBOLS);
        
        try {
            List<FinancialIndicatorDTO> indicators = scraperService.scrapeIndicators(symbols);
            
            if (indicators.isEmpty()) {
                LOG.warn("No indicators scraped, loading sample data as fallback");
                indicators = scraperService.getSampleIndicators();
            }
            
            indicators.forEach(cacheService::put);
            LOG.infof("Successfully cached %d indicators", indicators.size());
            
        } catch (Exception e) {
            LOG.errorf(e, "Error during scheduled scraping: %s", e.getMessage());
            // Load sample data as fallback
            loadFallbackData();
        }
    }

    /**
     * Manually triggers a scrape operation.
     * Useful for testing or forcing an immediate refresh.
     */
    public void triggerScrape() {
        LOG.info("Manual scrape triggered");
        scrapeIndicators();
    }

    /**
     * Loads sample/fallback data into the cache.
     * Used when scraping fails or for testing.
     */
    public void loadFallbackData() {
        LOG.info("Loading fallback sample data");
        scraperService.getSampleIndicators().forEach(cacheService::put);
    }

    /**
     * Initializes the cache with sample data on startup if configured.
     * This ensures the API returns data immediately after startup.
     */
    @Scheduled(cron = "{scraper.startup-load:disabled}")
    void loadOnStartup() {
        LOG.info("Loading initial data on startup");
        loadFallbackData();
    }
}
