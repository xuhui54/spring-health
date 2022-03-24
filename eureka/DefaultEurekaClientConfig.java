
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 默认的尤里卡客户端检查
 *
 * @author xuhui
 * @date 2022/03/15
 */@Configuration
public class DefaultEurekaClientConfig {

    /**
     * 尤里卡客户健康指示器
     *
     * @return {@link EurekaClientHealthIndicator}
     */
    @Bean
    @ConditionalOnMissingBean
    public EurekaClientHealthIndicator eurekaClientHealthIndicator(){
        return new EurekaClientHealthIndicator();
    }

}
