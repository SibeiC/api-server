package com.chencraft.common.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FileDataTest {
    @Test
    public void guessText() {
        assertCorrectMimeGuess("test.txt", "text/plain");
    }

    @Test
    public void guessShell() {
        assertCorrectMimeGuess("test.sh", "application/x-sh");
    }

    @Test
    public void guessPdf() {
        assertCorrectMimeGuess("test.pdf", "application/pdf");
    }

    @Test
    public void guessMarkdown() {
        assertCorrectMimeGuess("test.md", "text/markdown");
    }

    private void assertCorrectMimeGuess(String filename, String expectedMime) {
        FileData data = new FileData(filename, new byte[0]);
        Assertions.assertEquals(expectedMime, data.contentType());
    }

}