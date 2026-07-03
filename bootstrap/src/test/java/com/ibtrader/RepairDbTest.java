package com.ibtrader;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

class RepairDbTest {

    private static final String REPAIR_SQL = """
            INSERT INTO assets (id, symbol, exchange, currency, asset_class, multiplier, enabled, created_at, updated_at)
            VALUES
                (gen_random_uuid(), 'AAPL', 'SMART', 'USD', 'STOCK', 1, TRUE, now(), now()),
                (gen_random_uuid(), 'MSFT', 'SMART', 'USD', 'STOCK', 1, TRUE, now(), now()),
                (gen_random_uuid(), 'SNDK', 'SMART', 'USD', 'STOCK', 1, TRUE, now(), now()),
                (gen_random_uuid(), 'META', 'SMART', 'USD', 'STOCK', 1, TRUE, now(), now()),
                (gen_random_uuid(), 'NVDA', 'SMART', 'USD', 'STOCK', 1, TRUE, now(), now())
            ON CONFLICT (symbol, exchange) DO UPDATE SET enabled = TRUE
            """;

    @Test
    void repairAssetsWhenDatabaseCredentialsAreProvided() throws Exception {
        String url = System.getenv("IBTRADER_REPAIR_DB_URL");
        String user = System.getenv("IBTRADER_REPAIR_DB_USER");
        String password = System.getenv("IBTRADER_REPAIR_DB_PASSWORD");

        Assumptions.assumeTrue(url != null && !url.isBlank(), "No repair DB URL configured");
        Assumptions.assumeTrue(user != null && !user.isBlank(), "No repair DB user configured");
        Assumptions.assumeTrue(password != null && !password.isBlank(), "No repair DB password configured");

        try (java.sql.Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(REPAIR_SQL);

            try (ResultSet resultSet =
                         statement.executeQuery("SELECT symbol, enabled, exchange FROM assets ORDER BY symbol")) {
                while (resultSet.next()) {
                    System.out.println(
                            "ASSET: " + resultSet.getString("symbol")
                                    + ", ENABLED: " + resultSet.getBoolean("enabled")
                                    + ", EXCHANGE: " + resultSet.getString("exchange"));
                }
            }
        }
    }
}
