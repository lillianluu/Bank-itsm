package com.itsm.engine.sla;

import java.time.Instant;
import java.util.Optional;

/**
 * SLA 时钟引擎接口(设计文档 10.4-B):
 * Redis ZSet 时间轴(score=解决到期时间戳)+ DB 兜底扫描 + 工作日历换算。
 */
public interface SlaClockService {

    /** 启动解决时钟(工单提交时) */
    void start(String ticketNo, Instant resolveDeadline);

    /** 暂停(等待用户/等待供应商/挂起,按策略配置) */
    void pause(String ticketNo);

    /** 恢复计时,顺延到期时间 */
    void resume(String ticketNo);

    /** 停止并归档(解决/关闭时) */
    void stop(String ticketNo);

    Optional<Instant> getResolveDeadline(String ticketNo);
}

