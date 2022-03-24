import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.config.spring.ReferenceBean;
import org.apache.dubbo.config.spring.beans.factory.annotation.ReferenceAnnotationBeanPostProcessor;
import org.apache.dubbo.rpc.service.EchoService;
import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * dubbo的健康检查器
 * 将该类注册成为spring bean后即可生效
 *
 * @see DefaultDubboConfig
 */
public class DubboHealthIndicator extends AbstractHealthIndicator implements ApplicationContextAware {

    protected static final String ECHO_MESSAGE = "ok";

    private ApplicationContext applicationContext;

    private Boolean invokeCheckInitiated = false;
    private final Map<String, String> invokeCheckReferenceBeanNameMap = Maps.newHashMap();
    private final Map<String, ReferenceBean<?>> invokeCheckReferenceBeanCacheMap = Maps.newHashMap();

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        ExtensionLoader<StatusChecker> extensionLoader = getExtensionLoader(StatusChecker.class);

        // 对线程池进行检测，基于dubbo自身的org.apache.dubbo.rpc.protocol.dubbo.status.ThreadPoolStatusChecker
        org.apache.dubbo.common.status.Status threadpoolDubboStatus = extensionLoader.getExtension("threadpool").check();
        Status threadpoolStatus;
        if (org.apache.dubbo.common.status.Status.Level.OK.equals(threadpoolDubboStatus.getLevel())
            || org.apache.dubbo.common.status.Status.Level.UNKNOWN.equals(threadpoolDubboStatus.getLevel())) {
            threadpoolStatus = Status.UP;
        } else {
            threadpoolStatus = Status.DOWN;
        }
        builder.withDetail("threadpool", ImmutableMap.builder().put("status", threadpoolStatus.getCode()).build());

        // 对注册中心进行检测，基于dubbo自身的org.apache.dubbo.registry.status.RegistryStatusChecker
        org.apache.dubbo.common.status.Status registryDubboStatus = extensionLoader.getExtension("registry").check();
        Status registryStatus;
        if (org.apache.dubbo.common.status.Status.Level.OK.equals(registryDubboStatus.getLevel())) {
            registryStatus = Status.UP;
        } else {
            registryStatus = Status.DOWN;
        }
        builder.withDetail("registry", ImmutableMap.builder().put("status", registryStatus.getCode()).build());

        // 真实调用检测，基于dubbo的回声测试功能
        ReferenceAnnotationBeanPostProcessor referenceAnnotationBeanPostProcessor = (ReferenceAnnotationBeanPostProcessor) applicationContext.getBean("referenceAnnotationBeanPostProcessor");
        if (!invokeCheckInitiated) {
            for (ReferenceBean<?> referenceBean : referenceAnnotationBeanPostProcessor.getReferenceBeans()) {
                invokeCheckReferenceBeanNameMap.forEach((providerName, referenceBeanName) -> {
                    if (referenceBeanName.equalsIgnoreCase(referenceBean.getInterface())) {
                        invokeCheckReferenceBeanCacheMap.put(providerName, referenceBean);
                    }
                });
            }
            invokeCheckInitiated = true;
        }

        int invokeCheckUpCount = 0;
        for (Map.Entry<String, ReferenceBean<?>> entry : invokeCheckReferenceBeanCacheMap.entrySet()) {
            String providerName = entry.getKey();
            ReferenceBean<?> referenceBean = entry.getValue();
            EchoService echoService = (EchoService) referenceBean.getObject();
            if (echoService != null) {
                Map<String, Object> detailMap = Maps.newLinkedHashMap();
                long startTime = System.currentTimeMillis();
                long endTime;
                try {
                    String result = (String) echoService.$echo(ECHO_MESSAGE);
                    endTime = System.currentTimeMillis();
                    detailMap.put("status", Status.UP.getCode());
                    detailMap.put("result", result);
                    invokeCheckUpCount++;
                } catch (Throwable t) {
                    endTime = System.currentTimeMillis();
                    detailMap.put("status", Status.DOWN.getCode());
                    detailMap.put("error", t.getMessage());
                }
                detailMap.put("timeMs", endTime - startTime);
                builder.withDetail(providerName + "-invoke-check", detailMap);
            }
        }

        // 汇总统计
        if (Status.UP.equals(threadpoolStatus)
            && Status.UP.equals(registryStatus)
            && invokeCheckUpCount >= invokeCheckReferenceBeanCacheMap.size()) {
            builder.up();
        } else {
            builder.down();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void registerInvokeCheckReferenceBeanName(String providerName, String referenceBeanName) {
        this.invokeCheckReferenceBeanNameMap.put(providerName, referenceBeanName);
    }
}