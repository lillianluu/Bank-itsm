package com.itsm.common.spi.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * 对象存储 SPI(设计文档 10.2):
 * 默认实现 LocalObjectStorage(本地磁盘 + 鉴权代理下载,零外部依赖),
 * 可选 S3ObjectStorage(AWS SDK v2,兼容 MinIO/OSS/COS),itsm.storage.type 开关切换。
 */
public interface ObjectStorage {

    String name();

    /** 上传文件,返回存储键与访问地址 */
    StoredFile put(FileUpload upload);

    /** 按存储键流式读取 */
    InputStream get(String key);

    void remove(String key);

    /** 预签名下载地址;本地实现返回应用鉴权代理地址 */
    String presignedUrl(String key, Duration ttl);
}

