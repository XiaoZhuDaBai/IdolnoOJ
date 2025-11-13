package oj.oj_codesandbox.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.*;

public class DockerUtils {
    private static final Logger log = LoggerFactory.getLogger(DockerUtils.class);

    public static boolean imageExists(DockerClient dockerClient, String imageName) {
        if (imageName == null || imageName.isEmpty()) {
            throw new IllegalArgumentException("镜像名称不能为空");
        }
        // 解析镜像名称和标签
        String[] parts = imageName.split(":", 2); // 限制分割为两部分
        String repository = parts[0];
        String tag = parts.length > 1 ? parts[1] : "latest";
        // 构建镜像过滤参数
        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd()
                .withImageNameFilter(repository);
        try {
            return listImagesCmd.exec().stream().anyMatch(image ->
                    Arrays.stream(image.getRepoTags())
                            .anyMatch(repoTag -> repoTag.equals(repository + ":" + tag) ||
                                    repoTag.equals(imageName) ||
                                    repoTag.startsWith(imageName + ":"))
            );
        } catch (DockerException e) {
            if (e.getHttpStatus() == 404) {
                return false; // 镜像不存在
            }
            throw new DockerException("镜像查询失败: " + e.getMessage(), e.getHttpStatus());
        }
    }

    public static CreateContainerResponse createResponsePlus(DockerClient dockerClient, String imageName, String[] CMD, String path) {
        log.info("[DOCKER CONTAINER] 开始创建容器");
        log.info("[DOCKER CONTAINER] 镜像名称: " + imageName);
        log.info("[DOCKER CONTAINER] 执行命令: " + String.join(" ", CMD));
        log.info("[DOCKER CONTAINER] 挂载路径: " + path);

        CreateContainerResponse response = dockerClient.createContainerCmd(imageName)
                .withEnv("LANG=C.UTF-8", "LC_ALL=C.UTF-8")
                .withCmd(CMD)
                .withHostConfig(HostConfig.newHostConfig()
                        .withBinds(
                                new Bind(path, new Volume("/app"), AccessMode.rw)
                        )
                        .withMemory(64 * 1024 * 1024L)  // 64MB内存限制
                        .withMemorySwap(64 * 1024 * 1024L)  // 64MB swap限制
                        .withCpuCount(1L)  // 1个CPU核心
                        .withCpuQuota(100000L)  // CPU配额：1核心
                        .withCpuPeriod(100000L)  // CPU周期：100ms
                        .withOomKillDisable(false)  // 启用OOM Killer
                )
                .withTty(true)
                .withNetworkDisabled(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();

        log.info("[DOCKER CONTAINER] 容器创建成功: " + response.getId());
        return response;
    }


    public static DockerClient createDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://localhost:2375")
                .withDockerTlsVerify(false)  // 如果未启用TLS则设为false
                .withRegistryUrl(" https://mirror.ccs.tencentyun.com/")
                .build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }
}
