package oj.oj_codesandbox;


import oj.oj_codesandbox.codesandbox.ConfigurableCodeSandbox;
import oj.oj_codesandbox.codesandbox.ThirdCodeSandbox;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 通过静态工厂方法获取不同类型的CodeSandbox实例
 */
@Component
public class CodeSandboxFactor implements ApplicationContextAware {
    
    private static ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CodeSandboxFactor.applicationContext = applicationContext;
    }
    
    public static CodeSandbox newInstance(String type) {
        switch (type) {
            case "native":
            case "configurable":
                return applicationContext.getBean(ConfigurableCodeSandbox.class);
            case "third":
                return new ThirdCodeSandbox();
            default:
                throw new RuntimeException("没有实现这个沙箱");
        }
    }
}
