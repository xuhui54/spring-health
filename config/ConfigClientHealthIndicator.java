
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 配置客户端健康指示器
 *
 * @author xuhui
 * @date 2022/03/15
 */
@Component
public class ConfigClientHealthIndicator implements HealthIndicator {

  @Autowired
  ConfigClientProperties properties;

  /**
   * 健康
   *
   * @return {@link Health}
   */@Override
  public Health health() {
    // 健康检查的逻辑
    long startTime = System.currentTimeMillis();
    Status  status =getStatus();
    long endTime = System.currentTimeMillis();

    return new Health.Builder(status).withDetail("timeMs", endTime - startTime).build();
  }

  /**
   * 获得地位
   *
   * @return {@link Status}
   */private Status getStatus() {
    RestTemplate restTemplate = new RestTemplate();
    String path = "/{name}/{profile}";
    Object[] args = new String[] { "base", "dev" };
    HttpHeaders headers = new HttpHeaders();
    final HttpEntity<Void> entity = new HttpEntity<>((Void) null, headers);
    ResponseEntity<Environment>  response = restTemplate.exchange(properties.getUri()[0] + path, HttpMethod.GET, entity,
            Environment.class, args);
    return response.getStatusCode()== HttpStatus.OK?Status.UP : Status.DOWN;
  }



}
