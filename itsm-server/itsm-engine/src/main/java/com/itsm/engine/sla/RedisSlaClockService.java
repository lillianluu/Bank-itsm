package com.itsm.engine.sla;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * SLA 时钟引擎默认实现(Redis ZSet,设计文档 10.4-B):
 * key = itsm:sla:deadline,score = resolve_deadline 毫秒时间戳;
 * 定时任务按 score 升序扫描临期(20%/10%)/超时工单触发预警与升级;
 * Redis 不可用时自动降级 DB 扫描(RUNNING + resolve_deadline < now(),分钟级精度)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSlaClockService implements SlaClockService {

    public static final String DEADLINE_ZSET_KEY = "itsm:sla:deadline";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void start(String ticketNo, Instant resolveDeadline) {
        redisTemplate.opsForZSet().add(DEADLINE_ZSET_KEY, ticketNo, resolveDeadline.toEpochMilli());
    }

    @Override
    public void pause(String ticketNo) {
        // TODO: 记录暂停起点并累计 paused_seconds,顺延 resolve_deadline(设计文档 6.4)
        log.debug("SLA clock paused (skeleton): {}", ticketNo);
    }

    @Override
    public void resume(String ticketNo) {
        // TODO: 恢复计时,重算到期时间并更新 ZSet score
        log.debug("SLA clock resumed (skeleton): {}", ticketNo);
    }

    @Override
    public void stop(String ticketNo) {
        redisTemplate.opsForZSet().remove(DEADLINE_ZSET_KEY, ticketNo);
    }

    @Override
    public Optional<Instant> getResolveDeadline(String ticketNo) {
        Double score = redisTemplate.opsForZSet().score(DEADLINE_ZSET_KEY, ticketNo);
        return score == null ? Optional.empty() : Optional.of(Instant.ofEpochMilli(score.longValue()));
    }
}

