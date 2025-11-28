package mp.invest.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import mp.invest.dto.FinancialIndicatorDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class IndicatorScraperServiceTest {

    @Inject
    IndicatorScraperService scraperService;

    @Test
    void shouldReturnSampleIndicators() {
        List<FinancialIndicatorDTO> samples = scraperService.getSampleIndicators();
        
        assertNotNull(samples);
        assertFalse(samples.isEmpty());
        
        // Verify expected sample indicators exist
        assertTrue(samples.stream().anyMatch(i -> "IBOV".equals(i.symbol())));
        assertTrue(samples.stream().anyMatch(i -> "USD-BRL".equals(i.symbol())));
        assertTrue(samples.stream().anyMatch(i -> "BTC-USD".equals(i.symbol())));
    }

    @Test
    void shouldReturnEmptyForNullSymbol() {
        assertTrue(scraperService.scrapeIndicator(null).isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankSymbol() {
        assertTrue(scraperService.scrapeIndicator("").isEmpty());
        assertTrue(scraperService.scrapeIndicator("   ").isEmpty());
    }

    @Test
    void sampleIndicatorsShouldHaveRequiredFields() {
        List<FinancialIndicatorDTO> samples = scraperService.getSampleIndicators();
        
        for (FinancialIndicatorDTO indicator : samples) {
            assertNotNull(indicator.symbol(), "Symbol should not be null");
            assertNotNull(indicator.name(), "Name should not be null");
            assertNotNull(indicator.value(), "Value should not be null");
            assertNotNull(indicator.currency(), "Currency should not be null");
            assertNotNull(indicator.source(), "Source should not be null");
            assertNotNull(indicator.lastUpdated(), "LastUpdated should not be null");
        }
    }
}
