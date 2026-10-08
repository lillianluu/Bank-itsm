package com.itsm.module.platform.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.itsm.module.platform.security.LoginUser;
import com.itsm.module.platform.security.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

/** 审计字段自动填充:createdBy/createdAt/updatedBy/updatedAt(BaseDO) */
@Configuration(proxyBeanMethods = false)
public class MybatisAuditConfig {

    @Bean
    public MetaObjectHandler auditMetaObjectHandler() {
        return new MetaObjectHandler() {

            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                String operator = operator();
                fillIfPresent(metaObject, "createdAt", now);
                fillIfPresent(metaObject, "createdBy", operator);
                fillIfPresent(metaObject, "updatedAt", now);
                fillIfPresent(metaObject, "updatedBy", operator);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                fillIfPresent(metaObject, "updatedAt", LocalDateTime.now());
                fillIfPresent(metaObject, "updatedBy", operator());
            }

            /** BaseDO 为纯 POJO(无 @TableField(fill) 标记),直接按属性名反射填充 */
            private void fillIfPresent(MetaObject metaObject, String field, Object value) {
                if (metaObject.hasSetter(field) && metaObject.getValue(field) == null) {
                    metaObject.setValue(field, value);
                }
            }

            private String operator() {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof LoginUser user) {
                    return user.getUsername();
                }
                return "system";
            }
        };
    }
}

