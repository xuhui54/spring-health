
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 尤里卡客户健康指示器
 *
 * @author xuhui
 * @date 2022/03/15
 */
public class EurekaClientHealthIndicator implements HealthIndicator {

  @Qualifier("eurekaClient")
  @Autowired
  EurekaClient eurekaClient;
  @Autowired
  EurekaClientConfig clientConfig;


  private  static final String  CODE_SERVER = "WMP-ENGINE";
  private  static final String  OPEN_API = "WMP-API";

  /**
   * 健康检查
   *
   * @return {@link Health}
   */@Override
  public Health health() {
    // 基础健康检查的逻辑
    long startTime = System.currentTimeMillis();
    long endTime = 0;
    Status  satus =getStatus();

    //判断关联服务是否正常
    Applications applications = this.eurekaClient.getApplications();
    if (applications == null) {
      return new Health.Builder(Status.DOWN).build();
    }
    Map<String, Object> result = new HashMap<>();
    for (Application application : applications.getRegisteredApplications()) {
      if (!application.getInstances().isEmpty()) {

        Map<String, Object> appResult = new HashMap<>();
        appResult.put("total",application.getInstances().size());
        for (InstanceInfo instanceInfo : application.getInstances()) {
          //判断核心服务知否存在
          getInstanceInfoHealth(instanceInfo,appResult);
        }
        result.put(application.getName(), appResult);
      }
    }
    if (!result.containsKey(CODE_SERVER)){
      result.put(CODE_SERVER, Status.DOWN);
    }
    endTime = System.currentTimeMillis();
    return new Health.Builder(satus).withDetail("timeMs", endTime - startTime)
    .withDetail("servers",result).build();
  }

  /**
   * 获取服务状态
   *
   * @return {@link Status}
   */
  private Status getStatus() {
    Status status = new Status(
            this.eurekaClient.getInstanceRemoteStatus().toString(),
            "Remote status from Eureka server");
    DiscoveryClient discoveryClient = getDiscoveryClient();
    if (discoveryClient != null && clientConfig.shouldFetchRegistry()) {
      long lastFetch = discoveryClient.getLastSuccessfulRegistryFetchTimePeriod();

      if (lastFetch < 0) {
        status = new Status("UP",
                "Eureka discovery client has not yet successfully connected to a Eureka server");
      }
      else if (lastFetch > clientConfig.getRegistryFetchIntervalSeconds() * 2000) {
        status = new Status("UP",
                "Eureka discovery client is reporting failures to connect to a Eureka server");
      }
    }

    return status;
  }

  /**
   * 得到实例信息健康
   *
   * @param instanceInfo 实例信息
   * @param appResult 应用的结果
   */
  private void getInstanceInfoHealth(InstanceInfo instanceInfo,Map<String, Object> appResult) {
    try {
      RestTemplate restTemplate = new RestTemplate();
      long startTime = System.currentTimeMillis();
      ResponseEntity<EurekaHealth> responseEntity;
      Status status;
      if (instanceInfo.getAppName().equals(OPEN_API)){
        responseEntity= restTemplate.getForEntity(instanceInfo.getHealthCheckUrl()
                .replace("/actuator/health","/openApi/actuator/info"), EurekaHealth.class);
        status =  responseEntity.getStatusCode()== HttpStatus.OK ?Status.UP : Status.DOWN;
      }else {
        //其他服务健康检查，可以集成特殊业务的判断
        responseEntity
                = restTemplate.getForEntity(instanceInfo.getHealthCheckUrl(), EurekaHealth.class);
        EurekaHealth eurekaHealth = responseEntity.getBody();
        status =  responseEntity.getStatusCode()== HttpStatus.OK &&
                Objects.requireNonNull(eurekaHealth).getStatus().equals(Status.UP)?Status.UP : Status.DOWN;
      }
      long endTime = System.currentTimeMillis();
      appResult.put(instanceInfo.getId()+"_TimeMs",endTime-startTime);
      appResult.put(instanceInfo.getId()+"_Status",status.getCode());
    }catch (Exception e) {
        e.printStackTrace();
    }
  }

  public  static class EurekaHealth {

    private  Status status;

    private  Map<String, Object> details;

    public Status getStatus() {
      return status;
    }

    public void setStatus(Status status) {
      this.status = status;
    }

    public Map<String, Object> getDetails() {
      return details;
    }

    public void setDetails(Map<String, Object> details) {
      this.details = details;
    }

    public EurekaHealth(Status status, Map<String, Object> details) {
      this.status = status;
      this.details = details;
    }

    public EurekaHealth() {
    }
  }

  /**
   * 会发现客户端
   *
   * @return {@link DiscoveryClient}
   */private DiscoveryClient getDiscoveryClient() {
    DiscoveryClient discoveryClient = null;
    if(AopUtils.isAopProxy(eurekaClient)) {
      discoveryClient = ProxyUtils.getTargetObject(eurekaClient);
    } else if(DiscoveryClient.class.isInstance(eurekaClient)) {
      discoveryClient = (DiscoveryClient)eurekaClient;
    }
    return discoveryClient;
  }
}
