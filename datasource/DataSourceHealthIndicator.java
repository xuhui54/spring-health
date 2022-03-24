import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据库健康检查器
 * 重构自 org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator
 * 增加耗时统计
 * 将该类注册到spring中时，bean name命名为dbHealthIndicator，即可替代默认实现
 *
 * @see org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator
 * @see DefaultDataSourceConfig
 */
public class DataSourceHealthIndicator extends AbstractHealthIndicator {
    private static final String DEFAULT_QUERY = "SELECT 1";
    @Autowired
    private DataSource dataSource;

    private String query;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Create a new {@link org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator} instance.
     */
    public DataSourceHealthIndicator() {
        this(null, null);
    }

    /**
     * Create a new {@link org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator} using the specified
     * {@link DataSource}.
     *
     * @param dataSource the data source
     */
    public DataSourceHealthIndicator(DataSource dataSource) {
        this(dataSource, null);
    }

    /**
     * Create a new {@link org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator} using the specified
     * {@link DataSource} and validation query.
     *
     * @param dataSource the data source
     * @param query      the validation query to use (can be {@code null})
     */
    public DataSourceHealthIndicator(DataSource dataSource, String query) {
        super("DataSource health check failed");
        this.dataSource = dataSource;
        this.query = query;
        this.jdbcTemplate = (dataSource != null ? new JdbcTemplate(dataSource) : null);
    }


    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {

        if (this.dataSource == null) {
            builder.up().withDetail("database", "unknown");
        } else {
            doDataSourceHealthCheck(builder);
        }
    }

    private void doDataSourceHealthCheck(Health.Builder builder) throws Exception {
        String product = getProduct();
        builder.up().withDetail("database", product);
        String validationQuery = getValidationQuery(product);
        if (StringUtils.hasText(validationQuery)) {
            // Avoid calling getObject as it breaks MySQL on Java 7
            // 增加耗时统计
            long startTime = System.currentTimeMillis();
            List<Object> results = this.jdbcTemplate.query(validationQuery, new SingleColumnRowMapper());
            long endTime = System.currentTimeMillis();
            Object result = DataAccessUtils.requiredSingleResult(results);
            builder.withDetail("result", "1".equals(String.valueOf(result)) ? "ok" : "no");
            builder.withDetail("timeMs", endTime - startTime);//毫秒
        }
    }

    private String getProduct() {
        return this.jdbcTemplate.execute((ConnectionCallback<String>) this::getProduct);
    }

    private String getProduct(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName();
    }

    protected String getValidationQuery(String product) {
        String query = this.query;
        if (!StringUtils.hasText(query)) {
            DatabaseDriver specific = DatabaseDriver.fromProductName(product);
            query = specific.getValidationQuery();
        }
        if (!StringUtils.hasText(query)) {
            query = DEFAULT_QUERY;
        }
        return query;
    }

    /**
     * Set the {@link DataSource} to use.
     *
     * @param dataSource the data source
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Set a specific validation query to use to validate a connection. If none is set, a
     * default validation query is used.
     *
     * @param query the query
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Return the validation query or {@code null}.
     *
     * @return the query
     */
    public String getQuery() {
        return this.query;
    }

    /**
     * {@link RowMapper} that expects and returns results from a single column.
     */
    private static class SingleColumnRowMapper implements RowMapper<Object> {

        @Override
        public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
            ResultSetMetaData metaData = rs.getMetaData();
            int columns = metaData.getColumnCount();
            if (columns != 1) {
                throw new IncorrectResultSetColumnCountException(1, columns);
            }
            return JdbcUtils.getResultSetValue(rs, 1);
        }

    }
}
