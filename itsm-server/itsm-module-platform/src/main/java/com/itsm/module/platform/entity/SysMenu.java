package com.itsm.module.platform.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.itsm.domain.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 菜单与权限点:type = M 目录 / C 页面 / F 按钮权限 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseDO {

    private Long parentId;
    private String name;
    private String type;
    private String path;
    private String component;
    private String perm;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    @TableLogic
    private Boolean deleted;
}

