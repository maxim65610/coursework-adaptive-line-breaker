package hyphenation.impl;

import hyphenation.model.HyphenPattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TexPatternLoader {

    private final HyphenPatternParser parser;

    public TexPatternLoader(HyphenPatternParser parser) {
        this.parser = parser;
    }

    public List<HyphenPattern> loadPatterns(String resourcePath) {
        List<HyphenPattern> result = new ArrayList<>();

        try (BufferedReader reader = openResource(resourcePath)) {
            boolean insidePatternsBlock = false;
            String line;

            while ((line = reader.readLine()) != null) {
                String normalized = normalizeLine(line);

                if (normalized.isEmpty()) {
                    continue;
                }

                if (!insidePatternsBlock) {
                    int startIndex = normalized.indexOf("\\patterns{");
                    if (startIndex >= 0) {
                        insidePatternsBlock = true;
                        String tail = normalized.substring(startIndex + "\\patterns{".length()).trim();
                        consumePatternContent(tail, result);
                    }
                    continue;
                }

                if (consumePatternContent(normalized, result)) {
                    insidePatternsBlock = false;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load TeX patterns from resource: " + resourcePath, e);
        }

        return result;
    }

    private boolean consumePatternContent(String text, List<HyphenPattern> result) {
        int endIndex = text.indexOf('}');
        String content = (endIndex >= 0) ? text.substring(0, endIndex) : text;

        String[] parts = content.trim().split("\\s+");
        for (String part : parts) {
            String pattern = part.trim();
            if (!pattern.isEmpty()) {
                result.add(parser.parse(pattern));
            }
        }

        return endIndex >= 0;
    }

    private BufferedReader openResource(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }

        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    private String normalizeLine(String line) {
        String withoutComment = removeTexComment(line);
        return withoutComment.trim();
    }

    private String removeTexComment(String line) {
        int commentIndex = line.indexOf('%');
        if (commentIndex >= 0) {
            return line.substring(0, commentIndex);
        }
        return line;
    }
}