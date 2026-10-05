package com.itsm.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 统一分页结构,分页参数规范见设计文档 9.1(page/size/sort)。
 */
@Getter
@AllArgsConstructor
public class PageResult<T> {

    private final long total;
    private final int pageNum;
    private final int pageSize;
    private final List<T> records;

    public static <T> PageResult<T> of(long total, int pageNum, int pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records);
    }

    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        return of(0, pageNum, pageSize, List.of());
    }
}

