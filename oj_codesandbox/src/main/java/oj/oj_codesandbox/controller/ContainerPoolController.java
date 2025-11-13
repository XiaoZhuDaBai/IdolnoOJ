package oj.oj_codesandbox.controller;

import oj.oj_codesandbox.pool.MultiLanguageDockerSandBoxPool;
import oj.oj_codesandbox.pool.LanguageContainerPool;
import oj.oj_codesandbox.config.ContainerPoolMetrics;
import oj.oj_codesandbox.comm.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 容器池监控控制器
 * 提供容器池状态查询和监控接口
 */
@RestController
@RequestMapping("/api/container-pool")
public class ContainerPoolController {
    
    @Autowired
    private MultiLanguageDockerSandBoxPool containerPool;
    
    /**
     * 获取所有容器池状态
     */
    @GetMapping("/status")
    public ResponseResult<Map<String, LanguageContainerPool.PoolStatus>> getAllPoolStatus() {
        try {
            Map<String, LanguageContainerPool.PoolStatus> statusMap = containerPool.getAllPoolStatus();
            return ResponseResult.success(statusMap);
        } catch (Exception e) {
            return ResponseResult.fail("获取容器池状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定语言的容器池状态
     */
    @GetMapping("/status/{language}")
    public ResponseResult<LanguageContainerPool.PoolStatus> getPoolStatus(@PathVariable String language) {
        try {
            LanguageContainerPool.PoolStatus status = containerPool.getPoolStatus(language);
            if (status == null) {
                return ResponseResult.fail("不支持的语言: " + language);
            }
            return ResponseResult.success(status);
        } catch (Exception e) {
            return ResponseResult.fail("获取容器池状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有容器池指标
     */
    @GetMapping("/metrics")
    public ResponseResult<Map<String, ContainerPoolMetrics>> getAllMetrics() {
        try {
            Map<String, ContainerPoolMetrics> metricsMap = containerPool.getAllMetrics();
            return ResponseResult.success(metricsMap);
        } catch (Exception e) {
            return ResponseResult.fail("获取容器池指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定语言的容器池指标
     */
    @GetMapping("/metrics/{language}")
    public ResponseResult<ContainerPoolMetrics> getMetrics(@PathVariable String language) {
        try {
            ContainerPoolMetrics metrics = containerPool.getMetrics(language);
            if (metrics == null) {
                return ResponseResult.fail("不支持的语言: " + language);
            }
            return ResponseResult.success(metrics);
        } catch (Exception e) {
            return ResponseResult.fail("获取容器池指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseResult<Boolean> healthCheck() {
        try {
            boolean healthy = containerPool.isHealthy();
            return ResponseResult.success(healthy);
        } catch (Exception e) {
            return ResponseResult.fail("健康检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取支持的编程语言
     */
    @GetMapping("/languages")
    public ResponseResult<String[]> getSupportedLanguages() {
        try {
            String[] languages = containerPool.getSupportedLanguages();
            return ResponseResult.success(languages);
        } catch (Exception e) {
            return ResponseResult.fail("获取支持语言失败: " + e.getMessage());
        }
    }
    
    /**
     * 预热容器池
     */
    @PostMapping("/warmup")
    public ResponseResult<String> warmup() {
        try {
            containerPool.warmup();
            return ResponseResult.success("容器池预热完成");
        } catch (Exception e) {
            return ResponseResult.fail("容器池预热失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取初始化状态
     */
    @GetMapping("/init-status")
    public ResponseResult<Map<String, Object>> getInitializationStatus() {
        try {
            Map<String, Object> status = containerPool.getInitializationStatus();
            return ResponseResult.success(status);
        } catch (Exception e) {
            return ResponseResult.fail("获取初始化状态失败: " + e.getMessage());
        }
    }
}
