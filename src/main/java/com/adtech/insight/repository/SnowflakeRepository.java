package com.adtech.insight.repository;

import com.adtech.insight.dto.MetricType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;

@Repository
@RequiredArgsConstructor
public class SnowflakeRepository {

    @Qualifier("snowflakeDataSource")
    private final DataSource dataSource;

    public long query(
            String tenant, String campaignId, MetricType type, Instant from, Instant to) {
        String sql = """
            SELECT COALESCE(SUM(%s), 0)
            FROM CAMPAIGN_METRICS_FACT
            WHERE TENANT_ID = ?
              AND CAMPAIGN_ID = ?
              AND EVENT_TIME BETWEEN ? AND ?
            """.formatted(type.column());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, tenant);
            ps.setString(2, campaignId);
            ps.setTimestamp(3, Timestamp.from(from));
            ps.setTimestamp(4, Timestamp.from(to));

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getLong(1);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Snowflake query failed", e);
        }
    }

}
