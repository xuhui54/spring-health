import org.springframework.context.annotation.Bean;

public class DefaultDataSourceConfig {

    @Bean
    public DataSourceHealthIndicator dbHealthIndicator() {
        return new DataSourceHealthIndicator();
    }

}
