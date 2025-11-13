package oj.oj_codesandbox;

import oj.oj_codesandbox.service.ErrorClassificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 错误分类测试
 */
@SpringBootTest
public class ErrorClassificationTest {

    @Autowired
    private ErrorClassificationService errorClassificationService;

    @Test
    public void testJavaCompileError() {
        String errorOutput = "error: cannot find symbol\n  symbol:   variable x\n  location: class Main";
        String result = errorClassificationService.classifyError(errorOutput, 1, "java");
        assertEquals("COMPILE_ERROR", result);
    }

    @Test
    public void testJavaRuntimeError() {
        String errorOutput = "Exception in thread \"main\" java.lang.NullPointerException\n\tat Main.main(Main.java:5)";
        String result = errorClassificationService.classifyError(errorOutput, 1, "java");
        assertEquals("RUNTIME_ERROR", result);
    }

    @Test
    public void testCppCompileError() {
        String errorOutput = "error: 'x' was not declared in this scope";
        String result = errorClassificationService.classifyError(errorOutput, 1, "cpp");
        assertEquals("COMPILE_ERROR", result);
    }

    @Test
    public void testCppRuntimeError() {
        String errorOutput = "Segmentation fault (core dumped)";
        String result = errorClassificationService.classifyError(errorOutput, 139, "cpp");
        assertEquals("RUNTIME_ERROR", result);
    }

    @Test
    public void testPythonCompileError() {
        String errorOutput = "SyntaxError: invalid syntax";
        String result = errorClassificationService.classifyError(errorOutput, 1, "python3");
        assertEquals("COMPILE_ERROR", result);
    }

    @Test
    public void testPythonRuntimeError() {
        String errorOutput = "Traceback (most recent call last):\n  File \"main.py\", line 1, in <module>\n    print(x)\nNameError: name 'x' is not defined";
        String result = errorClassificationService.classifyError(errorOutput, 1, "python3");
        assertEquals("RUNTIME_ERROR", result);
    }

    @Test
    public void testJavaScriptCompileError() {
        String errorOutput = "SyntaxError: Unexpected token '{'";
        String result = errorClassificationService.classifyError(errorOutput, 1, "javascript");
        assertEquals("COMPILE_ERROR", result);
    }

    @Test
    public void testJavaScriptRuntimeError() {
        String errorOutput = "ReferenceError: x is not defined";
        String result = errorClassificationService.classifyError(errorOutput, 1, "javascript");
        assertEquals("RUNTIME_ERROR", result);
    }

    @Test
    public void testUnknownLanguage() {
        String errorOutput = "Some error message";
        String result = errorClassificationService.classifyError(errorOutput, 1, "unknown");
        assertEquals("RUNTIME_ERROR", result);
    }
}
