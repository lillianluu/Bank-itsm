package com.itsm.common.spi.storage;

import java.io.InputStream;

/**
 * 上传文件包装:调用方负责在 try-with-resources 中关闭流。
 */
public record FileUpload(String name, String contentType, long size, InputStream stream) implements AutoCloseable {

    @Override
    public void close() throws Exception {
        stream.close();
    }
}

