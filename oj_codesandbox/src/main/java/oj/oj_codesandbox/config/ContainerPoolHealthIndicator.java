package oj.oj_codesandbox.config;

import oj.oj_codesandbox.pool.MultiLanguageDockerSandBoxPool;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 容器池健康检查指示器
 * 集成Spring Boot Actuator健康检查
 */
@Component
public class ContainerPoolHealthIndicator implements HealthIndicator {
    
    @Autowired
    private MultiLanguageDockerSandBoxPool containerPool;
    
    @Override
    public Health health() {
        try {
            // 检查容器池整体健康状态
            boolean isHealthy = containerPool.isHealthy();
            
            if (!isHealthy) {
                return Health.down()
                        .withDetail("status", "容器池不健康")
                        .withDetail("reason", "Docker连接失败或容器池为空")
                        .build();
            }
            
            // 获取详细状态信息
            Map<String, oj.oj_codesandbox.pool.LanguageContainerPool.PoolStatus> statusMap = 
                    containerPool.getAllPoolStatus();
            
            Health.Builder healthBuilder = Health.up()
                    .withDetail("status", "容器池健康")
                    .withDetail("supportedLanguages", containerPool.getSupportedLanguages())
                    .withDetail("poolCount", statusMap.size());
            
            // 添加各语言池的详细状态
            for (Map.Entry<String, oj.oj_codesandbox.pool.LanguageContainerPool.PoolStatus> entry : statusMap.entrySet()) {
                String language = entry.getKey();
                oj.oj_codesandbox.pool.LanguageContainerPool.PoolStatus status = entry.getValue();
                
                healthBuilder.withDetail(language + ".currentSize", status.getCurrentSize())
                        .withDetail(language + ".targetSize", status.getTargetSize())
                        .withDetail(language + ".availableSize", status.getAvailableSize())
                        .withDetail(language + ".utilizationRate", String.format("%.2f%%", status.getUtilizationRate() * 100));
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "容器池健康检查异常")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
