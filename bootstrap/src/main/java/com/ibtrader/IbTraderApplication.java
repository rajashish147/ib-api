package com.ibtrader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IBKR Portfolio Management & Automated Trading Platform.
 *
 * <p>Entry point for the Spring Boot application. All submodules are assembled here:
 * <ul>
 *   <li><b>domain</b> — Pure domain model, aggregates, events, port interfaces</li>
 *   <li><b>application</b> — Use-case orchestration, application services</li>
 *   <li><b>infrastructure</b> — JPA adapters, IBKR TWS API integration, Flyway</li>
 *   <li><b>api</b> — REST controllers, OpenAPI, request/response DTOs</li>
 *   <li><b>strategy-engine</b> — Spring State Machine, threshold evaluation, rebalancing</li>
 *   <li><b>risk</b> — Pre-trade risk pipeline, circuit breakers</li>
 *   <li><b>scheduler</b> — Portfolio snapshot, strategy polling, reconciliation jobs</li>
 * </ul>
 *
 * <p><b>Safety note</b>: This application can submit real orders to Interactive Brokers.
 * Ensure {@code app.ib.paper-trading=true} is set unless intentionally operating on a live account.
 *
 * @see <a href="http://localhost:8080/swagger-ui.html">Swagger UI (local)</a>
 * @see <a href="http://localhost:8080/actuator/health">Health Check</a>
 */
@SpringBootApplication(scanBasePackages = "com.ibtrader")
@ConfigurationPropertiesScan("com.ibtrader")
@EnableScheduling
public class IbTraderApplication {

    public static void main(String[] args) {
        SpringApplication.run(IbTraderApplication.class, args);
    }
}
