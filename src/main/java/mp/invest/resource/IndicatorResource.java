package mp.invest.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import mp.invest.dto.FinancialIndicatorDTO;
import mp.invest.exception.IndicatorNotFoundException;
import mp.invest.service.IndicatorCacheService;
import mp.invest.service.IndicatorSchedulerService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Collection;
import java.util.Map;

/**
 * REST resource for financial indicators.
 * Provides endpoints to list all indicators, get by symbol, and trigger refresh.
 */
@Path("/api/indicators")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Financial Indicators", description = "Endpoints for financial market indicators")
public class IndicatorResource {

    private final IndicatorCacheService cacheService;
    private final IndicatorSchedulerService schedulerService;

    @Inject
    public IndicatorResource(IndicatorCacheService cacheService, 
                              IndicatorSchedulerService schedulerService) {
        this.cacheService = cacheService;
        this.schedulerService = schedulerService;
    }

    @GET
    @Operation(summary = "List all indicators", 
               description = "Returns all cached financial indicators")
    @APIResponses({
            @APIResponse(responseCode = "200", 
                         description = "List of indicators",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                           schema = @Schema(implementation = FinancialIndicatorDTO.class)))
    })
    public Collection<FinancialIndicatorDTO> getAllIndicators() {
        Collection<FinancialIndicatorDTO> indicators = cacheService.getAll();
        
        // If cache is empty, load fallback data
        if (indicators.isEmpty()) {
            schedulerService.loadFallbackData();
            indicators = cacheService.getAll();
        }
        
        return indicators;
    }

    @GET
    @Path("/{symbol}")
    @Operation(summary = "Get indicator by symbol", 
               description = "Returns a specific indicator by its symbol")
    @APIResponses({
            @APIResponse(responseCode = "200", 
                         description = "Indicator found",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON,
                                           schema = @Schema(implementation = FinancialIndicatorDTO.class))),
            @APIResponse(responseCode = "404", 
                         description = "Indicator not found")
    })
    public FinancialIndicatorDTO getIndicatorBySymbol(
            @Parameter(description = "Symbol of the indicator (e.g., IBOV, USD-BRL, BTC-USD)")
            @PathParam("symbol") String symbol) {
        return cacheService.get(symbol)
                .orElseThrow(() -> new IndicatorNotFoundException(symbol));
    }

    @GET
    @Path("/refresh")
    @Operation(summary = "Trigger manual refresh", 
               description = "Manually triggers a scrape operation to refresh indicators")
    @APIResponses({
            @APIResponse(responseCode = "200", 
                         description = "Refresh triggered successfully")
    })
    public Response triggerRefresh() {
        schedulerService.triggerScrape();
        return Response.ok(Map.of(
                "status", "OK",
                "message", "Refresh triggered successfully",
                "cacheSize", cacheService.size()
        )).build();
    }

    @GET
    @Path("/stats")
    @Operation(summary = "Get cache statistics", 
               description = "Returns current cache statistics")
    @APIResponses({
            @APIResponse(responseCode = "200", 
                         description = "Cache statistics")
    })
    public Response getCacheStats() {
        return Response.ok(Map.of(
                "cacheSize", cacheService.size(),
                "indicators", cacheService.getAll().stream()
                        .map(FinancialIndicatorDTO::symbol)
                        .toList()
        )).build();
    }
}
