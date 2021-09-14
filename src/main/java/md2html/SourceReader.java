package md2html;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

public class SourceReader implements AutoCloseable {
    public static final char EOF = 0;
    private final BufferedReader reader;
    private String line;
    private StringBuilder paragraph;
    private int pos = 0;

    public SourceReader(final String inputName, final Charset charset) throws IOException {
        reader = new BufferedReader(new FileReader(inputName, charset));
        line = reader.readLine();
        skipEmptyLines();
    }

    public boolean hasNext() {
        return line != null;
    }

    private void skipEmptyLines() throws IOException {
        while (line != null && line.isEmpty()) {
            line = reader.readLine();
        }
    }

    public void nextParagraph() throws IOException {
        paragraph = new StringBuilder(line);
        line = reader.readLine();
        while (line != null && !line.isEmpty()) {
            paragraph.append("\n").append(line);
            line = reader.readLine();
        }
        pos = 0;
        skipEmptyLines();
    }

    public char nextChar() {
        return pos++ >= paragraph.length() ? EOF : paragraph.charAt(pos - 1);
    }

    public StringBuilder nextSequence(final int length) {
        final StringBuilder res = new StringBuilder();
        for (int i = 0; i < length; i++) {
            res.append(nextChar());
        }
        return res;
    }

    public void goBack(final int offset) {
        pos = Math.max(0, pos - offset);
    }

    public void close() throws IOException {
        reader.close();
    }

    public boolean test(final char c) {
        if (c == nextChar()) {
            return true;
        } else {
            goBack(1);
            return false;
        }
    }
}
