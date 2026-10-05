package com.itsm.storage;

import com.itsm.common.spi.storage.ObjectStorage;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * 存储自动装配:未引入 AWS SDK 依赖时,S3 分支整体不可达;
 * 默认兜底本地磁盘实现,保证零外部依赖可运行(设计文档 10.2.2)。
 */
@AutoConfiguration
@EnableConfigurationProperties(ItsStorageProperties.class)
public class ItsStorageAutoConfiguration {

    /** S3 可选实现:classpath 存在 AWS SDK 且 itsm.storage.type=s3 时装配 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "software.amazon.awssdk.services.s3.S3Client")
    @ConditionalOnProperty(name = "itsm.storage.type", havingValue = "s3")
    static class S3StorageConfiguration {

        @Bean
        public ObjectStorage s3ObjectStorage(ItsStorageProperties props) {
            ItsStorageProperties.S3 s3 = props.getS3();
            StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey()));
            S3Client client = S3Client.builder()
                    .endpointOverride(URI.create(s3.getEndpoint()))
                    .region(Region.of(s3.getRegion()))
                    .forcePathStyle(s3.isPathStyle())
                    .credentialsProvider(credentials)
                    .build();
            S3Presigner presigner = S3Presigner.builder()
                    .endpointOverride(URI.create(s3.getEndpoint()))
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(credentials)
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(s3.isPathStyle())
                            .build())
                    .build();
            return new S3ObjectStorage(client, presigner, s3.getBucket());
        }
    }

    /** 默认兜底:本地磁盘存储,零外部依赖 */
    @Bean
    @ConditionalOnMissingBean(ObjectStorage.class)
    public ObjectStorage localObjectStorage(ItsStorageProperties props) throws Exception {
        return new LocalObjectStorage(props.getLocal().getPath(), props.getLocal().getUrlPrefix());
    }
}

