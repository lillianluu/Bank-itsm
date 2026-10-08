package com.itsm.module.platform.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.itsm.domain.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 用户 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseDO {

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private Long deptId;
    private Integer status;
    private LocalDateTime lastLoginAt;
    @TableLogic
    private Boolean deleted;
}

