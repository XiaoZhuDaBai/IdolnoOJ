package oj.oj_codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.MemoryStatsConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.Statistics;
import oj.oj_codesandbox.model.ExecuteCodeResponse;
import oj.oj_codesandbox.model.ExecuteMessage;
import oj.oj_codesandbox.judge.entity.JudgeInfo;
import oj.oj_codesandbox.utils.DockerUtils;
import oj.oj_codesandbox.pool.MultiLanguageDockerSandBoxPool;
import oj.oj_codesandbox.pool.ContainerInfo;
import oj.oj_codesandbox.pool.ContainerPoolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * 代码沙箱模板
 * 支持容器池的代码执行模板
 */
@Component
public class CodeSandboxTemplate {
    private static final Logger log = LoggerFactory.getLogger(CodeSandboxTemplate.class);
    private static final int MAX_RUN_TIME = 10000; // 单位毫秒
    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String GLOBAL_CODE_DIR_NAME = "UserCode";
    private static final String[] CMD = {"sh", "-c", ""};

    @Autowired
    private MultiLanguageDockerSandBoxPool containerPool;

    @PostConstruct
    public void checkContainerPool() {
        if (containerPool == null) {
            log.error("容器池注入失败！");
        } else {
            log.info("容器池注入成功: {}", containerPool.getClass().getSimpleName());
        }
    }

    /**
     * 保存成文件
     */
    public File saveCodeToFile(String code, String codeFileName) {
        String globalCodeDirPath = USER_DIR + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodeDirPath)) {
            FileUtil.mkdir(globalCodeDirPath);
        }

        String userCodeFilePath = globalCodeDirPath + File.separator + UUID.randomUUID();
        String userCodePath = userCodeFilePath + File.separator + codeFileName;
        return FileUtil.writeUtf8String(code, userCodePath);
    }

    /**
     * 解释型语言所用
     */
    public List<ExecuteMessage> runFile(File userCodeFile, String problemId, String imageName, String languageCmd) {
        return executeWithTestCases(userCodeFile, problemId, imageName, languageCmd, false, null, null);
    }

    /**
     * 使用容器池的解释型语言执行
     */
    public List<ExecuteMessage> runFileWithPool(File userCodeFile, String problemId, String language, String languageCmd) {
        return executeWithTestCasesUsingPool(userCodeFile, problemId, language, languageCmd, false, null, null);
    }

    /**
     * 带用户输入的运行
     */
    public List<ExecuteMessage> runFileWithInput(File userCodeFile, File userInputFile, String imageName, String languageCmd) {
        return executeWithUserInput(userCodeFile, userInputFile, imageName, languageCmd, false, null, null);
    }

    /**
     * 编译语言所用
     */
    public List<ExecuteMessage> compileAndRunFile(File userCodeFile, String problemId, String imageName, String compileCmd, String languageCmd, String codeFileName) {
        return executeWithTestCases(userCodeFile, problemId, imageName, languageCmd, true, compileCmd, codeFileName);
    }

    /**
     * 使用容器池的编译语言执行
     */
    public List<ExecuteMessage> compileAndRunFileWithPool(File userCodeFile, String problemId, String language, String compileCmd, String languageCmd, String codeFileName) {
        return executeWithTestCasesUsingPool(userCodeFile, problemId, language, languageCmd, true, compileCmd, codeFileName);
    }

    /**
     * 带用户输入的编译运行
     */
    public List<ExecuteMessage> compileAndRunFileWithInput(File userCodeFile, File userInputFile, String imageName, String compileCmd, String languageCmd, String codeFileName) {
        return executeWithUserInput(userCodeFile, userInputFile, imageName, languageCmd, true, compileCmd, codeFileName);
    }

    /**
     * 使用容器池的带用户输入的运行
     */
    public List<ExecuteMessage> runFileWithInputUsingPool(File userCodeFile, File userInputFile, String language, String languageCmd) {
        return executeWithUserInputUsingPool(userCodeFile, userInputFile, language, languageCmd, false, null, null);
    }

    /**
     * 使用容器池的带用户输入的编译运行
     */
    public List<ExecuteMessage> compileAndRunFileWithInputUsingPool(File userCodeFile, File userInputFile, String language, String compileCmd, String languageCmd, String codeFileName) {
        return executeWithUserInputUsingPool(userCodeFile, userInputFile, language, languageCmd, true, compileCmd, codeFileName);
    }

    /**
     * 执行测试用例的通用方法
     */
    private List<ExecuteMessage> executeWithTestCases(File userCodeFile, String problemId, String imageName, String languageCmd, boolean needCompile, String compileCmd, String codeFileName) {
        String userCodeDir = userCodeFile.getParentFile().getAbsolutePath();
        DockerClient dockerClient = DockerUtils.createDockerClient();
        PullDockerImage(dockerClient, imageName);

        List<ExecuteMessage> executeMessages = new ArrayList<>();
        String inputFileParentPath = Paths.get(USER_DIR, "Problems", problemId, "input").toString();
        long inputFileCount = getDirChildFileCount(inputFileParentPath);
        String MainPath = "/app" + File.separator + userCodeDir.substring(USER_DIR.length() + 1);

        for (int i = 1; i <= inputFileCount; i++) {
            String inputFile = Paths.get("/app", "Problems", problemId, "input", String.format("input%d.txt", i)).toString();
            String newLanguageCmd = String.format(languageCmd, MainPath, inputFile);

            String finalCmd = needCompile ?
                    String.format(compileCmd, MainPath + File.separator + codeFileName) + " && " + newLanguageCmd :
                    newLanguageCmd;

            ExecuteMessage message = executeContainer(dockerClient, imageName, finalCmd, MainPath);

            // 检验结果
            String answerFilePath = Paths.get(USER_DIR, "Problems", problemId, "answer", "answer" + i + ".txt").toString();
            message.setCorrect(check(message.getMessage(), answerFilePath));

            executeMessages.add(message);
        }

        return executeMessages;
    }

    /**
     * 使用容器池执行测试用例的通用方法
     */
    private List<ExecuteMessage> executeWithTestCasesUsingPool(File userCodeFile, String problemId, String language, String languageCmd, boolean needCompile, String compileCmd, String codeFileName) {
        String userCodeDir = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessages = new ArrayList<>();
        String inputFileParentPath = Paths.get(USER_DIR, "Problems", problemId, "input").toString();
        long inputFileCount = getDirChildFileCount(inputFileParentPath);
        String MainPath = "/app" + File.separator + userCodeDir.substring(USER_DIR.length() + 1);

        for (int i = 1; i <= inputFileCount; i++) {
            String inputFile = Paths.get("/app", "Problems", problemId, "input", String.format("input%d.txt", i)).toString();
            String newLanguageCmd = String.format(languageCmd, MainPath, inputFile);

            String finalCmd = needCompile ?
                    String.format(compileCmd, MainPath + File.separator + codeFileName) + " && " + newLanguageCmd :
                    newLanguageCmd;

            ExecuteMessage message = executeContainerUsingPool(language, finalCmd, MainPath);

            // 检验结果
            String answerFilePath = Paths.get(USER_DIR, "Problems", problemId, "answer", "answer" + i + ".txt").toString();
            message.setCorrect(check(message.getMessage(), answerFilePath));

            executeMessages.add(message);
        }

        return executeMessages;
    }

    /**
     * 执行用户输入的通用方法
     */
    private List<ExecuteMessage> executeWithUserInput(File userCodeFile, File userInputFile, String imageName, String languageCmd, boolean needCompile, String compileCmd, String codeFileName) {
        String userCodeDir = userCodeFile.getParentFile().getAbsolutePath();
        String userInputDir = userInputFile.getAbsolutePath();
        DockerClient dockerClient = DockerUtils.createDockerClient();
        PullDockerImage(dockerClient, imageName);

        String MainPath = "/app" + File.separator + userCodeDir.substring(USER_DIR.length() + 1);
        String InputPath = "/app" + File.separator + userInputDir.substring(USER_DIR.length() + 1);
        String newLanguageCmd = String.format(languageCmd, MainPath, InputPath);

        String finalCmd = needCompile ?
                String.format(compileCmd, MainPath + File.separator + codeFileName) + " && " + newLanguageCmd :
                newLanguageCmd;

        ExecuteMessage message = executeContainer(dockerClient, imageName, finalCmd, MainPath);
        return List.of(message);
    }

    /**
     * 使用容器池执行用户输入的通用方法
     */
    private List<ExecuteMessage> executeWithUserInputUsingPool(File userCodeFile, File userInputFile, String language, String languageCmd, boolean needCompile, String compileCmd, String codeFileName) {
        String userCodeDir = userCodeFile.getParentFile().getAbsolutePath();
        String userInputDir = userInputFile.getAbsolutePath();

        String MainPath = "/app" + File.separator + userCodeDir.substring(USER_DIR.length() + 1);
        String InputPath = "/app" + File.separator + userInputDir.substring(USER_DIR.length() + 1);
        String newLanguageCmd = String.format(languageCmd, MainPath, InputPath);

        String finalCmd = needCompile ?
                String.format(compileCmd, MainPath + File.separator + codeFileName) + " && " + newLanguageCmd :
                newLanguageCmd;

        ExecuteMessage message = executeContainerUsingPool(language, finalCmd, MainPath);
        return List.of(message);
    }

    /**
     * 执行容器的核心方法 - 提取的公共逻辑
     */
    private ExecuteMessage executeContainer(DockerClient dockerClient, String imageName, String command, String mainPath) {
        CMD[CMD.length - 1] = command;
        ExecuteMessage message = new ExecuteMessage();

        // 创建容器
        CreateContainerResponse response = DockerUtils.createResponsePlus(dockerClient, imageName, CMD, USER_DIR);
        String containerId = response.getId();
        log.info("[CONTAINER CREATE] ID: {}, Image: {}", containerId, imageName);

        try {
            // 启动容器
            dockerClient.startContainerCmd(containerId).exec();

            // 等待容器完全启动
            Thread.sleep(1000);

            // 异步获取执行前的内存基线（新容器启动后的基线）
            CompletableFuture<Long> baselineFuture = CompletableFuture.supplyAsync(() -> {
                Long baseline = getContainerMemoryUsage(dockerClient, containerId);
                log.info("[MEMORY BASELINE] 容器启动后内存使用: {}B ({}KB)", baseline, baseline / 1024);
                return baseline;
            });

            // 监控资源使用
            Long[] maxMemoryUsage = {0L};
            AtomicBoolean stopMonitoring = new AtomicBoolean(false);

            // 异步启动监控，等待基线获取完成
            CompletableFuture.runAsync(() -> {
                try {
                    Long baselineMemory = baselineFuture.get(1000, TimeUnit.MILLISECONDS);
                    monitorContainerResourcesWithBaseline(dockerClient, containerId, maxMemoryUsage, baselineMemory, stopMonitoring);
                } catch (Exception e) {
                    log.warn("获取内存基线失败，使用默认值0: {}", e.getMessage());
                    monitorContainerResourcesWithBaseline(dockerClient, containerId, maxMemoryUsage, 0L, stopMonitoring);
                }
            });

            // 执行并等待结果
            executeAndWait(dockerClient, containerId, message, maxMemoryUsage);

            // 收集容器状态
            collectContainerStatus(dockerClient, containerId, message);

            // 获取执行结果
            collectExecutionOutput(dockerClient, containerId, message);

        } catch (Exception e) {
            message.setExitValue(-1L);
            message.setErrorMessage("容器异常: " + e.getMessage());
            log.error("容器执行异常: {}", e.getMessage(), e);
        } finally {
            // 清理容器
            cleanupContainer(dockerClient, containerId);
        }

        return message;
    }

    /**
     * 使用容器池执行容器的核心方法
     * 真正的容器复用：在运行中的容器内执行命令
     */
    private ExecuteMessage executeContainerUsingPool(String language, String command, String mainPath) {
        CMD[CMD.length - 1] = command;
        ExecuteMessage message = new ExecuteMessage();
        ContainerInfo containerInfo = null;
        DockerClient dockerClient = null;

        try {
            // 检查容器池是否可用
            log.info("检查容器池状态: {}", containerPool);
            if (containerPool == null) {
                log.error("容器池未初始化，无法执行代码");
                log.error("当前 CodeSandboxTemplate 实例: {}", this);
                message.setExitValue(-1L);
                message.setErrorMessage("容器池未初始化");
                return message;
            }

            // 从容器池获取运行中的容器
            containerInfo = containerPool.getContainer(language, 5000);
            String containerId = containerInfo.getContainerId();
            log.info("[CONTAINER POOL] 获取运行中容器: {} (语言: {})", containerId, language);

            // 创建DockerClient
            dockerClient = DockerUtils.createDockerClient();

            // 在运行中的容器内执行命令（真正的复用！）
            String execId = dockerClient.execCreateCmd(containerId)
                    .withCmd(CMD)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec()
                    .getId();

            log.debug("[CONTAINER POOL] 在容器 {} 中执行命令: {}", containerId, String.join(" ", CMD));

            // 轻量级内存监控 - 只记录基线，不持续监控
            Long[] maxMemoryUsage = {0L};
            StringBuilder realTimeOutput = new StringBuilder();

            // 快速获取内存基线（同步，减少延迟）
            Long baselineMemory = getContainerMemoryUsage(dockerClient, containerId);
            log.info("[MEMORY BASELINE] 执行前内存使用: {}B ({}KB)", baselineMemory, baselineMemory / 1024);

            // 执行命令并收集输出（不包含内存基线时间）
            executeAndCollectOutput(dockerClient, execId, message, maxMemoryUsage, realTimeOutput);

            // 快速最终内存检查 - 同步执行，减少延迟
            try {
                // 使用进程级内存监控（更精确）
                Long processMemory = getProcessMemoryUsage(dockerClient, containerId);
                if (processMemory > 0) {
                    // 使用进程内存作为最终结果
                    maxMemoryUsage[0] = processMemory;
                    log.info("[MEMORY PEAK] 进程级内存使用: {}B ({}KB)", processMemory, processMemory / 1024);
                } else {
                    // 降级到容器级监控
                    Long finalMemory = getContainerMemoryUsage(dockerClient, containerId);
                    Long finalDelta = finalMemory - baselineMemory;
                    if (finalDelta > maxMemoryUsage[0]) {
                        maxMemoryUsage[0] = finalDelta;
                        log.info("[MEMORY PEAK] 容器级内存增量: {}B ({}KB)", finalDelta, finalDelta / 1024);
                    }
                }

                // 更新message中的内存使用量
                message.setMemory(maxMemoryUsage[0]);

                log.info("[MEMORY FINAL] 最终内存统计 - 基线: {}B, 峰值使用: {}B",
                        baselineMemory, maxMemoryUsage[0]);
            } catch (Exception e) {
                log.debug("最终内存检查失败: {}", e.getMessage());
            }

            // 设置输出结果
            String result = realTimeOutput.toString().replaceAll("\r", "");
            Long exitValue = message.getExitValue();
            if (exitValue == null || exitValue != 0) {
                message.setErrorMessage(result);
            } else {
                message.setMessage(result);
            }
            log.info("输出结果：{}", result);

        } catch (ContainerPoolException e) {
            message.setExitValue(-1L);
            message.setErrorMessage("容器池异常: " + e.getMessage());
            log.error("容器池异常: {}", e.getMessage(), e);
        } catch (Exception e) {
            message.setExitValue(-1L);
            message.setErrorMessage("容器执行异常: " + e.getMessage());
            log.error("容器执行异常: {}", e.getMessage(), e);
        } finally {
            // 归还容器到池中（容器继续运行，供下次使用）
            if (containerInfo != null) {
                containerPool.returnContainer(containerInfo);
                log.debug("[CONTAINER POOL] 归还运行中容器: {} (语言: {})", containerInfo.getContainerId(), language);
            }
        }

        return message;
    }

    /**
     * 执行命令并收集输出
     */
    private void executeAndCollectOutput(DockerClient dockerClient, String execId, ExecuteMessage message, Long[] maxMemoryUsage, StringBuilder realTimeOutput) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        CompletableFuture<Void> completion = CompletableFuture.runAsync(() -> {
            try {
                // 执行命令并收集输出
                dockerClient.execStartCmd(execId)
                        .exec(new ResultCallback.Adapter<Frame>() {
                            @Override
                            public void onNext(Frame frame) {
                                String output = new String(frame.getPayload());
                                realTimeOutput.append(output);
                                // 实时打印输出，方便调试
                                if (!output.trim().isEmpty()) {
                                    log.info("[REAL-TIME OUTPUT] {}", output.trim());
                                }
                                super.onNext(frame);
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                log.warn("命令执行失败: {}", throwable.getMessage());
                                super.onError(throwable);
                            }
                        }).awaitCompletion();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            // 等待命令执行完成
            completion.get(MAX_RUN_TIME, TimeUnit.MILLISECONDS);

            // 获取退出码
            try {
                InspectExecResponse inspectResponse = dockerClient.inspectExecCmd(execId).exec();
                Long exitCode = inspectResponse.getExitCodeLong();
                message.setExitValue(exitCode != null ? exitCode : 0L);
                log.info("命令执行完成，退出码: {}", message.getExitValue());
            } catch (Exception e) {
                log.warn("获取退出码失败，使用默认值0: {}", e.getMessage());
                message.setExitValue(0L);
            }

        } catch (TimeoutException e) {
            // 超时处理
            message.setExitValue(-1L);
            message.setErrorMessage("执行超时");
            log.warn("命令执行超时");
        } catch (ExecutionException | InterruptedException e) {
            message.setExitValue(-1L);
            message.setErrorMessage("执行异常: " + e.getMessage());
            log.error("命令执行异常: {}", e.getMessage(), e);
        } finally {
            stopWatch.stop();
            // 注意：这里不设置内存，因为内存统计在方法外部进行
            message.setTime(stopWatch.getLastTaskTimeMillis());
            log.info("执行时间：{}ms", message.getTime());
            // 内存统计会在外部方法中设置
        }
    }

    /**
     * 执行exec命令并等待结果
     */
    private void executeAndWaitForExec(DockerClient dockerClient, String execId, ExecuteMessage message, Long[] maxMemoryUsage) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        CompletableFuture<Void> completion = CompletableFuture.runAsync(() -> {
            try {
                // 执行命令
                dockerClient.execStartCmd(execId)
                        .exec(new ResultCallback.Adapter<Frame>() {
                            @Override
                            public void onNext(Frame frame) {
                                // 这里可以实时处理输出
                                super.onNext(frame);
                            }
                        }).awaitCompletion();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            // 等待命令执行完成
            completion.get(MAX_RUN_TIME, TimeUnit.MILLISECONDS);

            // 获取退出码
            try {
                InspectExecResponse inspectResponse = dockerClient.inspectExecCmd(execId).exec();
                Long exitCode = inspectResponse.getExitCodeLong();
                message.setExitValue(exitCode != null ? exitCode : 0L);
                log.info("命令执行完成，退出码: {}", message.getExitValue());
            } catch (Exception e) {
                log.warn("获取退出码失败，使用默认值0: {}", e.getMessage());
                message.setExitValue(0L);
            }

        } catch (TimeoutException e) {
            // 超时处理
            message.setExitValue(-1L);
            message.setErrorMessage("执行超时");
            log.warn("命令执行超时");
        } catch (ExecutionException | InterruptedException e) {
            message.setExitValue(-1L);
            message.setErrorMessage("执行异常: " + e.getMessage());
            log.error("命令执行异常: {}", e.getMessage(), e);
        } finally {
            stopWatch.stop();
            message.setMemory(maxMemoryUsage[0]);
            message.setTime(stopWatch.getLastTaskTimeMillis());
            log.info("执行时间：{}ms", message.getTime());
            log.info("占用内存：{}B", message.getMemory());
        }
    }



    /**
     * 监控容器资源使用（重载方法，保持向后兼容）
     */
    private void monitorContainerResources(DockerClient dockerClient, String containerId, Long[] maxMemoryUsage) {
        monitorContainerResources(dockerClient, containerId, maxMemoryUsage, new AtomicBoolean(false));
    }

    /**
     * 获取容器当前内存使用量（超轻量级版本，最小延迟）
     */
    private Long getContainerMemoryUsage(DockerClient dockerClient, String containerId) {
        try {
            // 使用同步方式获取容器统计信息
            final Long[] memoryUsage = {0L};
            final AtomicBoolean completed = new AtomicBoolean(false);

            dockerClient.statsCmd(containerId)
                    .withNoStream(true)
                    .exec(new ResultCallback.Adapter<Statistics>() {
                        @Override
                        public void onNext(Statistics statistics) {
                            if (statistics != null && statistics.getMemoryStats() != null) {
                                MemoryStatsConfig memoryStats = statistics.getMemoryStats();
                                if (memoryStats.getUsage() != null) {
                                    memoryUsage[0] = memoryStats.getUsage();
                                }
                            }
                            completed.set(true);
                            super.onNext(statistics);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            log.warn("获取容器内存统计失败: {}", throwable.getMessage());
                            completed.set(true);
                            super.onError(throwable);
                        }
                    });

            // 超轻量级等待：最多等待200ms，每10ms检查一次
            long startTime = System.currentTimeMillis();
            while (!completed.get() && (System.currentTimeMillis() - startTime) < 200) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (!completed.get()) {
                log.debug("获取内存基线超时，使用默认值0");
            }

            return memoryUsage[0];
        } catch (Exception e) {
            log.warn("获取容器内存使用量失败: {}", e.getMessage());
        }
        return 0L;
    }

    /**
     * 监控容器资源使用（基于基线的增量监控）
     */
    private void monitorContainerResourcesWithBaseline(DockerClient dockerClient, String containerId, Long[] maxMemoryUsage, Long baselineMemory, AtomicBoolean stopMonitoring) {
        try {
            dockerClient.statsCmd(containerId)
                    .withNoStream(false)
                    .exec(new ResultCallback.Adapter<Statistics>() {
                        @Override
                        public void onNext(Statistics statistics) {
                            try {
                                // 检查是否需要停止监控
                                if (stopMonitoring.get()) {
                                    log.debug("内存监控被停止");
                                    return;
                                }

                                // 获取当前内存使用情况
                                Long currentUsage = null;
                                if (statistics.getMemoryStats() != null) {
                                    MemoryStatsConfig memoryStats = statistics.getMemoryStats();
                                    if (memoryStats.getUsage() != null) {
                                        currentUsage = memoryStats.getUsage();
                                    }
                                }

                                if (currentUsage != null && currentUsage > 0) {
                                    // 计算相对于基线的内存增量
                                    Long memoryDelta = currentUsage - baselineMemory;
                                    if (memoryDelta > 0) {
                                        if (memoryDelta > maxMemoryUsage[0]) {
                                            maxMemoryUsage[0] = memoryDelta;
                                        }
                                        log.info("容器内存增量：{}B ({}KB) [总使用: {}B]",
                                                memoryDelta, memoryDelta / 1024, currentUsage);
                                    } else if (memoryDelta < 0) {
                                        // 内存被回收，但记录当前状态
                                        log.debug("容器内存增量：{}B (已回收) [总使用: {}B]", memoryDelta, currentUsage);
                                    } else {
                                        // 内存无变化
                                        log.debug("容器内存增量：0B (无变化) [总使用: {}B]", currentUsage);
                                    }
                                } else {
                                    log.debug("内存消耗：暂无数据");
                                }

                                // 获取CPU使用情况
                                if (statistics.getCpuStats() != null && statistics.getCpuStats().getCpuUsage() != null) {
                                    log.debug("CPU：{}", statistics.getCpuStats().getCpuUsage());
                                } else {
                                    log.debug("CPU：暂无数据");
                                }
                            } catch (Exception e) {
                                log.warn("处理容器统计信息时发生异常: {}", e.getMessage());
                            }
                            super.onNext(statistics);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            log.warn("容器统计信息获取失败: {}", throwable.getMessage());
                            super.onError(throwable);
                        }
                    });
        } catch (Exception e) {
            log.warn("启动容器统计监控失败: {}", e.getMessage());
        }
    }

    /**
     * 获取进程级内存使用（优化版本）
     * 使用 /proc/meminfo 和 /proc/self/status 获取更准确的内存信息
     */
    private Long getProcessMemoryUsage(DockerClient dockerClient, String containerId) {
        try {
            // 使用更准确的内存监控命令
            String execId = dockerClient.execCreateCmd(containerId)
                    .withCmd("sh", "-c", "cat /proc/self/status | grep VmRSS | awk '{print $2}'")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec()
                    .getId();

            final Long[] memoryUsage = {0L};
            final AtomicBoolean completed = new AtomicBoolean(false);
            StringBuilder output = new StringBuilder();

            dockerClient.execStartCmd(execId)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            output.append(new String(frame.getPayload()));
                            completed.set(true);
                            super.onNext(frame);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            completed.set(true);
                            super.onError(throwable);
                        }
                    });

            // 超轻量级等待：最多等待200ms，每10ms检查一次
            long startTime = System.currentTimeMillis();
            while (!completed.get() && (System.currentTimeMillis() - startTime) < 200) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 解析输出结果
            String result = output.toString().trim();
            if (!result.isEmpty() && result.matches("\\d+")) {
                memoryUsage[0] = Long.parseLong(result) * 1024; // KB转字节
                log.debug("进程内存使用(VmRSS): {}B ({}KB)", memoryUsage[0], memoryUsage[0] / 1024);
            } else {
                // 降级方案：使用 ps 命令
                return getProcessMemoryUsageFallback(dockerClient, containerId);
            }

            return memoryUsage[0];
        } catch (Exception e) {
            log.warn("获取进程内存使用失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 降级方案：使用 ps 命令获取进程内存
     */
    private Long getProcessMemoryUsageFallback(DockerClient dockerClient, String containerId) {
        try {
            String execId = dockerClient.execCreateCmd(containerId)
                    .withCmd("sh", "-c", "ps -o rss= -p $$ | tr -d ' '")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec()
                    .getId();

            final Long[] memoryUsage = {0L};
            final AtomicBoolean completed = new AtomicBoolean(false);
            StringBuilder output = new StringBuilder();

            dockerClient.execStartCmd(execId)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            output.append(new String(frame.getPayload()));
                            completed.set(true);
                            super.onNext(frame);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            completed.set(true);
                            super.onError(throwable);
                        }
                    });

            long startTime = System.currentTimeMillis();
            while (!completed.get() && (System.currentTimeMillis() - startTime) < 150) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            String result = output.toString().trim();
            if (!result.isEmpty() && result.matches("\\d+")) {
                memoryUsage[0] = Long.parseLong(result) * 1024; // KB转字节
                log.debug("进程内存使用(ps): {}B ({}KB)", memoryUsage[0], memoryUsage[0] / 1024);
            }

            return memoryUsage[0];
        } catch (Exception e) {
            log.warn("降级方案获取进程内存失败: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 监控容器资源使用（原始方法，保持向后兼容）
     */
    private void monitorContainerResources(DockerClient dockerClient, String containerId, Long[] maxMemoryUsage, AtomicBoolean stopMonitoring) {
        try {
            dockerClient.statsCmd(containerId)
                    .withNoStream(false)
                    .exec(new ResultCallback.Adapter<Statistics>() {
                        @Override
                        public void onNext(Statistics statistics) {
                            try {
                                // 检查是否需要停止监控
                                if (stopMonitoring.get()) {
                                    log.debug("内存监控被停止");
                                    return;
                                }

                                // 获取内存使用情况 - 改进获取方式
                                Long usage = null;
                                if (statistics.getMemoryStats() != null) {
                                    MemoryStatsConfig memoryStats = statistics.getMemoryStats();
                                    // 尝试多种方式获取内存使用量
                                    if (memoryStats.getUsage() != null) {
                                        usage = memoryStats.getUsage();
                                    }
                                }

                                if (usage != null && usage > 0) {
                                    if (usage > maxMemoryUsage[0]) {
                                        maxMemoryUsage[0] = usage;
                                    }
                                    log.info("内存消耗：{}B ({}KB)", usage, usage / 1024);
                                } else {
                                    log.debug("内存消耗：暂无数据");
                                }

                                // 获取CPU使用情况
                                if (statistics.getCpuStats() != null && statistics.getCpuStats().getCpuUsage() != null) {
                                    log.debug("CPU：{}", statistics.getCpuStats().getCpuUsage());
                                } else {
                                    log.debug("CPU：暂无数据");
                                }
                            } catch (Exception e) {
                                log.warn("处理容器统计信息时发生异常: {}", e.getMessage());
                            }
                            super.onNext(statistics);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            log.warn("容器统计信息获取失败: {}", throwable.getMessage());
                            super.onError(throwable);
                        }
                    });
        } catch (Exception e) {
            log.warn("启动容器统计监控失败: {}", e.getMessage());
        }
    }

    /**
     * 执行并等待容器完成
     */
    private void executeAndWait(DockerClient dockerClient, String containerId, ExecuteMessage message, Long[] maxMemoryUsage) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        CompletableFuture<Void> completion = CompletableFuture.runAsync(() -> {
            try {
                dockerClient.waitContainerCmd(containerId)
                        .exec(new WaitContainerResultCallback())
                        .awaitCompletion();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            completion.get(MAX_RUN_TIME, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            dockerClient.killContainerCmd(containerId).exec();
            message.setExitValue(-1L);
            message.setErrorMessage("执行超时");
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            stopWatch.stop();
            message.setMemory(maxMemoryUsage[0]);
            message.setTime(stopWatch.getLastTaskTimeMillis());
            log.info("执行时间：{}ms", message.getTime());
            log.info("占用内存：{}B", message.getMemory());
        }
    }

    /**
     * 收集容器状态
     */
    private void collectContainerStatus(DockerClient dockerClient, String containerId, ExecuteMessage message) {
        try {
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            if (inspect != null && inspect.getState() != null) {
                Long exitCode = inspect.getState().getExitCodeLong();
                message.setExitValue(exitCode);
                Boolean oomKilled = inspect.getState().getOOMKilled();
                String error = inspect.getState().getError();
                String status = inspect.getState().getStatus();

                log.info("容器退出码: {}", exitCode);
                log.info("OOMKilled: {}", oomKilled);
                log.info("Error: {}", error);
                log.info("Status: {}", status);

                // 检测OOM状态
                if (Boolean.TRUE.equals(oomKilled)) {
                    message.setExitValue(-1L);
                    message.setErrorMessage("内存超限 (OOM Killed)");
                    log.warn("容器因内存超限被杀死: {}", containerId);
                } else if (exitCode != null && exitCode != 0) {
                    message.setErrorMessage(error != null ? error : "程序异常退出");
                }

                message.setErrorMessage(error);
            } else {
                message.setExitValue(-1L);
                message.setErrorMessage("无法获取容器状态");
            }
        } catch (Exception e) {
            message.setExitValue(-1L);
            message.setErrorMessage("inspectContainerCmd异常: " + e.getMessage());
            throw new RuntimeException("容器异常退出", e);
        }
    }

    /**
     * 收集执行输出
     */
    private void collectExecutionOutput(DockerClient dockerClient, String containerId, ExecuteMessage message) {
        StringBuilder output = new StringBuilder();
        try {
            dockerClient.logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(false)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            output.append(new String(frame.getPayload()));
                        }
                    }).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String result = output.toString().replaceAll("\r", "");
        if (message.getExitValue() != 0) {
            message.setErrorMessage(result.substring(result.indexOf("error")));
        } else {
            message.setMessage(result);
        }
        log.info("输出结果：{}", result);
    }

    /**
     * 清理容器
     */
    private void cleanupContainer(DockerClient dockerClient, String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId)
                    .withForce(true)
                    .exec();
        } catch (Exception e) {
            log.error("清理容器失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取输出响应
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();

        List<String> outputList = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        long maxTime = 0;
        long maxMemory = 0;
        int size = executeMessageList.size();
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setCorrect(new boolean[size]);

        for (int i = 0; i < executeMessageList.size(); i++) {
            judgeInfo.getCorrect()[i] = true;
            ExecuteMessage message = executeMessageList.get(i);
            maxTime = Math.max(maxTime, message.getTime());
            maxMemory = Math.max(maxMemory, message.getMemory());

            Long exitValue = message.getExitValue();
            if (exitValue != null && exitValue != 0) {
                errorMessages.add(String.format(message.getErrorMessage()));
                response.setExitCode(exitValue);
                break;
            }

            if (!message.isCorrect()) {
                judgeInfo.getCorrect()[i] = false;
            }
            outputList.add(message.getMessage());
        }

        judgeInfo.setTime(maxTime);
        judgeInfo.setMemory(maxMemory);
        judgeInfo.setErrorMessages(errorMessages);
        response.setJudgeInfo(judgeInfo);
        response.setOutputList(outputList);
        return response;
    }

    /**
     * 检查结果
     */
    public boolean check(String outputStr, String answerFilePath) {
        try {
            if (outputStr == null || outputStr.trim().isEmpty()) {
                log.warn("输出结果为空，无法进行校验");
                return false;
            }
            List<String> output = List.of(outputStr.split("\n"));
            List<String> answer = Files.readAllLines(Paths.get(answerFilePath));
            if (output.size() != answer.size()) {
                return false;
            }
            for (int i = 0; i < output.size(); i++) {
                if (!output.get(i).equals(answer.get(i))) {
                    return false;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * 获取目录子文件数量
     */
    public long getDirChildFileCount(String inputFileParentPath) {
        Path path = Paths.get(inputFileParentPath);
        try (Stream<Path> walk = Files.walk(path)) {
            return walk.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 拉取Docker镜像
     */
    public void PullDockerImage(DockerClient dockerClient, String imageName) {
        boolean imageExists = DockerUtils.imageExists(dockerClient, imageName);
        if (!imageExists) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(imageName);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    String status = item.getStatus();
                    String progress = item.getProgress();
                    log.info("[DOCKER PULL] Status: {}, Progress: {}, ID: {}", status, progress, item.getId());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd.exec(pullImageResultCallback).awaitCompletion();
            } catch (InterruptedException e) {
                log.info("拉取镜像异常");
                throw new RuntimeException(e);
            }
        }
        log.info(imageExists ? "镜像存在" : "下载完成");
    }
}