package com.itsm.module.platform.service.impl;

import com.itsm.common.exception.BusinessException;
import com.itsm.common.api.ErrorCode;
import com.itsm.module.platform.dto.DeptSaveCmd;
import com.itsm.module.platform.entity.SysDept;
import com.itsm.module.platform.mapper.SysDeptMapper;
import com.itsm.module.platform.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysDeptServiceImplTest {
    SysDeptMapper depts;
    SysUserMapper users;
    SysDeptServiceImpl service;
    @BeforeEach void setup() {
        depts = mock(SysDeptMapper.class);
        users = mock(SysUserMapper.class);
        service = new SysDeptServiceImpl(depts, users);
        when(depts.selectCount(any())).thenReturn(0L);
        when(users.selectCount(any())).thenReturn(0L);
    }
    DeptSaveCmd cmd(Long parent) {
        return new DeptSaveCmd(parent, " 运维组 ", null, null, null, "ops@example.com", null);
    }
    SysDept dept(long id, long parent, int status) {
        SysDept d = new SysDept();
        d.setId(id); d.setParentId(parent); d.setStatus(status); d.setAncestors("0,1");
        return d;
    }
    @Test void creationMaintainsAncestorsAndTrimsName() {
        when(depts.selectById(2L)).thenReturn(dept(2, 1, 1));
        service.create(cmd(2L));
        verify(depts).insert(argThat((SysDept d) -> d.getAncestors().equals("0,1,2")
                && d.getName().equals("运维组") && d.getSort() == 0 && d.getStatus() == 1));
    }
    @Test void disabledParentRejectsCreation() {
        when(depts.selectById(2L)).thenReturn(dept(2, 1, 0));
        assertEquals(ErrorCode.BAD_REQUEST, assertThrows(BusinessException.class,
                () -> service.create(cmd(2L))).getErrorCode());
        verify(depts, never()).insert(any(SysDept.class));
    }
    @Test void missingParentRejectsCreation() {
        assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class,
                () -> service.create(cmd(2L))).getErrorCode());
    }
    @Test void editingCannotMoveDepartment() {
        SysDept d = dept(3, 2, 1);
        when(depts.selectById(3L)).thenReturn(d);
        service.update(3L, cmd(99L));
        assertEquals(2L, d.getParentId());
        assertEquals("0,1", d.getAncestors());
    }
    @Test void duplicateNameIsConflict() {
        when(depts.selectCount(any())).thenReturn(1L);
        assertEquals(ErrorCode.CONFLICT, assertThrows(BusinessException.class,
                () -> service.create(cmd(0L))).getErrorCode());
    }
    @Test void concurrentDuplicateIsConflict() {
        when(depts.insert(any(SysDept.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertEquals(ErrorCode.CONFLICT, assertThrows(BusinessException.class,
                () -> service.create(cmd(0L))).getErrorCode());
    }
    @Test void childrenPreventDeletion() {
        when(depts.selectById(3L)).thenReturn(dept(3, 2, 1));
        when(depts.selectCount(any())).thenReturn(1L);
        assertEquals("存在下级部门,无法删除", assertThrows(BusinessException.class,
                () -> service.delete(3L)).getMessage());
        verify(depts, never()).deleteById(3L);
    }
    @Test void activeUsersPreventDeletion() {
        when(depts.selectById(3L)).thenReturn(dept(3, 2, 1));
        when(users.selectCount(any())).thenReturn(1L);
        assertEquals("部门下存在用户,无法删除", assertThrows(BusinessException.class,
                () -> service.delete(3L)).getMessage());
    }
    @Test void emptyRootCanBeLogicallyDeleted() {
        when(depts.selectById(1L)).thenReturn(dept(1, 0, 1));
        service.delete(1L);
        verify(depts).deleteById(1L);
    }
}
