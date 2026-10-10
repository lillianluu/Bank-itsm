package com.itsm.module.platform.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableName;
import com.itsm.domain.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 部门(树形) */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseDO {

    private Long parentId;
    private String ancestors;
    private String name;
    private Integer sort;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String leader;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String phone;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String email;
    private Integer status;
    @TableLogic
    private Boolean deleted;
}

