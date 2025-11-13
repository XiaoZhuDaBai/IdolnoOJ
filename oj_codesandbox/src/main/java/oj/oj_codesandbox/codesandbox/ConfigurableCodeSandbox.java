package oj.oj_codesandbox.codesandbox;

import lombok.extern.slf4j.Slf4j;
import oj.oj_codesandbox.CodeSandbox;
import oj.oj_codesandbox.CodeSandboxTemplate;
import oj.oj_codesandbox.config.SandboxPoolProperties;
import oj.oj_codesandbox.model.ExecuteCodeRequest;
import oj.oj_codesandbox.model.ExecuteCodeResponse;
import oj.oj_codesandbox.model.ExecuteMessage;
import oj.oj_codesandbox.model.LanguageConfigInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 配置驱动的代码沙箱
 * 完全基于配置文件支持多语言，无需为每种语言创建单独的沙箱类
 */
@Slf4j
@Component
public class ConfigurableCodeSandbox implements CodeSandbox {

    @Autowired
    private SandboxPoolProperties properties;

    @Autowired
    private CodeSandboxTemplate codeSandboxTemplate;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();
        String problemId = executeCodeRequest.getProblemId();

        log.info("开始执行代码，语言: {}, 题目ID: {}", language, problemId);

        try {
            // 从配置中获取语言配置
            LanguageConfig config = getLanguageConfig(language);
            String codeFileName = "Main." + getFileExtension(language);

            // 保存代码
            File userCodeFile = codeSandboxTemplate.saveCodeToFile(code, codeFileName);

            // 开始编译和执行
            List<ExecuteMessage> executeMessageList;
            try {
                if (config.needCompile()) {
                    if (config.useContainerPool()) {
                        // 使用容器池执行
                        executeMessageList = codeSandboxTemplate.compileAndRunFileWithPool(
                                userCodeFile, problemId, language, 
                                config.compileCmd(), config.runCmd(), codeFileName);
                    } else {
                        // 使用传统方式执行
                        executeMessageList = codeSandboxTemplate.compileAndRunFile(
                                userCodeFile, problemId, config.imageName(),
                                config.compileCmd(), config.runCmd(), codeFileName);
                    }
                } else {
                    if (config.useContainerPool()) {
                        // 使用容器池执行解释型语言
                        executeMessageList = codeSandboxTemplate.runFileWithPool(
                                userCodeFile, problemId, language, config.runCmd());
                    } else {
                        // 使用传统方式执行
                        executeMessageList = codeSandboxTemplate.runFile(
                                userCodeFile, problemId, config.imageName(), config.runCmd());
                    }
                }
            } catch (Exception e) {
                log.error("执行代码时发生错误: {}", e.getMessage(), e);
                throw new RuntimeException("代码执行失败: " + e.getMessage(), e);
            } finally {
                boolean deletedFile = deleteFile(userCodeFile);
                if (!deletedFile) {
                    log.warn("删除代码文件失败: {}", userCodeFile.getAbsolutePath());
                }
            }

            // 返回运行结果
            return codeSandboxTemplate.getOutputResponse(executeMessageList);

        } catch (Exception e) {
            log.error("代码沙箱执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("代码沙箱执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ExecuteCodeResponse userTestCode(ExecuteCodeRequest executeCodeRequest) {
        String code = executeCodeRequest.getCode();
        String input = executeCodeRequest.getUserInput();
        String language = executeCodeRequest.getLanguage();

        log.info("开始用户测试代码，语言: {}", language);

        try {
            // 从配置中获取语言配置
            LanguageConfig config = getLanguageConfig(language);
            String codeFileName = "Main." + getFileExtension(language);

            // 保存代码和输入
            File userCodeFile = codeSandboxTemplate.saveCodeToFile(code, codeFileName);
            File userInputFile = codeSandboxTemplate.saveCodeToFile(input, "input.txt");

            // 开始执行
            List<ExecuteMessage> executeMessageList;
            try {
                if (config.needCompile()) {
                    if (config.useContainerPool()) {
                        executeMessageList = codeSandboxTemplate.compileAndRunFileWithInputUsingPool(
                                userCodeFile, userInputFile, language,
                                config.compileCmd(), config.runCmd(), codeFileName);
                    } else {
                        executeMessageList = codeSandboxTemplate.compileAndRunFileWithInput(
                                userCodeFile, userInputFile, config.imageName(),
                                config.compileCmd(), config.runCmd(), codeFileName);
                    }
                } else {
                    if (config.useContainerPool()) {
                        executeMessageList = codeSandboxTemplate.runFileWithInputUsingPool(
                                userCodeFile, userInputFile, language, config.runCmd());
                    } else {
                        executeMessageList = codeSandboxTemplate.runFileWithInput(
                                userCodeFile, userInputFile, config.imageName(), config.runCmd());
                    }
                }
            } catch (Exception e) {
                log.error("用户测试代码时发生错误: {}", e.getMessage(), e);
                throw new RuntimeException("用户测试代码失败: " + e.getMessage(), e);
            } finally {
                boolean codeFileDeleted = deleteFile(userCodeFile);
                boolean inputFileDeleted = deleteFile(userInputFile);
                if (!codeFileDeleted || !inputFileDeleted) {
                    log.warn("删除文件失败 - 代码文件: {}, 输入文件: {}",
                            codeFileDeleted ? "成功" : "失败",
                            inputFileDeleted ? "成功" : "失败");
                }
            }

            // 返回运行结果
            return codeSandboxTemplate.getOutputResponse(executeMessageList);

        } catch (Exception e) {
            log.error("用户测试代码沙箱执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("用户测试代码沙箱执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取语言配置
     */
    private LanguageConfig getLanguageConfig(String language) {
        Map<String, LanguageConfigInfo> languageConfigs = properties.getLanguages();
        LanguageConfigInfo config = languageConfigs.get(language);
        if (config == null) {
            throw new RuntimeException("不支持的语言: " + language);
        }
        
        // 转换为内部使用的 LanguageConfig
        return new LanguageConfig(
                config.imageName(),
                config.compileCmd() != null ? config.compileCmd() : "",
                config.runCmd(),
                config.needCompile(),
                config.useContainerPool()
        );
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String language) {
        return switch (language) {
            case "java" -> "java";
            case "cpp" -> "cpp";
            case "c" -> "c";
            case "python", "python3" -> "py";
            case "javascript", "js" -> "js";
            case "go" -> "go";
            case "rust" -> "rs";
            case "php" -> "php";
            default -> language;
        };
    }

    /**
     * 删除文件
     */
    private boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            final boolean del = cn.hutool.core.io.FileUtil.del(userCodeParentPath);
            log.debug("删除文件: {}", del ? "成功" : "失败");
            return del;
        }
        return true;
    }

    /**
     * 内部语言配置类
     */
    public record LanguageConfig(
            String imageName,
            String compileCmd,
            String runCmd,
            boolean needCompile,
            boolean useContainerPool
    ) {}
}
