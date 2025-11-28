# panorama-dash-api

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Financial Indicators API

This API provides financial market indicators through scheduled web scraping with caching. It collects data from financial sources (such as indices, exchange rates, commodities, and cryptocurrencies) and serves them via REST endpoints.

### Features

- **Scheduled Web Scraping**: Periodically fetches financial data using configurable cron expressions
- **In-Memory Cache**: Thread-safe caching with configurable TTL using ConcurrentHashMap
- **Retry with Exponential Backoff**: Automatic retries with exponential backoff for failed requests
- **Fallback Data**: Sample data fallback when scraping fails
- **RESTful API**: Endpoints to list indicators, get by symbol, and trigger manual refresh
- **Health Checks**: Built-in health check endpoints
- **Metrics**: Prometheus metrics for monitoring scraping success/failure rates
- **OpenAPI/Swagger**: API documentation with Swagger UI

### ⚠️ Compliance Warning

Web scraping carries legal and technical risks:

- **Terms of Service**: Many financial websites prohibit scraping. Always review and comply with target site's ToS.
- **Rate Limiting**: Excessive requests may result in IP blocking.
- **Data Accuracy**: Scraped data may be delayed, incomplete, or incorrect.
- **API Alternatives**: For production use, consider official APIs like Alpha Vantage, Yahoo Finance, or Google Finance API.

### API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/indicators` | List all cached indicators |
| GET | `/api/indicators/{symbol}` | Get indicator by symbol (e.g., IBOV, USD-BRL) |
| GET | `/api/indicators/refresh` | Trigger manual refresh |
| GET | `/api/indicators/stats` | Get cache statistics |
| GET | `/q/health` | Health check |
| GET | `/q/metrics` | Prometheus metrics |
| GET | `/swagger-ui` | Swagger UI documentation |

### Configuration

Key configuration properties in `application.yml`:

```yaml
scraper:
  schedule: "0 */5 * * * ?"  # Cron expression (every 5 minutes)
  connection-timeout: 10s
  read-timeout: 30s
  max-retries: 3
  retry-backoff: 1s
  cache-ttl: 10m
  enabled: true
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

### Testing the API

Once running, you can test the endpoints:

```bash
# List all indicators
curl http://localhost:8080/api/indicators

# Get specific indicator
curl http://localhost:8080/api/indicators/IBOV

# Trigger manual refresh
curl http://localhost:8080/api/indicators/refresh

# Check health
curl http://localhost:8080/q/health

# View metrics
curl http://localhost:8080/q/metrics
```

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it's not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/panorama-dash-api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Document your REST APIs with OpenAPI - comes with Swagger UI
- YAML Configuration ([guide](https://quarkus.io/guides/config-yaml)): Use YAML to configure your Quarkus application
- Scheduler ([guide](https://quarkus.io/guides/scheduler)): Schedule jobs and tasks
- Micrometer Metrics ([guide](https://quarkus.io/guides/micrometer)): Collect runtime metrics with Micrometer
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor application health

## Provided Code

### YAML Config

Configure your application with YAML

[Related guide section...](https://quarkus.io/guides/config-reference#configuration-examples)

The Quarkus application configuration is located in `src/main/resources/application.yml`.

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
