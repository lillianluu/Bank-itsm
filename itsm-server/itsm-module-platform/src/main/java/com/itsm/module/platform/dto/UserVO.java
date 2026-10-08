package com.itsm.module.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 用户视图 */
public record UserVO(
        Long id,
        String username,
        String nickname,
        Long deptId,
        String deptName,
        String email,
        String phone,
        Integer status,
        List<Long> roleIds,
        List<String> roleNames,
        LocalDateTime createdAt) {
}

