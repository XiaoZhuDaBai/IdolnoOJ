package oj.oj_codesandbox.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import oj.oj_codesandbox.model.LanguageConfigInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

/**
 * 沙箱池配置属性类
 * 映射 application.yml 中的 sandbox 配置
 */
@Component
@ConfigurationProperties(prefix = "sandbox")
@Validated
@Data
public class SandboxPoolProperties {

    @NotBlank(message = "宿主机代码根目录不能为空")
    private String hostCodeBaseDir = "${user.dir}";

    @Valid
    private DockerConfig docker = new DockerConfig();

    @Valid
    private MonitoringConfig monitoring = new MonitoringConfig();

    @Valid
    private SecurityConfig security = new SecurityConfig();

    @Valid
    private Map<String, LanguageConfigInfo> languages;


    /**
     * Docker 配置
     */
    @Data
    public static class DockerConfig {
        @NotBlank(message = "Docker主机地址不能为空")
        private String host = "tcp://localhost:2375";

        @Positive(message = "连接超时时间必须大于0")
        private int connectTimeout = 30000;

        @Positive(message = "读取超时时间必须大于0")
        private int readTimeout = 60000;

    }

    /**
     * 监控配置
     */
    @Data
    public static class MonitoringConfig {
        @Positive(message = "健康检查间隔必须大于0")
        private long healthCheckInterval = 30000;

        private boolean metricsEnabled = true;

    }

    /**
     * 安全配置
     */
    @Data
    public static class SecurityConfig {
        private boolean enableNetworkIsolation = true;

        @Positive(message = "最大文件大小必须大于0")
        private long maxFileSize = 10485760; // 10MB

        private String[] allowedSystemCalls = {"read", "write", "exit"};
    }
}
