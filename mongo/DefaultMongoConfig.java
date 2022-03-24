import org.springframework.context.annotation.Bean;

public class DefaultMongoConfig {

    @Bean
    public MongoHealthIndicator dubboHealthIndicator(MongoTemplate mongoTemplate) {
        return new MongoHealthIndicator(mongoTemplate);
    }

}
