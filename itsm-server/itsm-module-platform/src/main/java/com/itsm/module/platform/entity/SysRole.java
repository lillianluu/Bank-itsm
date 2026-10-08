package com.itsm.module.platform.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.itsm.domain.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseDO {

    /** 内置超管角色编码,代码层直通校验 */
    public static final String BUILTIN_ADMIN = "ADMIN";

    private String name;
    private String code;
    private Integer sort;
    private String dataScope;
    private Integer status;
    private String remark;
    @TableLogic
    private Boolean deleted;
}

