package oj.oj_codesandbox.utils;

import oj.oj_codesandbox.config.ErrorPatternProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配置化的错误分类器
 * 从配置文件读取错误模式，支持动态配置
 */
@Component
public class ConfigurableErrorClassifier {

    @Autowired
    private ErrorPatternProperties errorPatternProperties;

    /**
     * 根据语言和错误输出分类错误类型
     * @param errorOutput 错误输出信息
     * @param exitCode 退出码
     * @param language 编程语言
     * @return 错误类型：COMPILE_ERROR, RUNTIME_ERROR, UNKNOWN_ERROR
     */
    public String classifyError(String errorOutput, long exitCode, String language) {
        if (errorOutput == null || errorOutput.trim().isEmpty()) {
            return "UNKNOWN_ERROR";
        }

        // 获取该语言的编译错误模式
        List<String> compileErrorPatterns = errorPatternProperties.getCompileErrorPatterns(language);
        
        // 检查是否包含编译错误关键词
        for (String pattern : compileErrorPatterns) {
            if (containsPattern(errorOutput, pattern)) {
                return "COMPILE_ERROR";
            }
        }

        // 获取该语言的运行时错误模式
        List<String> runtimeErrorPatterns = errorPatternProperties.getRuntimeErrorPatterns(language);
        
        // 检查是否包含运行时错误关键词
        for (String pattern : runtimeErrorPatterns) {
            if (containsPattern(errorOutput, pattern)) {
                return "RUNTIME_ERROR";
            }
        }

        // 根据退出码和通用错误关键词判断
        if (exitCode == 1 && errorOutput.contains("error")) {
            return "COMPILE_ERROR";
        }

        // 默认返回运行时错误
        return "RUNTIME_ERROR";
    }

    /**
     * 检查错误输出是否包含指定模式
     * 支持正则表达式匹配
     * @param errorOutput 错误输出
     * @param pattern 匹配模式
     * @return 是否匹配
     */
    private boolean containsPattern(String errorOutput, String pattern) {
        if (pattern.contains(".*")) {
            // 如果模式包含正则表达式，使用正则匹配
            try {
                return errorOutput.matches(".*" + pattern + ".*");
            } catch (Exception e) {
                // 如果正则表达式无效，回退到简单包含匹配
                return errorOutput.contains(pattern.replace(".*", ""));
            }
        } else {
            // 简单包含匹配
            return errorOutput.contains(pattern);
        }
    }

    /**
     * 兼容原有接口，使用默认语言分类
     * @param errorOutput 错误输出
     * @param exitCode 退出码
     * @return 错误类型
     */
    public String classifyError(String errorOutput, long exitCode) {
        return classifyError(errorOutput, exitCode, "java");
    }
}
