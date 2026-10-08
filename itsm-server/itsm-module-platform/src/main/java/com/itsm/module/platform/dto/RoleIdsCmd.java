package com.itsm.module.platform.dto;

import java.util.List;

/** 用户分配角色请求 */
public record RoleIdsCmd(List<Long> roleIds) {
}

