package com.itsm.common.spi.storage;

/**
 * 存储结果:storageKey 用于后续读取/删除,url 为访问地址
 * (本地实现 = 鉴权代理地址,S3 实现 = 预签名地址)。
 */
public record StoredFile(String key, String name, String contentType, long size, String url) {
}

