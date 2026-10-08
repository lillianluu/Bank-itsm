package com.itsm.module.platform.controller;

import com.itsm.common.api.Result;
import com.itsm.module.platform.dto.DeptNode;
import com.itsm.module.platform.dto.DeptSaveCmd;
import com.itsm.module.platform.security.RequirePerm;
import com.itsm.module.platform.service.SysDeptService;
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

/** 部门管理接口 */
@RestController
@RequestMapping("/api/v1/depts")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptService deptService;

    @GetMapping("/tree")
    @RequirePerm("sys:dept:list")
    public Result<List<DeptNode>> tree() {
        return Result.ok(deptService.tree());
    }

    @PostMapping
    @RequirePerm("sys:dept:add")
    public Result<Long> create(@Valid @RequestBody DeptSaveCmd cmd) {
        return Result.ok(deptService.create(cmd));
    }

    @PutMapping("/{id}")
    @RequirePerm("sys:dept:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody DeptSaveCmd cmd) {
        deptService.update(id, cmd);
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    @RequirePerm("sys:dept:delete")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok(null);
    }
}

