package oj.oj_codesandbox;

import oj.oj_codesandbox.pool.MultiLanguageDockerSandBoxPool;
import oj.oj_codesandbox.pool.LanguageContainerPool;
import oj.oj_codesandbox.config.ContainerPoolMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * 容器池测试类
 */
@SpringBootTest
public class ContainerPoolTest {
    
    @Autowired
    private MultiLanguageDockerSandBoxPool containerPool;
    
    @Test
    public void testContainerPoolInitialization() {
        // 测试容器池初始化
        assert containerPool != null;
        System.out.println("容器池初始化成功");
    }
    
    @Test
    public void testGetSupportedLanguages() {
        // 测试获取支持的语言
        String[] languages = containerPool.getSupportedLanguages();
        assert languages != null;
        System.out.println("支持的语言: " + String.join(", ", languages));
    }
    
    @Test
    public void testGetPoolStatus() {
        // 测试获取容器池状态
        Map<String, LanguageContainerPool.PoolStatus> statusMap = containerPool.getAllPoolStatus();
        assert statusMap != null;
        
        for (Map.Entry<String, LanguageContainerPool.PoolStatus> entry : statusMap.entrySet()) {
            String language = entry.getKey();
            LanguageContainerPool.PoolStatus status = entry.getValue();
            System.out.printf("语言: %s, 当前大小: %d/%d, 可用: %d, 利用率: %.2f%%\n",
                    language, status.getCurrentSize(), status.getTargetSize(),
                    status.getAvailableSize(), status.getUtilizationRate() * 100);
        }
    }
    
    @Test
    public void testGetMetrics() {
        // 测试获取性能指标
        Map<String, ContainerPoolMetrics> metricsMap = containerPool.getAllMetrics();
        assert metricsMap != null;
        
        for (Map.Entry<String, ContainerPoolMetrics> entry : metricsMap.entrySet()) {
            String language = entry.getKey();
            ContainerPoolMetrics metrics = entry.getValue();
            System.out.printf("语言: %s, 获取次数: %d, 归还次数: %d, 故障次数: %d\n",
                    language, metrics.getTotalAcquired(), metrics.getTotalReturned(), metrics.getTotalFaults());
        }
    }
    
    @Test
    public void testHealthCheck() {
        // 测试健康检查
        boolean healthy = containerPool.isHealthy();
        System.out.println("容器池健康状态: " + (healthy ? "健康" : "不健康"));
    }
}
