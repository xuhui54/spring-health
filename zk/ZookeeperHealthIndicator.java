import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * zookeeper的健康检查器
 * 重构自 org.springframework.cloud.zookeeper.ZookeeperHealthIndicator
 * 添加耗时的展示
 * 将该类注册到spring中时，即可替代默认实现
 * 如果系统使用了spring-cloud-starter-zookeeper，还需要将其AutoConfiguration移除
 * @SpringBootApplication(exclude = {ZookeeperHealthAutoConfiguration.class})
 *
 * @see org.springframework.cloud.zookeeper.ZookeeperHealthIndicator
 * @see DefaultZookeeperConfig
 */
public class ZookeeperHealthIndicator extends AbstractHealthIndicator {

    @Autowired
    protected CuratorFramework curator;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            CuratorFrameworkState state = curator.getState();
            if (state == CuratorFrameworkState.STARTED) {
                // 增加耗时统计
                long starTime = System.currentTimeMillis();
                Stat stat = curator.checkExists().forPath("/");
                long entTime = System.currentTimeMillis();
                builder.withDetail("timeMs", entTime - starTime);
                if (stat != null) {
                    builder.up();
                } else {
                    builder.down().withDetail("error", "Root for namespace does not exist");
                }
            } else {
                builder.down().withDetail("error", "Client not started");
            }
        } catch (Exception e) {
            builder.down(e);
        }
    }

}