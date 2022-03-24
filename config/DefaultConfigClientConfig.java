
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 默认spring配置中心健康检查
 *
 * @author xuhui
 * @date 2022/03/15
 */@Configuration
public class DefaultConfigClientConfig {

    /**
     * 配置客户端健康指示器
     *
     * @return {@link ConfigClientHealthIndicator}
     */
    @Bean
    @ConditionalOnMissingBean
    public ConfigClientHealthIndicator configClientHealthIndicator(){
        return new ConfigClientHealthIndicator();
    }
}
