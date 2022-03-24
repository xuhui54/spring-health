import org.springframework.context.annotation.Bean;

public class DefaultDubboConfig {

    @Bean
    public DubboHealthIndicator dubboHealthIndicator() {
        DubboHealthIndicator dubboHealthIndicator = new DubboHealthIndicator();
        dubboHealthIndicator.registerInvokeCheckReferenceBeanName("basic", "com.xxx.rdc.basic.system.user.UserProvider");
        return dubboHealthIndicator;
    }

}
