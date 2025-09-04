package com.chencraft.common.component;

import com.chencraft.common.service.executor.TaskExecutor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AutoCleaner {
    private final TaskExecutor taskExecutor;
    private final List<Cleanable> cleanableClasses;

    @Autowired
    public AutoCleaner(TaskExecutor taskExecutor, List<Cleanable> cleanableClasses) {
        this.taskExecutor = taskExecutor;
        this.cleanableClasses = cleanableClasses;
    }

    @PostConstruct
    public void init() {
        List<String> classes = cleanableClasses.stream().map(Cleanable::getClass).map(Class::getSimpleName).toList();
        log.info("Scheduling auto-cleaner task for {}", classes);
        taskExecutor.scheduleAtFixedRate(this::cleanAll, 1, 1, java.util.concurrent.TimeUnit.DAYS);
    }

    private void cleanAll() {
        log.info("Cleaning up all registered cleanable classes");
        cleanableClasses.forEach(Cleanable::cleanUp);
    }
}
