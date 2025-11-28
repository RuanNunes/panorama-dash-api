package mp.invest.exception;

/**
 * Exception thrown when an indicator is not found in the cache.
 */
public class IndicatorNotFoundException extends RuntimeException {

    private final String symbol;

    public IndicatorNotFoundException(String symbol) {
        super("Indicator not found: " + symbol);
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
