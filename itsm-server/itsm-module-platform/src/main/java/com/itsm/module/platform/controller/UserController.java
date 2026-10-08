package com.itsm.module.platform.controller;

import com.itsm.common.api.PageResult;
import com.itsm.common.api.Result;
import com.itsm.module.platform.dto.PwdCmd;
import com.itsm.module.platform.dto.RoleIdsCmd;
import com.itsm.module.platform.dto.UserCreateCmd;
import com.itsm.module.platform.dto.UserQuery;
import com.itsm.module.platform.dto.UserUpdateCmd;
import com.itsm.module.platform.dto.UserVO;
import com.itsm.module.platform.security.RequirePerm;
import com.itsm.module.platform.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 用户管理接口 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService userService;

    @GetMapping
    @RequirePerm("sys:user:list")
    public Result<PageResult<UserVO>> page(UserQuery query) {
        return Result.ok(userService.page(query));
    }

    @GetMapping("/{id}")
    @RequirePerm("sys:user:list")
    public Result<UserVO> get(@PathVariable Long id) {
        return Result.ok(userService.get(id));
    }

    @PostMapping
    @RequirePerm("sys:user:add")
    public Result<Long> create(@Valid @RequestBody UserCreateCmd cmd) {
        return Result.ok(userService.create(cmd));
    }

    @PutMapping("/{id}")
    @RequirePerm("sys:user:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateCmd cmd) {
        userService.update(id, cmd);
        return Result.ok(null);
    }

    @DeleteMapping("/{ids}")
    @RequirePerm("sys:user:delete")
    public Result<Void> delete(@PathVariable List<Long> ids) {
        userService.delete(ids);
        return Result.ok(null);
    }

    @PutMapping("/{id}/password")
    @RequirePerm("sys:user:reset-pwd")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody PwdCmd cmd) {
        userService.resetPassword(id, cmd.newPassword());
        return Result.ok(null);
    }

    @PutMapping("/{id}/roles")
    @RequirePerm("sys:user:assign-role")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody RoleIdsCmd cmd) {
        userService.assignRoles(id, cmd.roleIds());
        return Result.ok(null);
    }
}

