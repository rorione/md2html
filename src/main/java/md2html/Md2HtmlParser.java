package md2html;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Md2HtmlParser {
    private static final int maxTagLength = 2;
    private final SourceReader source;
    private static final Map<String, String> TagPairs = new HashMap<>();
    private static final Map<Character, String> specialSymbols = new HashMap<>();

    static {
        TagPairs.put("**", "strong");
        TagPairs.put("*", "em");
        TagPairs.put("__", "strong");
        TagPairs.put("_", "em");
        TagPairs.put("--", "s");
        TagPairs.put("`", "code");

        specialSymbols.put('&', "&amp;");
        specialSymbols.put('>', "&gt;");
        specialSymbols.put('<', "&lt;");
    }

    public Md2HtmlParser(final SourceReader source) {
        this.source = source;
    }

    public String parse() throws IOException {
        if (source == null) {
            throw new IOException("Source doesn't exist");
        } else {
            final StringBuilder result = new StringBuilder();
            while (source.hasNext()) {
                source.nextParagraph();
                result.append(parseParagraph());
            }
            return result.toString();
        }
    }

    private StringBuilder parseParagraph() {
        final int headerCounter = parseHeader();
        final StringBuilder content;

        if (headerCounter == 0) {
            content = new StringBuilder("<p>")
                    .append(parseContent("", new HashSet<>()))
                    .append("</p>");
        } else {
            content = new StringBuilder("<h")
                    .append(headerCounter)
                    .append(">")
                    .append(parseContent("", new HashSet<>()))
                    .append("</h")
                    .append(headerCounter)
                    .append(">");
        }
        return content.append('\n');
    }

    private StringBuilder parseContent(final String currentTag, final Set<String> openedTags) {
        final StringBuilder content = new StringBuilder();

        outer:
        while (!source.test(SourceReader.EOF)) {
            if (source.test('\\')) {
                content.append(source.nextChar());
                continue;
            }
            if (source.test('!')) {
                if (source.test('[')) {
                    content.append(parseImage());
                    continue;
                } else {
                    source.goBack(1);
                }
            }

            final char next = source.nextChar();
            final String specialSymbolAssumption = specialSymbols.get(next);
            if (specialSymbolAssumption != null) {
                content.append(specialSymbolAssumption);
                continue;
            } else {
                source.goBack(1);
            }

            final StringBuilder peek = source.nextSequence(maxTagLength);
            for (int i = maxTagLength; i > 0; i--) {
                String tagAssumption = TagPairs.get(peek.toString());
                if (tagAssumption == null) {
                    peek.setLength(peek.length() - 1);
                    source.goBack(1);
                } else if (peek.toString().equals(currentTag)) {
                    openedTags.remove(currentTag);
                    content.insert(0, new StringBuilder("<").append(tagAssumption).append(">"));
                    content.append("</").append(tagAssumption).append(">");
                    return content;
                } else if (openedTags.contains(peek.toString())) {
                    source.goBack(i);
                    openedTags.remove(currentTag);
                    return new StringBuilder(currentTag).append(content);
                } else {
                    openedTags.add(peek.toString());
                    content.append(parseContent(peek.toString(), openedTags));
                    continue outer;
                }
            }
            content.append(source.nextChar());
        }
        return currentTag.isEmpty() ? content : new StringBuilder(currentTag).append(content);
    }

    private int parseHeader() {
        char ch = source.nextChar();
        int headerCounter = 0;
        while (ch == '#') {
            headerCounter++;
            ch = source.nextChar();
        }
        if (Character.isWhitespace(ch) && headerCounter != 0) {
            return headerCounter;
        } else {
            source.goBack(headerCounter + 1);
            return 0;
        }
    }

    private StringBuilder parseImage() {
        final StringBuilder linkText = new StringBuilder();
        int parsedLength = 0;
        while (!source.test(']')) {
            if (source.test(SourceReader.EOF)) {
                source.goBack(parsedLength + 1);
                return new StringBuilder("![");
            }
            linkText.append(source.nextChar());
            parsedLength++;
        }
        if (source.test('(')) {
            final StringBuilder linkHref = new StringBuilder();
            while (!source.test(')')) {
                if (source.test(SourceReader.EOF)) {
                    source.goBack(parsedLength + 3);
                    return new StringBuilder("![");
                }
                linkHref.append(source.nextChar());
                parsedLength++;
            }
            return new StringBuilder("<img alt='")
                    .append(linkText)
                    .append("' src='")
                    .append(linkHref)
                    .append("'>");
        } else {
            source.goBack(parsedLength + 1);
            return new StringBuilder("![");
        }
    }

    public static void main(final String[] args) throws IOException {
        try (final SourceReader source = new SourceReader("test.txt", StandardCharsets.UTF_8)) {
            final Md2HtmlParser parser = new Md2HtmlParser(source);
            final String result = parser.parse();

            System.out.println(result);
        }
    }
}
