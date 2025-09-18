package com.chencraft.common.component;


import org.apache.tika.Tika;


public record FileData(String filename, String contentType, byte[] bytes) {

    private static final Tika tika = new Tika();

    public FileData(String filename, byte[] bytes) {
        this(filename, null, bytes);
    }

    public FileData {
        // sanitize the filename before assignment
        filename = extractFilename(filename);

        // If contentType is null or blank, guess it
        if (contentType == null || contentType.isBlank()) {
            contentType = guessMimeType(filename);
        }
    }

    private static String extractFilename(String path) {
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private static String guessMimeType(String filename) {
        String mimeType = tika.detect(filename);
        switch (mimeType) {
            case "text/x-web-markdown" -> mimeType = "text/markdown";
            case null, default -> {
            }
        }
        return mimeType;
    }
}