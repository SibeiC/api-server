package com.chencraft.common.exception;

public class GitHubUnauthorizedException extends RuntimeException {
    public GitHubUnauthorizedException(String message) {
        super(message);
    }
}
