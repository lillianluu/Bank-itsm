package com.itsm.storage;

import com.itsm.common.spi.storage.FileUpload;
import com.itsm.common.spi.storage.ObjectStorage;
import com.itsm.common.spi.storage.StoredFile;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * S3 兼容对象存储实现(AWS SDK v2,兼容 MinIO/阿里云 OSS/腾讯云 COS 的 S3 模式)。
 * 仅当 classpath 存在 AWS SDK 且 itsm.storage.type=s3 时装配。
 */
@Slf4j
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3ObjectStorage(S3Client s3, S3Presigner presigner, String bucket) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    @Override
    public String name() {
        return "s3";
    }

    @Override
    public StoredFile put(FileUpload upload) {
        String key = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + extOf(upload.name());
        try (InputStream in = upload.stream()) {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(upload.contentType())
                            .build(),
                    RequestBody.fromInputStream(in, upload.size()));
        } catch (Exception e) {
            throw new IllegalStateException("S3 上传失败: " + upload.name(), e);
        }
        return new StoredFile(key, upload.name(), upload.contentType(), upload.size(), key);
    }

    @Override
    public InputStream get(String key) {
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public void remove(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(ttl)
                        .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                        .build())
                .url().toString();
    }

    private String extOf(String name) {
        int i = name == null ? -1 : name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i);
    }
}

