package mp.invest.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO representing a financial indicator collected from web scraping.
 * Contains the symbol, name, current value, change percentage, and metadata.
 */
public record FinancialIndicatorDTO(
        String symbol,
        String name,
        BigDecimal value,
        BigDecimal change,
        BigDecimal changePercent,
        String currency,
        Instant lastUpdated,
        String source
) {
    public static FinancialIndicatorDTO of(String symbol, String name, BigDecimal value, 
                                            BigDecimal change, BigDecimal changePercent,
                                            String currency, String source) {
        return new FinancialIndicatorDTO(symbol, name, value, change, changePercent, 
                                          currency, Instant.now(), source);
    }
}
