package oj.oj_codesandbox.pool;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import oj.oj_codesandbox.model.LanguageConfigInfo;
import oj.oj_codesandbox.config.ContainerPoolMetrics;
import oj.oj_codesandbox.utils.DockerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Iterator;

/**
 * 单语言容器池实现
 * 管理特定语言的容器生命周期
 */
public class LanguageContainerPool {
    
    private static final Logger log = LoggerFactory.getLogger(LanguageContainerPool.class);
    
    private final String language;
    private final LanguageConfigInfo config;
    private final DockerClient dockerClient;
    private final String hostCodePath;
    private final ContainerPoolMetrics metrics;
    
    // 容器池
    private final BlockingQueue<ContainerInfo> availableContainers;
    private final List<ContainerInfo> allContainers;
    private final AtomicInteger currentPoolSize;
    
    // 线程池
    private final ScheduledExecutorService healthCheckExecutor;
    private final ExecutorService containerCreationExecutor;
    
    // 状态控制
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    public LanguageContainerPool(String language, LanguageConfigInfo config, DockerClient dockerClient,
                                 String hostCodePath, ContainerPoolMetrics metrics) {
        this.language = language;
        this.config = config;
        this.dockerClient = dockerClient;
        this.hostCodePath = hostCodePath;
        this.metrics = metrics;
        
        this.availableContainers = new LinkedBlockingQueue<>();
        this.allContainers = new CopyOnWriteArrayList<>();
        this.currentPoolSize = new AtomicInteger(0);
        
        this.healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "health-check-" + language));
        this.containerCreationExecutor = Executors.newCachedThreadPool(
                r -> new Thread(r, "container-creation-" + language));
    }
    
    /**
     * 初始化容器池
     */
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            log.info("开始初始化 {} 容器池，目标大小: {}", language, config.poolSize());
            
            // 清理可能存在的冲突容器
            cleanupConflictingContainers();
            
            // 预创建容器
            for (int i = 0; i < config.poolSize(); i++) {
                containerCreationExecutor.submit(this::createContainer);
            }
            
            // 启动健康检查
            startHealthCheck();
            
            log.info("{} 容器池初始化完成", language);
        }
    }
    
    /**
     * 清理可能存在的冲突容器
     */
    private void cleanupConflictingContainers() {
        try {
            // 查找同名的容器并清理
            List<com.github.dockerjava.api.model.Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .withNameFilter(List.of(config.containerNamePrefix()))
                    .exec();
            
            for (com.github.dockerjava.api.model.Container container : containers) {
                try {
                    String containerId = container.getId();
                    String containerName = container.getNames()[0];
                    
                    // 停止并删除容器
                    dockerClient.stopContainerCmd(containerId).exec();
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                    
                    log.info("清理冲突容器: {} ({})", containerName, containerId);
                } catch (Exception e) {
                    log.warn("清理容器失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("清理冲突容器时发生异常: {}", e.getMessage());
        }
    }
    
    /**
     * 获取容器
     */
    public ContainerInfo getContainer(long timeoutMs) throws ContainerPoolException {
        if (shutdown.get()) {
            throw new ContainerPoolException.ContainerNotAvailableException("容器池已关闭");
        }
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            ContainerInfo container = availableContainers.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (container == null) {
                metrics.recordTimeout();
                throw new ContainerPoolException.ContainerNotAvailableException(
                        String.format("获取 %s 容器超时，等待时间: %dms", language, timeoutMs));
            }
            
            if (container.acquire()) {
                stopWatch.stop();
                metrics.recordAcquired(stopWatch.getLastTaskTimeMillis());
                log.debug("成功获取 {} 容器: {}", language, container.getContainerId());
                return container;
            } else {
                // 容器已被其他线程获取，递归重试
                return getContainer(timeoutMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ContainerPoolException.ContainerNotAvailableException("获取容器被中断");
        }
    }
    
    /**
     * 归还容器
     */
    public void returnContainer(ContainerInfo container) {
        if (container == null) {
            return;
        }
        
        container.release();
        metrics.recordReturned();
        
        if (container.isHealthy() && !shutdown.get()) {
            availableContainers.offer(container);
            log.debug("归还 {} 容器: {}", language, container.getContainerId());
        } else {
            // 容器不健康，需要替换
            replaceContainer(container);
        }
    }
    
    /**
     * 创建新容器
     */
    private void createContainer() {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                log.debug("开始创建 {} 容器 (尝试 {}/{})", language, retryCount + 1, maxRetries);
                
                // 拉取镜像
                DockerUtils.imageExists(dockerClient, config.imageName());
                if (!DockerUtils.imageExists(dockerClient, config.imageName())) {
                    log.warn("镜像 {} 不存在，跳过容器创建", config.imageName());
                    return;
                }
                
                // 构建容器配置
                HostConfig hostConfig = config.buildHostConfig(hostCodePath);
                String containerName = config.generateContainerName(currentPoolSize.get());
                
                // 创建容器
                CreateContainerResponse response = dockerClient.createContainerCmd(config.imageName())
                        .withName(containerName)
                        .withHostConfig(hostConfig)
                        .withEnv("LANG=C.UTF-8", "LC_ALL=C.UTF-8")
                        .withTty(true)
                        .withAttachStdin(true)
                        .withAttachStderr(true)
                        .withAttachStdout(true)
                        .exec();
                
                String containerId = response.getId();
                
                // 立即启动容器并保持运行状态（真正的容器复用！）
                dockerClient.startContainerCmd(containerId).exec();
                log.debug("启动容器: {}", containerId);
                
                ContainerInfo containerInfo = new ContainerInfo(containerId, language, config.imageName());
                
                allContainers.add(containerInfo);
                availableContainers.offer(containerInfo);
                currentPoolSize.incrementAndGet();
                metrics.recordCreated();
                
                log.info("成功创建并启动 {} 容器: {} (池大小: {}/{})", 
                        language, containerId, currentPoolSize.get(), config.poolSize());
                
                // 成功创建，跳出重试循环
                break;
                
            } catch (com.github.dockerjava.api.exception.ConflictException e) {
                retryCount++;
                log.warn("容器名称冲突，重试创建 {} 容器 (尝试 {}/{})", language, retryCount, maxRetries);
                if (retryCount >= maxRetries) {
                    log.error("创建 {} 容器失败，已达到最大重试次数: {}", language, e.getMessage());
                    metrics.recordFault();
                }
                // 等待一小段时间后重试
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                log.error("创建 {} 容器失败: {}", language, e.getMessage(), e);
                metrics.recordFault();
                break;
            }
        }
    }
    
    /**
     * 替换不健康的容器
     */
    private void replaceContainer(ContainerInfo oldContainer) {
        log.info("替换不健康的 {} 容器: {}", language, oldContainer.getContainerId());
        
        // 销毁旧容器
        destroyContainer(oldContainer);
        
        // 创建新容器
        if (!shutdown.get()) {
            containerCreationExecutor.submit(this::createContainer);
        }
    }
    
    /**
     * 销毁容器
     */
    private void destroyContainer(ContainerInfo container) {
        try {
            allContainers.remove(container);
            currentPoolSize.decrementAndGet();
            metrics.recordDestroyed();
            
            dockerClient.removeContainerCmd(container.getContainerId())
                    .withForce(true)
                    .exec();
            
            log.debug("销毁 {} 容器: {}", language, container.getContainerId());
        } catch (Exception e) {
            log.error("销毁 {} 容器失败: {}", language, e.getMessage(), e);
        }
    }
    
    /**
     * 启动健康检查
     */
    private void startHealthCheck() {
        healthCheckExecutor.scheduleWithFixedDelay(() -> {
            try {
                performHealthCheck();
            } catch (Exception e) {
                log.error("健康检查异常: {}", e.getMessage(), e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        log.debug("开始 {} 容器池健康检查", language);
        
        Iterator<ContainerInfo> iterator = allContainers.iterator();
        while (iterator.hasNext()) {
            ContainerInfo container = iterator.next();
            
            try {
                // 检查容器状态
                InspectContainerResponse inspect = dockerClient.inspectContainerCmd(container.getContainerId()).exec();
                if (inspect == null || inspect.getState() == null || !"running".equals(inspect.getState().getStatus())) {
                    log.warn("发现不健康的 {} 容器: {}", language, container.getContainerId());
                    container.markUnhealthy();
                    availableContainers.remove(container);
                }
                
                // 检查容器年龄，超过1小时的重置
                if (container.getAgeInMinutes() > 60) {
                    log.info("重置老化的 {} 容器: {}", language, container.getContainerId());
                    container.markUnhealthy();
                    availableContainers.remove(container);
                }
                
            } catch (Exception e) {
                log.warn("检查 {} 容器状态失败: {}", container.getContainerId(), e.getMessage());
                container.markUnhealthy();
                availableContainers.remove(container);
            }
        }
        
        // 清理不健康的容器
        allContainers.removeIf(container -> !container.isHealthy());
        
        // 确保池大小
        int currentSize = currentPoolSize.get();
        int targetSize = config.poolSize();
        if (currentSize < targetSize && !shutdown.get()) {
            int needCreate = targetSize - currentSize;
            for (int i = 0; i < needCreate; i++) {
                containerCreationExecutor.submit(this::createContainer);
            }
        }
        
        log.debug("{} 容器池健康检查完成，当前大小: {}/{}", language, currentSize, targetSize);
    }
    
    /**
     * 获取池状态
     */
    public PoolStatus getStatus() {
        return new PoolStatus(
                language,
                currentPoolSize.get(),
                config.poolSize(),
                availableContainers.size(),
                allContainers.stream().mapToInt(ContainerInfo::getUsageCount).sum()
        );
    }
    
    /**
     * 关闭容器池
     */
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            log.info("开始关闭 {} 容器池", language);
            
            // 停止健康检查
            healthCheckExecutor.shutdown();
            
            // 停止容器创建
            containerCreationExecutor.shutdown();
            
            // 销毁所有容器
            allContainers.forEach(this::destroyContainer);
            allContainers.clear();
            availableContainers.clear();
            currentPoolSize.set(0);
            
            log.info("{} 容器池已关闭", language);
        }
    }
    
    /**
     * 池状态信息
     */
    public static class PoolStatus {
        private final String language;
        private final int currentSize;
        private final int targetSize;
        private final int availableSize;
        private final int totalUsageCount;
        
        public PoolStatus(String language, int currentSize, int targetSize, int availableSize, int totalUsageCount) {
            this.language = language;
            this.currentSize = currentSize;
            this.targetSize = targetSize;
            this.availableSize = availableSize;
            this.totalUsageCount = totalUsageCount;
        }
        
        // Getters
        public String getLanguage() { return language; }
        public int getCurrentSize() { return currentSize; }
        public int getTargetSize() { return targetSize; }
        public int getAvailableSize() { return availableSize; }
        public int getTotalUsageCount() { return totalUsageCount; }
        public double getUtilizationRate() { 
            return targetSize > 0 ? (double) (targetSize - availableSize) / targetSize : 0.0; 
        }
    }
}
