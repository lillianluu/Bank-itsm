package com.itsm.module.platform.dto;

import java.util.List;

/** 角色授权菜单请求 */
public record MenuIdsCmd(List<Long> menuIds) {
}

