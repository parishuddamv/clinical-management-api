package com.clinicos.emr.ai;

import com.clinicos.common.exception.RegistrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Caps AI requests per clinic per day (India time) so a runaway screen or misuse can't run up the bill.
 * Counts are kept in memory, per service instance; they reset at midnight or on restart.
 */
@Component
public class AiUsageLimiter {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    private final int dailyLimit;
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    public AiUsageLimiter(@Value("${ai.daily-limit-per-clinic:200}") int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public void acquire(String clinicId) {
        String today = LocalDate.now(INDIA).toString();
        counts.keySet().removeIf(k -> !k.startsWith(today));
        int used = counts.computeIfAbsent(today + "|" + clinicId, k -> new AtomicInteger()).incrementAndGet();
        if (used > dailyLimit) {
            throw new RegistrationException(HttpStatus.TOO_MANY_REQUESTS,
                    "Today's AI limit for this clinic has been reached. It resets at midnight.");
        }
    }
}
