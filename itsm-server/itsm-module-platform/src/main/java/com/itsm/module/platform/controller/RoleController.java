package com.itsm.module.platform.controller;

import com.itsm.common.api.Result;
import com.itsm.module.platform.dto.MenuIdsCmd;
import com.itsm.module.platform.dto.RoleSaveCmd;
import com.itsm.module.platform.dto.RoleVO;
import com.itsm.module.platform.security.RequirePerm;
import com.itsm.module.platform.service.SysRoleService;
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

/** 角色管理接口 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleService roleService;

    @GetMapping
    @RequirePerm("sys:role:list")
    public Result<List<RoleVO>> list() {
        return Result.ok(roleService.list());
    }

    @PostMapping
    @RequirePerm("sys:role:add")
    public Result<Long> create(@Valid @RequestBody RoleSaveCmd cmd) {
        return Result.ok(roleService.create(cmd));
    }

    @PutMapping("/{id}")
    @RequirePerm("sys:role:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleSaveCmd cmd) {
        roleService.update(id, cmd);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    @RequirePerm("sys:role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return Result.ok(null);
    }

    @PutMapping("/{id}/menus")
    @RequirePerm("sys:role:assign-menu")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody MenuIdsCmd cmd) {
        roleService.assignMenus(id, cmd.menuIds());
        return Result.ok(null);
    }
}

