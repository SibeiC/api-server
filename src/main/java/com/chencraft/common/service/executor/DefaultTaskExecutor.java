package com.chencraft.common.service.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultTaskExecutor implements TaskExecutor {
    private final ScheduledExecutorService executor;
    private final List<ScheduledFuture<?>> scheduledTasks = new ArrayList<>();

    public DefaultTaskExecutor(int poolSize) {
        this.executor = Executors.newScheduledThreadPool(poolSize);
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        ScheduledFuture<?> scheduled = executor.schedule(task, delay, unit);
        scheduledTasks.add(scheduled);
        return scheduled;
    }

    @Override
    public void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        scheduledTasks.add(executor.scheduleAtFixedRate(task, initialDelay, period, unit));
    }

    @Override
    public void shutdown() {
        scheduledTasks.forEach(task -> task.cancel(true));
        executor.shutdown();
    }
}
