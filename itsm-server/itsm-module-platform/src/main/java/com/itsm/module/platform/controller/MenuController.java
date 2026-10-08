package com.itsm.module.platform.controller;

import com.itsm.common.api.Result;
import com.itsm.module.platform.dto.MenuNode;
import com.itsm.module.platform.dto.MenuSaveCmd;
import com.itsm.module.platform.security.RequirePerm;
import com.itsm.module.platform.service.SysMenuService;
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

/** 菜单管理接口 */
@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
public class MenuController {

    private final SysMenuService menuService;

    @GetMapping("/tree")
    @RequirePerm("sys:menu:list")
    public Result<List<MenuNode>> tree() {
        return Result.ok(menuService.tree());
    }

    @PostMapping
    @RequirePerm("sys:menu:add")
    public Result<Long> create(@Valid @RequestBody MenuSaveCmd cmd) {
        return Result.ok(menuService.create(cmd));
    }

    @PutMapping("/{id}")
    @RequirePerm("sys:menu:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody MenuSaveCmd cmd) {
        menuService.update(id, cmd);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    @RequirePerm("sys:menu:delete")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return Result.ok(null);
    }
}

