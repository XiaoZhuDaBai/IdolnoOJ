package oj.oj_codesandbox;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("oj.oj_codesandbox.mapper")
public class OjCodesandboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(OjCodesandboxApplication.class, args);
    }

}
