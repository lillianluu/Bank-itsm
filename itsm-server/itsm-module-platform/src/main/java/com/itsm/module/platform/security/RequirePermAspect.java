package com.itsm.module.platform.security;

import com.itsm.common.api.ErrorCode;
import com.itsm.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** @RequirePerm 切面:无权限点直接 403 */
@Slf4j
@Aspect
@Component
public class RequirePermAspect {

    @Around("@annotation(requirePerm)")
    public Object around(ProceedingJoinPoint pjp, RequirePerm requirePerm) throws Throwable {
        if (!SecurityUtils.hasPerm(requirePerm.value())) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "缺少权限: " + requirePerm.value());
        }
        return pjp.proceed();
    }
}

