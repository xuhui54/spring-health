import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.Assert;

/**
 * mongodb的健康检查器
 * 重构自 org.springframework.boot.actuate.mongo.MongoHealthIndicator
 * 增加真实调用与耗时统计
 * 将该类注册到spring中时，bean name命名为mongoHealthIndicator，即可替代默认实现
 *
 * @see org.springframework.boot.actuate.mongo.MongoHealthIndicator
 * @see DefaultRedisConfig
 */
public class MongoHealthIndicator extends AbstractHealthIndicator {

    private final MongoTemplate mongoTemplate;

    /**
     * 查询的collection名称，根据实际修改
     */
    @Value("${spring.data.mongodb.health.collection:request_log}")
    private String collectionName;

    public MongoHealthIndicator(MongoTemplate mongoTemplate) {
        super("MongoDB health check failed");
        Assert.notNull(mongoTemplate, "MongoTemplate must not be null");
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void doHealthCheck(Builder builder) {
        try {
            long startTime = System.currentTimeMillis();
            long result = this.mongoTemplate.getCollection(collectionName).count();
            long endTime = System.currentTimeMillis();
            builder.withDetail("result", result >= 0 ? "yes" : "no");
            builder.withDetail("timeMs", endTime - startTime);
            Document buildInfoResult = this.mongoTemplate.executeCommand("{ buildInfo: 1 }");
            builder.withDetail("version", buildInfoResult.getString("version"));
            builder.up();
        } catch (Exception e) {
            builder.down(e);
        }

    }
}
