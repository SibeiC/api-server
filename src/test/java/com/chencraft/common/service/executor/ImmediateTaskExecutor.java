package com.chencraft.common.service.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Primary
@Component
public class ImmediateTaskExecutor implements TaskExecutor {

    @Override
    public void execute(Runnable task) {
        task.run();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        task.run();
        return null;
    }

    @Override
    public void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        log.info("Task should be delayed for {} {}, and repeated every {} {}", initialDelay, unit, period, unit);
        if (initialDelay == 0) {
            task.run();
        } else {
            log.warn("Skipping task for unit test since initial delay is not 0");
        }
    }

    @Override
    public void shutdown() {

    }
}