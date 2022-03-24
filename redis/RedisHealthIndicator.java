import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.ClusterInfo;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;

/**
 * redis的健康检查器
 * 重构自 org.springframework.boot.actuate.redis.RedisHealthIndicator
 * 增加真实调用与耗时统计
 * 将该类注册到spring中时，bean name命名为redisHealthIndicator，即可替代默认实现
 *
 * @see org.springframework.boot.actuate.redis.RedisHealthIndicator
 * @see DefaultRedisConfig
 */
public class RedisHealthIndicator extends AbstractHealthIndicator {

    protected static final byte[] GET_OK_BYTES = "ok".getBytes(StandardCharsets.UTF_8);

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisHealthIndicator(RedisConnectionFactory connectionFactory) {
        super("Redis health check failed");
        Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
        this.redisConnectionFactory = connectionFactory;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        RedisConnection connection = RedisConnectionUtils.getConnection(this.redisConnectionFactory);
        try {
            doHealthCheck(builder, connection);
        } finally {
            RedisConnectionUtils.releaseConnection(connection, this.redisConnectionFactory);
        }
    }

    private void doHealthCheck(Health.Builder builder, RedisConnection connection) {
        // 增加真实调用与耗时统计
        try {
            long starTime = System.currentTimeMillis();
            connection.get(GET_OK_BYTES);
            long entTime = System.currentTimeMillis();
            builder.withDetail("result", "ok");
            builder.withDetail("timeMs", entTime - starTime);
            builder.up();
        } catch (Throwable t) {
            builder.down(t);
        }

        if (connection instanceof RedisClusterConnection) {
            ClusterInfo clusterInfo = ((RedisClusterConnection) connection).clusterGetClusterInfo();
            builder.withDetail("cluster_size", clusterInfo.getClusterSize());
            builder.withDetail("slots_up", clusterInfo.getSlotsOk());
            builder.withDetail("slots_fail", clusterInfo.getSlotsFail());
        } else {
            builder.withDetail("version", connection.info("server").getProperty("redis_version"));
        }
    }

}
