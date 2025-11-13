package oj.oj_codesandbox.config;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 容器池指标收集器
 * 线程安全的指标统计
 */
public class ContainerPoolMetrics {
    
    // 容器获取相关指标
    private final AtomicLong totalAcquired = new AtomicLong(0);
    private final AtomicLong totalReturned = new AtomicLong(0);
    private final AtomicLong totalFaults = new AtomicLong(0);
    private final AtomicLong totalTimeouts = new AtomicLong(0);
    
    // 容器创建相关指标
    private final AtomicLong totalCreated = new AtomicLong(0);
    private final AtomicLong totalDestroyed = new AtomicLong(0);
    
    // 性能指标
    private final AtomicLong totalAcquireTime = new AtomicLong(0);
    private final AtomicLong totalExecuteTime = new AtomicLong(0);
    
    /**
     * 记录容器获取
     */
    public void recordAcquired(long acquireTimeMs) {
        totalAcquired.incrementAndGet();
        totalAcquireTime.addAndGet(acquireTimeMs);
    }
    
    /**
     * 记录容器归还
     */
    public void recordReturned() {
        totalReturned.incrementAndGet();
    }
    
    /**
     * 记录容器故障
     */
    public void recordFault() {
        totalFaults.incrementAndGet();
    }
    
    /**
     * 记录超时
     */
    public void recordTimeout() {
        totalTimeouts.incrementAndGet();
    }
    
    /**
     * 记录容器创建
     */
    public void recordCreated() {
        totalCreated.incrementAndGet();
    }
    
    /**
     * 记录容器销毁
     */
    public void recordDestroyed() {
        totalDestroyed.incrementAndGet();
    }
    
    /**
     * 记录执行时间
     */
    public void recordExecuteTime(long executeTimeMs) {
        totalExecuteTime.addAndGet(executeTimeMs);
    }
    
    // Getters
    public long getTotalAcquired() {
        return totalAcquired.get();
    }
    
    public long getTotalReturned() {
        return totalReturned.get();
    }
    
    public long getTotalFaults() {
        return totalFaults.get();
    }
    
    public long getTotalTimeouts() {
        return totalTimeouts.get();
    }
    
    public long getTotalCreated() {
        return totalCreated.get();
    }
    
    public long getTotalDestroyed() {
        return totalDestroyed.get();
    }
    
    public long getTotalAcquireTime() {
        return totalAcquireTime.get();
    }
    
    public long getTotalExecuteTime() {
        return totalExecuteTime.get();
    }
    
    /**
     * 获取平均获取时间
     */
    public double getAverageAcquireTime() {
        long acquired = totalAcquired.get();
        return acquired > 0 ? (double) totalAcquireTime.get() / acquired : 0.0;
    }
    
    /**
     * 获取平均执行时间
     */
    public double getAverageExecuteTime() {
        long acquired = totalAcquired.get();
        return acquired > 0 ? (double) totalExecuteTime.get() / acquired : 0.0;
    }
    
    /**
     * 获取故障率
     */
    public double getFaultRate() {
        long acquired = totalAcquired.get();
        return acquired > 0 ? (double) totalFaults.get() / acquired : 0.0;
    }
    
    /**
     * 获取超时率
     */
    public double getTimeoutRate() {
        long acquired = totalAcquired.get();
        return acquired > 0 ? (double) totalTimeouts.get() / acquired : 0.0;
    }
    
    /**
     * 重置所有指标
     */
    public void reset() {
        totalAcquired.set(0);
        totalReturned.set(0);
        totalFaults.set(0);
        totalTimeouts.set(0);
        totalCreated.set(0);
        totalDestroyed.set(0);
        totalAcquireTime.set(0);
        totalExecuteTime.set(0);
    }
}
