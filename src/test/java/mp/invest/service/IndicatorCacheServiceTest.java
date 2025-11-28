package mp.invest.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import mp.invest.dto.FinancialIndicatorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class IndicatorCacheServiceTest {

    @Inject
    IndicatorCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService.clear();
    }

    @Test
    void shouldStoreAndRetrieveIndicator() {
        FinancialIndicatorDTO indicator = FinancialIndicatorDTO.of(
                "TEST-USD", "Test Symbol", new BigDecimal("100.00"),
                new BigDecimal("1.00"), new BigDecimal("1.00"),
                "USD", "Test Source"
        );

        cacheService.put(indicator);

        Optional<FinancialIndicatorDTO> result = cacheService.get("TEST-USD");
        assertTrue(result.isPresent());
        assertEquals("TEST-USD", result.get().symbol());
        assertEquals(new BigDecimal("100.00"), result.get().value());
    }

    @Test
    void shouldReturnEmptyForNonExistentSymbol() {
        Optional<FinancialIndicatorDTO> result = cacheService.get("NON-EXISTENT");
        assertFalse(result.isPresent());
    }

    @Test
    void shouldBeCaseInsensitive() {
        FinancialIndicatorDTO indicator = FinancialIndicatorDTO.of(
                "TEST", "Test", new BigDecimal("50.00"),
                BigDecimal.ZERO, BigDecimal.ZERO,
                "USD", "Test"
        );

        cacheService.put(indicator);

        assertTrue(cacheService.get("test").isPresent());
        assertTrue(cacheService.get("TEST").isPresent());
        assertTrue(cacheService.get("Test").isPresent());
    }

    @Test
    void shouldReturnAllIndicators() {
        cacheService.put(FinancialIndicatorDTO.of(
                "SYM1", "Symbol 1", new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Test"
        ));
        cacheService.put(FinancialIndicatorDTO.of(
                "SYM2", "Symbol 2", new BigDecimal("200.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Test"
        ));

        Collection<FinancialIndicatorDTO> all = cacheService.getAll();
        assertEquals(2, all.size());
    }

    @Test
    void shouldClearCache() {
        cacheService.put(FinancialIndicatorDTO.of(
                "SYM1", "Symbol 1", new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, "USD", "Test"
        ));

        assertEquals(1, cacheService.size());
        cacheService.clear();
        assertEquals(0, cacheService.size());
    }

    @Test
    void shouldHandleNullSymbol() {
        Optional<FinancialIndicatorDTO> result = cacheService.get(null);
        assertFalse(result.isPresent());
    }

    @Test
    void shouldNotStoreNullIndicator() {
        cacheService.put(null);
        assertEquals(0, cacheService.size());
    }
}
