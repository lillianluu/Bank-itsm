package com.itsm.module.platform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itsm.common.exception.BusinessException;
import com.itsm.module.platform.dto.DeptNode;
import com.itsm.module.platform.dto.DeptSaveCmd;
import com.itsm.module.platform.entity.SysDept;
import com.itsm.module.platform.entity.SysUser;
import com.itsm.module.platform.mapper.SysDeptMapper;
import com.itsm.module.platform.mapper.SysUserMapper;
import com.itsm.module.platform.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.itsm.common.api.ErrorCode.BAD_REQUEST;
import static com.itsm.common.api.ErrorCode.CONFLICT;
import static com.itsm.common.api.ErrorCode.NOT_FOUND;

/** 部门服务实现 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    @Override
    public List<DeptNode> tree() {
        List<SysDept> depts = deptMapper.selectList(
                Wrappers.<SysDept>lambdaQuery().orderByAsc(SysDept::getSort).orderByAsc(SysDept::getId));
        Map<Long, DeptNode> nodeMap = new LinkedHashMap<>();
        for (SysDept d : depts) {
            nodeMap.put(d.getId(), DeptNode.of(d.getId(), d.getParentId(), d.getName(),
                    d.getSort(), d.getLeader(), d.getPhone(), d.getEmail(), d.getStatus()));
        }
        List<DeptNode> roots = new ArrayList<>();
        for (DeptNode node : nodeMap.values()) {
            DeptNode parent = node.parentId() == null || node.parentId() == 0
                    ? null : nodeMap.get(node.parentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public Long create(DeptSaveCmd cmd) {
        SysDept parent = validateParent(cmd.parentId());
        if (parent != null && !Integer.valueOf(1).equals(parent.getStatus())) {
            throw new BusinessException(BAD_REQUEST, "停用部门下不允许新增下级部门");
        }
        validateName(cmd.parentId(), cmd.name().trim(), null);
        SysDept dept = new SysDept();
        apply(dept, cmd);
        dept.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getId());
        try {
            deptMapper.insert(dept);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(CONFLICT, "同一上级下部门名称已存在");
        }
        return dept.getId();
    }

    @Override
    @Transactional
    public void update(Long id, DeptSaveCmd cmd) {
        SysDept dept = requireExists(id);
        // M1 阶段不支持调整挂载关系(涉及整棵子树 ancestors 重算),V1.1 提供"移动部门"
        Long parentId = dept.getParentId();
        validateName(parentId, cmd.name().trim(), id);
        apply(dept, cmd);
        dept.setParentId(parentId);
        try {
            deptMapper.updateById(dept);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(CONFLICT, "同一上级下部门名称已存在");
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireExists(id);
        Long children = deptMapper.selectCount(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BusinessException(BAD_REQUEST, "存在下级部门,无法删除");
        }
        Long users = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getDeptId, id));
        if (users > 0) {
            throw new BusinessException(BAD_REQUEST, "部门下存在用户,无法删除");
        }
        deptMapper.deleteById(id);
    }

    private void validateName(Long parentId, String name, Long excludeId) {
        Long count = deptMapper.selectCount(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getParentId, parentId)
                .eq(SysDept::getName, name)
                .ne(excludeId != null, SysDept::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(CONFLICT, "同一上级下部门名称已存在");
        }
    }

    private SysDept validateParent(Long parentId) {
        if (parentId == null || parentId == 0) {
            return null;
        }
        SysDept parent = deptMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(NOT_FOUND, "上级部门不存在");
        }
        return parent;
    }

    private SysDept requireExists(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(NOT_FOUND, "部门不存在");
        }
        return dept;
    }

    private void apply(SysDept dept, DeptSaveCmd cmd) {
        dept.setParentId(cmd.parentId() == null ? 0L : cmd.parentId());
        dept.setName(cmd.name().trim());
        dept.setSort(cmd.sort() == null ? 0 : cmd.sort());
        dept.setLeader(cmd.leader());
        dept.setPhone(cmd.phone());
        dept.setEmail(cmd.email());
        dept.setStatus(cmd.status() == null ? 1 : cmd.status());
    }
}

