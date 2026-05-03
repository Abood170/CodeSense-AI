package com.example.codereviewer.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        // Railway may inject a full URL in DATABASE_URL or SPRING_DATASOURCE_URL.
        // Both use the raw postgres(ql):// scheme which JDBC rejects.
        String rawUrl = System.getenv("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = System.getenv("SPRING_DATASOURCE_URL");
        }

        HikariConfig config = new HikariConfig();

        if (rawUrl != null && !rawUrl.isBlank()) {
            if (rawUrl.startsWith("jdbc:")) {
                // Already a valid JDBC URL — use as-is
                config.setJdbcUrl(rawUrl);
            } else {
                // Convert postgres://user:pass@host:port/db
                //      or postgresql://user:pass@host:port/db
                //   ->  jdbc:postgresql://host:port/db  (credentials set separately)
                try {
                    String normalized = rawUrl.replaceFirst("^postgres(ql)?://", "postgresql://");
                    URI uri = new URI(normalized);

                    String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                            + (uri.getPort() != -1 ? ":" + uri.getPort() : "")
                            + uri.getPath();
                    config.setJdbcUrl(jdbcUrl);

                    String userInfo = uri.getUserInfo();
                    if (userInfo != null && !userInfo.isBlank()) {
                        String[] parts = userInfo.split(":", 2);
                        config.setUsername(parts[0]);
                        if (parts.length > 1) {
                            config.setPassword(parts[1]);
                        }
                    }
                } catch (URISyntaxException e) {
                    throw new RuntimeException("Invalid database URL: " + rawUrl, e);
                }
            }
        } else {
            // No full URL — fall back to Railway's individual PG* vars (or local defaults)
            config.setJdbcUrl("jdbc:postgresql://"
                    + env("PGHOST", "localhost") + ":"
                    + env("PGPORT", "5432") + "/"
                    + env("PGDATABASE", "code_reviewer"));
            config.setUsername(env("PGUSER", "postgres"));
            config.setPassword(env("PGPASSWORD", "123123"));
        }

        return new HikariDataSource(config);
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return (v != null && !v.isBlank()) ? v : fallback;
    }
}
