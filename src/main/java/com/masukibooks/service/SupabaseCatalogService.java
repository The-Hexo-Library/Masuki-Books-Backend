package com.masukibooks.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseCatalogService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceRoleKey;

    @Value("${supabase.books-table:books}")
    private String booksTable;

    public void deleteBookCatalogRow(UUID productId) {
        if (productId == null || isBlank(supabaseUrl) || isBlank(supabaseServiceRoleKey)) {
            deleteBookCatalogRowFromDatabase(productId);
            return;
        }

        deleteBookCatalogRowFromDatabase(productId);

        String base = trimTrailingSlash(supabaseUrl);
        String table = isBlank(booksTable) ? "books" : booksTable.trim();
        String encodedTable = URLEncoder.encode(table, StandardCharsets.UTF_8);
        String encodedId = URLEncoder.encode(productId.toString(), StandardCharsets.UTF_8);
        String requestUrl = base + "/rest/v1/" + encodedTable + "?id=eq." + encodedId;

        HttpRequest request = HttpRequest.newBuilder(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(10))
                .header("apikey", supabaseServiceRoleKey)
                .header("Authorization", "Bearer " + supabaseServiceRoleKey)
                .header("Prefer", "return=minimal")
                .DELETE()
                .build();

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                log.warn("Supabase catalog delete failed for product {} with status {}: {}", productId, status, response.body());
            }
        } catch (Exception ex) {
            log.warn("Supabase catalog delete failed for product {}: {}", productId, ex.getMessage());
        }
    }

    private void deleteBookCatalogRowFromDatabase(UUID productId) {
        if (productId == null) {
            return;
        }
        String table = normalizedTableName(booksTable);
        if (table == null) {
            log.warn("Skipping Supabase catalog DB delete due to invalid table name: {}", booksTable);
            return;
        }

        try {
            boolean hasId = hasColumn(table, "id");
            boolean hasProductId = hasColumn(table, "product_id");
            boolean hasBookId = hasColumn(table, "book_id");
            if (!hasId && !hasProductId && !hasBookId) {
                return;
            }

            StringBuilder where = new StringBuilder();
            if (hasId) {
                where.append("CAST(id AS text) = ?");
            }
            if (hasProductId) {
                if (!where.isEmpty()) {
                    where.append(" OR ");
                }
                where.append("CAST(product_id AS text) = ?");
            }
            if (hasBookId) {
                if (!where.isEmpty()) {
                    where.append(" OR ");
                }
                where.append("CAST(book_id AS text) = ?");
            }

            String sql = "DELETE FROM public." + table + " WHERE " + where;
            int paramCount = (hasId ? 1 : 0) + (hasProductId ? 1 : 0) + (hasBookId ? 1 : 0);
            Object[] params = new Object[paramCount];
            for (int i = 0; i < paramCount; i++) {
                params[i] = productId.toString();
            }
            jdbcTemplate.update(sql, params);
        } catch (Exception ex) {
            log.warn("Supabase catalog DB delete failed for product {}: {}", productId, ex.getMessage());
        }
    }

    private boolean hasColumn(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                Integer.class,
                table,
                column
        );
        return count != null && count > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimTrailingSlash(String value) {
        String out = value == null ? "" : value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private String normalizedTableName(String value) {
        String name = isBlank(value) ? "books" : value.trim();
        if (!name.matches("^[a-zA-Z0-9_]+$")) {
            return null;
        }
        return name.toLowerCase();
    }
}
