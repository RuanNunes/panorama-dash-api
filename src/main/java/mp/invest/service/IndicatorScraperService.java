package mp.invest.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import mp.invest.config.ScraperConfig;
import mp.invest.dto.FinancialIndicatorDTO;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for scraping financial indicators from web sources.
 * 
 * <h2>Compliance Warning</h2>
 * <p>
 * Web scraping carries legal and technical risks:
 * <ul>
 *   <li><b>Terms of Service:</b> Many financial websites prohibit scraping in their ToS. 
 *       Always review and comply with the target site's terms.</li>
 *   <li><b>Rate Limiting:</b> Excessive requests may result in IP blocking or legal action.</li>
 *   <li><b>Data Accuracy:</b> Scraped data may be delayed, incomplete, or incorrect.</li>
 *   <li><b>API Alternatives:</b> Consider using official APIs (Google Finance API, Yahoo Finance API, 
 *       Alpha Vantage, etc.) for production use.</li>
 * </ul>
 * </p>
 * 
 * <h2>Selector Warning</h2>
 * <p>
 * The CSS selectors used in this service are examples only and may change at any time.
 * Website structures are frequently updated, requiring selector maintenance.
 * Always validate selectors manually before relying on scraped data.
 * </p>
 */
@ApplicationScoped
public class IndicatorScraperService {

    private static final Logger LOG = Logger.getLogger(IndicatorScraperService.class);

    /**
     * Base URL for Google Finance. Note: Google's ToS may prohibit scraping.
     * This is provided as an example only.
     */
    private static final String GOOGLE_FINANCE_BASE_URL = "https://www.google.com/finance/quote/";

    /**
     * Example CSS selectors for Google Finance.
     * WARNING: These selectors are examples and may not work as website structure changes frequently.
     * You must validate and update these selectors manually.
     */
    private static final String SELECTOR_PRICE = "div[data-last-price]";
    private static final String SELECTOR_CHANGE = "div[data-price-change]";
    private static final String SELECTOR_CHANGE_PERCENT = "div[data-price-change-percent]";
    private static final String SELECTOR_NAME = "div[class*='zzDege']";

    private final ScraperConfig config;
    private final MeterRegistry meterRegistry;

    private Counter scrapeSuccessCounter;
    private Counter scrapeFailureCounter;

    @Inject
    public IndicatorScraperService(ScraperConfig config, MeterRegistry meterRegistry) {
        this.config = config;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void init() {
        scrapeSuccessCounter = Counter.builder("scraper.requests.success")
                .description("Number of successful scraping requests")
                .register(meterRegistry);
        scrapeFailureCounter = Counter.builder("scraper.requests.failure")
                .description("Number of failed scraping requests")
                .register(meterRegistry);
        LOG.info("Indicator scraper service initialized");
    }

    /**
     * Scrapes a financial indicator by symbol with retry and exponential backoff.
     *
     * @param symbol the symbol to scrape (e.g., "IBOV:INDEXBVMF", "USD-BRL", "BTC-USD")
     * @return Optional containing the scraped indicator, or empty if scraping failed
     */
    public Optional<FinancialIndicatorDTO> scrapeIndicator(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            LOG.warn("Attempted to scrape null or blank symbol");
            return Optional.empty();
        }

        int attempts = 0;
        Duration backoff = config.retryBackoff();

        while (attempts < config.maxRetries()) {
            attempts++;
            try {
                Optional<FinancialIndicatorDTO> result = doScrape(symbol);
                if (result.isPresent()) {
                    scrapeSuccessCounter.increment();
                    return result;
                }
            } catch (IOException e) {
                LOG.warnf("Scrape attempt %d/%d failed for symbol %s: %s", 
                          attempts, config.maxRetries(), symbol, e.getMessage());
                
                if (attempts < config.maxRetries()) {
                    try {
                        Thread.sleep(backoff.toMillis());
                        backoff = backoff.multipliedBy(2); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        scrapeFailureCounter.increment();
        LOG.errorf("Failed to scrape symbol %s after %d attempts", symbol, attempts);
        return Optional.empty();
    }

    /**
     * Scrapes multiple indicators.
     *
     * @param symbols list of symbols to scrape
     * @return list of successfully scraped indicators
     */
    public List<FinancialIndicatorDTO> scrapeIndicators(List<String> symbols) {
        List<FinancialIndicatorDTO> results = new ArrayList<>();
        for (String symbol : symbols) {
            scrapeIndicator(symbol).ifPresent(results::add);
            
            // Add delay between requests to avoid rate limiting
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return results;
    }

    /**
     * Performs the actual scraping using Jsoup.
     */
    private Optional<FinancialIndicatorDTO> doScrape(String symbol) throws IOException {
        String url = buildUrl(symbol);
        LOG.debugf("Scraping URL: %s", url);

        Document doc = Jsoup.connect(url)
                .userAgent(config.userAgent())
                .timeout((int) config.connectionTimeout().toMillis())
                .followRedirects(true)
                .get();

        return parseDocument(doc, symbol);
    }

    /**
     * Parses the HTML document to extract indicator data.
     * 
     * <p>WARNING: These selectors are examples and may need manual validation and updates.</p>
     */
    private Optional<FinancialIndicatorDTO> parseDocument(Document doc, String symbol) {
        try {
            // Example selector - may need adjustment based on actual page structure
            Element priceElement = doc.selectFirst(SELECTOR_PRICE);
            if (priceElement == null) {
                // Fallback: try to find any element with price-like data
                priceElement = doc.selectFirst("[data-value]");
            }

            BigDecimal value = null;
            if (priceElement != null) {
                String priceAttr = priceElement.attr("data-last-price");
                if (priceAttr.isEmpty()) {
                    priceAttr = priceElement.attr("data-value");
                }
                if (!priceAttr.isEmpty()) {
                    value = new BigDecimal(priceAttr);
                }
            }

            if (value == null) {
                LOG.debugf("Could not extract price for symbol: %s", symbol);
                return Optional.empty();
            }

            BigDecimal change = parseChangeValue(doc);
            BigDecimal changePercent = parseChangePercentValue(doc);
            String name = parseName(doc, symbol);
            String currency = detectCurrency(symbol);

            return Optional.of(FinancialIndicatorDTO.of(
                    symbol,
                    name,
                    value,
                    change,
                    changePercent,
                    currency,
                    "Google Finance"
            ));

        } catch (Exception e) {
            LOG.warnf("Failed to parse document for symbol %s: %s", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private BigDecimal parseChangeValue(Document doc) {
        Element changeElement = doc.selectFirst(SELECTOR_CHANGE);
        if (changeElement != null) {
            String changeAttr = changeElement.attr("data-price-change");
            if (!changeAttr.isEmpty()) {
                try {
                    return new BigDecimal(changeAttr);
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal parseChangePercentValue(Document doc) {
        Element changePercentElement = doc.selectFirst(SELECTOR_CHANGE_PERCENT);
        if (changePercentElement != null) {
            String changePercentAttr = changePercentElement.attr("data-price-change-percent");
            if (!changePercentAttr.isEmpty()) {
                try {
                    return new BigDecimal(changePercentAttr);
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private String parseName(Document doc, String symbol) {
        Element nameElement = doc.selectFirst(SELECTOR_NAME);
        if (nameElement != null) {
            String name = nameElement.text();
            if (!name.isEmpty()) {
                return name;
            }
        }
        // Fallback to title or symbol
        String title = doc.title();
        if (title != null && !title.isEmpty()) {
            return title.split(" - ")[0].trim();
        }
        return symbol;
    }

    private String detectCurrency(String symbol) {
        if (symbol.contains("USD") || symbol.contains(":")) {
            return "USD";
        }
        if (symbol.contains("BRL")) {
            return "BRL";
        }
        if (symbol.contains("EUR")) {
            return "EUR";
        }
        return "USD";
    }

    private String buildUrl(String symbol) {
        // Handle different symbol formats
        // Examples: "IBOV:INDEXBVMF", "USD-BRL", "BTC-USD"
        return GOOGLE_FINANCE_BASE_URL + symbol.replace("-", "/");
    }

    /**
     * Creates sample/mock indicators for testing or fallback when scraping fails.
     * In production, consider using official APIs as fallback.
     */
    public List<FinancialIndicatorDTO> getSampleIndicators() {
        return List.of(
                FinancialIndicatorDTO.of("IBOV", "Índice Bovespa", new BigDecimal("128500.00"), 
                        new BigDecimal("1500.00"), new BigDecimal("1.18"), "BRL", "Sample Data"),
                FinancialIndicatorDTO.of("USD-BRL", "Dólar/Real", new BigDecimal("4.95"), 
                        new BigDecimal("-0.02"), new BigDecimal("-0.40"), "BRL", "Sample Data"),
                FinancialIndicatorDTO.of("EUR-BRL", "Euro/Real", new BigDecimal("5.35"), 
                        new BigDecimal("0.03"), new BigDecimal("0.56"), "BRL", "Sample Data"),
                FinancialIndicatorDTO.of("BTC-USD", "Bitcoin", new BigDecimal("42500.00"), 
                        new BigDecimal("850.00"), new BigDecimal("2.04"), "USD", "Sample Data"),
                FinancialIndicatorDTO.of("ETH-USD", "Ethereum", new BigDecimal("2250.00"), 
                        new BigDecimal("45.00"), new BigDecimal("2.04"), "USD", "Sample Data"),
                FinancialIndicatorDTO.of("GOLD", "Ouro", new BigDecimal("2050.00"), 
                        new BigDecimal("15.00"), new BigDecimal("0.74"), "USD", "Sample Data")
        );
    }
}
