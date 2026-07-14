package com.luv2code.jobportal.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class JobIndexVersionService {

    private static final Logger log = LoggerFactory.getLogger(JobIndexVersionService.class);
    private static final String VERSION_KEY = "jobportal:job-index:version";
    private static final String BACKFILL_LOCK_KEY = "jobportal:job-index:backfill-lock";

    private final StringRedisTemplate redisTemplate;
    private final AtomicLong localFallback = new AtomicLong();

    public JobIndexVersionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long currentVersion() {
        try {
            String value = redisTemplate.opsForValue().get(VERSION_KEY);
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception exception) {
            log.warn("Redis job-index version read failed; using local fallback", exception);
            return localFallback.get();
        }
    }

    public long increment() {
        try {
            Long value = redisTemplate.opsForValue().increment(VERSION_KEY);
            return value == null ? localFallback.incrementAndGet() : value;
        } catch (Exception exception) {
            long value = localFallback.incrementAndGet();
            log.warn("Redis job-index version increment failed; using local fallback={}", value, exception);
            return value;
        }
    }

    public boolean tryAcquireBackfillLock(String owner) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(BACKFILL_LOCK_KEY, owner, Duration.ofMinutes(30)));
        } catch (Exception exception) {
            log.warn("Redis backfill lock is unavailable; allowing local backfill", exception);
            return true;
        }
    }

    public void releaseBackfillLock(String owner) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then "
                + "return redis.call('del', KEYS[1]) else return 0 end";
        try {
            redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                    Collections.singletonList(BACKFILL_LOCK_KEY), owner);
        } catch (Exception exception) {
            log.warn("Redis backfill lock release failed; it will expire automatically", exception);
        }
    }
}
