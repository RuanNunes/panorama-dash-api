package mp.invest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

/**
 * Exception mapper for IndicatorNotFoundException.
 * Returns a 404 response with error details.
 */
@Provider
public class IndicatorNotFoundExceptionMapper implements ExceptionMapper<IndicatorNotFoundException> {

    @Override
    public Response toResponse(IndicatorNotFoundException e) {
        Map<String, Object> error = Map.of(
                "error", "NOT_FOUND",
                "message", e.getMessage(),
                "symbol", e.getSymbol(),
                "timestamp", Instant.now().toString()
        );
        return Response.status(Response.Status.NOT_FOUND)
                .entity(error)
                .build();
    }
}
