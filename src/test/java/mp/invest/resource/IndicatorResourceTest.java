package mp.invest.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class IndicatorResourceTest {

    @Test
    void shouldListAllIndicators() {
        given()
                .when().get("/api/indicators")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", not(empty()));
    }

    @Test
    void shouldGetIndicatorBySymbol() {
        // First, ensure cache has data
        given()
                .when().get("/api/indicators/refresh")
                .then()
                .statusCode(200);

        // Then get a specific indicator (using sample data symbol)
        given()
                .when().get("/api/indicators/IBOV")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("symbol", equalTo("IBOV"))
                .body("value", notNullValue());
    }

    @Test
    void shouldReturn404ForNonExistentIndicator() {
        given()
                .when().get("/api/indicators/NON-EXISTENT-SYMBOL")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("error", equalTo("NOT_FOUND"));
    }

    @Test
    void shouldTriggerRefresh() {
        given()
                .when().get("/api/indicators/refresh")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("OK"))
                .body("message", containsString("triggered"));
    }

    @Test
    void shouldGetCacheStats() {
        given()
                .when().get("/api/indicators/stats")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("cacheSize", notNullValue())
                .body("indicators", notNullValue());
    }
}
