package oj.oj_codesandbox.service;

import oj.oj_codesandbox.utils.ConfigurableErrorClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 错误分类服务
 * 提供统一的错误分类接口，支持不同语言的错误识别
 */
@Service
public class ErrorClassificationService {

    @Autowired
    private ConfigurableErrorClassifier errorClassifier;

    /**
     * 根据语言分类错误
     * @param errorOutput 错误输出
     * @param exitCode 退出码
     * @param language 编程语言
     * @return 错误类型
     */
    public String classifyError(String errorOutput, long exitCode, String language) {
        return errorClassifier.classifyError(errorOutput, exitCode, language);
    }

    /**
     * 兼容原有接口，使用默认语言分类
     * @param errorOutput 错误输出
     * @param exitCode 退出码
     * @return 错误类型
     */
    public String classifyError(String errorOutput, long exitCode) {
        return errorClassifier.classifyError(errorOutput, exitCode);
    }

    /**
     * 判断是否为编译错误
     * @param errorOutput 错误输出
     * @param exitCode 退出码
     * @param language 编程语言
     * @return 是否为编译错误
     */
    public boolean isCompileError(String errorOutput, long exitCode, String language) {
        return "COMPILE_ERROR".equals(classifyError(errorOutput, exitCode, language));
    }

    /**
     * 判断是否为运行时错误
     * @param errorOutput 错误输出
     * @param exitCode 退出码
     * @param language 编程语言
     * @return 是否为运行时错误
     */
    public boolean isRuntimeError(String errorOutput, long exitCode, String language) {
        return "RUNTIME_ERROR".equals(classifyError(errorOutput, exitCode, language));
    }
}
