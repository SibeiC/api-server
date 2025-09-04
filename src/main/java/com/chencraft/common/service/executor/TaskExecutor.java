package com.chencraft.common.service.executor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface TaskExecutor {
    void execute(Runnable task);

    ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit);

    void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit);

    void shutdown();
}
