package com.chencraft.common.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class CommandRunner {
    public void run(List<String> commands, Path directory) throws IOException {
        log.info("Executing command: {}, from: {}", String.join(" ", commands), directory.toAbsolutePath());
        Process process = new ProcessBuilder(commands).directory(directory.toFile())
                                                      .inheritIO()
                                                      .start();
        try {
            if (process.waitFor() != 0) {
                throw new IOException("Command failed: " + String.join(" ", commands));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
