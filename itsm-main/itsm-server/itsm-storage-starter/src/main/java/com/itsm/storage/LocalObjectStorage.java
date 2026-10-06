package com.itsm.storage;

import com.itsm.common.spi.storage.FileUpload;
import com.itsm.common.spi.storage.ObjectStorage;
import com.itsm.common.spi.storage.StoredFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘存储(默认实现,零外部依赖):
 * 数据落 {itsm.storage.local.path},下载统一走应用鉴权代理地址;
 * 仅允许单实例形态,多副本部署需切换 S3/MinIO(设计文档 11.3)。
 */
public class LocalObjectStorage implements ObjectStorage {

    private final Path root;
    private final String urlPrefix;

    public LocalObjectStorage(String rootPath, String urlPrefix) throws IOException {
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        this.urlPrefix = (urlPrefix == null || urlPrefix.isBlank()) ? "/api/v1/files" : urlPrefix;
    }

    @Override
    public String name() {
        return "local";
    }

    @Override
    public StoredFile put(FileUpload upload) {
        String key = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                + "/" + UUID.randomUUID().toString().replace("-", "") + extOf(upload.name());
        Path target = resolve(key);
        try (InputStream in = upload.stream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new IllegalStateException("本地存储写入失败: " + upload.name(), e);
        }
        return new StoredFile(key, upload.name(), upload.contentType(), upload.size(), urlPrefix + "/" + key);
    }

    @Override
    public InputStream get(String key) {
        try {
            return Files.newInputStream(resolve(key));
        } catch (IOException e) {
            throw new IllegalArgumentException("文件不存在: " + key, e);
        }
    }

    @Override
    public void remove(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new IllegalStateException("文件删除失败: " + key, e);
        }
    }

    @Override
    public String presignedUrl(String key, Duration ttl) {
        // 本地实现:返回鉴权代理地址,不参与签名
        return urlPrefix + "/" + key;
    }

    /** 防路径穿越 */
    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径: " + key);
        }
        return p;
    }

    private String extOf(String name) {
        int i = name == null ? -1 : name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i);
    }
}

