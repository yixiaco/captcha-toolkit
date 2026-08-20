package com.captcha.toolkit.store;

import com.captcha.toolkit.model.CaptchaTicket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 默认内存票据存储：线程安全 + 后台守护线程定期清理过期票据。
 */
public class InMemoryCaptchaTicketStore implements CaptchaTicketStore {

    private final Map<String, CaptchaTicket> tickets = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

    public InMemoryCaptchaTicketStore() {
        this(60_000);
    }

    public InMemoryCaptchaTicketStore(long cleanupIntervalMillis) {
        cleaner = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "captcha-ticket-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        cleaner.scheduleWithFixedDelay(this::clearExpired,
                cleanupIntervalMillis, cleanupIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void put(CaptchaTicket ticket) {
        if (ticket != null) {
            tickets.put(ticket.getTicket(), ticket);
        }
    }

    @Override
    public CaptchaTicket get(String ticket) {
        if (ticket == null) {
            return null;
        }
        CaptchaTicket value = tickets.get(ticket);
        if (value == null) {
            return null;
        }
        if (value.isExpired()) {
            tickets.remove(ticket);
            return null;
        }
        return value;
    }

    @Override
    public void remove(String ticket) {
        if (ticket != null) {
            tickets.remove(ticket);
        }
    }

    @Override
    public void clearExpired() {
        long now = System.currentTimeMillis();
        tickets.entrySet().removeIf(entry -> entry.getValue().getExpiresAt() < now);
    }
}
