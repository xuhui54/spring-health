import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

public class DefaultZookeeperConfig {

    @Bean
    public ZookeeperHealthIndicator zookeeperHealthIndicator() {
        return new ZookeeperHealthIndicator();
    }
}
