package hyphenation.impl;

import hyphenation.model.HyphenPattern;
import hyphenation.model.HyphenPatternSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HyphenPatternRepository {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String BASE_PATH = "hyphenation/";

    private final HyphenPatternParser parser;
    private final Map<String, HyphenPatternSet> patternSets;

    public HyphenPatternRepository() {
        this.parser = new HyphenPatternParser();
        this.patternSets = new HashMap<>();

        patternSets.put("ru", loadPatternSet("ru"));
        patternSets.put("en", loadPatternSet("en"));
    }

    public HyphenPatternSet getPatternSet(Locale locale) {
        String language = (locale == null) ? DEFAULT_LANGUAGE : locale.getLanguage();

        HyphenPatternSet set = patternSets.get(language);
        if (set != null) {
            return set;
        }

        return patternSets.get(DEFAULT_LANGUAGE);
    }

    private HyphenPatternSet loadPatternSet(String language) {
        String patternsResource = BASE_PATH + language + "-patterns.txt";
        String exceptionsResource = BASE_PATH + language + "-exceptions.txt";

        List<HyphenPattern> patterns = loadPatterns(patternsResource);
        Map<String, List<Integer>> exceptions = loadExceptions(exceptionsResource);

        return new HyphenPatternSet(patterns, exceptions);
    }

    private List<HyphenPattern> loadPatterns(String resourcePath) {
        List<HyphenPattern> patterns = new ArrayList<>();

        try (BufferedReader reader = openResource(resourcePath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = normalizePatternLine(line);

                if (trimmed.isEmpty()) {
                    continue;
                }

                patterns.add(parser.parse(trimmed));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load hyphenation patterns from: " + resourcePath,
                    e
            );
        }

        return patterns;
    }

    private Map<String, List<Integer>> loadExceptions(String resourcePath) {
        Map<String, List<Integer>> exceptions = new HashMap<>();

        try (BufferedReader reader = openResource(resourcePath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = normalizeExceptionLine(line);

                if (trimmed.isEmpty()) {
                    continue;
                }

                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0 || separatorIndex == trimmed.length() - 1) {
                    throw new IllegalStateException(
                            "Invalid exception line in resource " + resourcePath + ": " + trimmed
                    );
                }

                String word = trimmed.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
                String positionsPart = trimmed.substring(separatorIndex + 1).trim();

                exceptions.put(word, parsePositions(positionsPart, resourcePath, trimmed));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load hyphenation exceptions from: " + resourcePath,
                    e
            );
        }

        return exceptions;
    }

    private List<Integer> parsePositions(String positionsPart, String resourcePath, String originalLine) {
        List<Integer> positions = new ArrayList<>();

        String[] parts = positionsPart.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try {
                positions.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Invalid exception position in resource " + resourcePath + ": " + originalLine,
                        e
                );
            }
        }

        return positions;
    }

    private BufferedReader openResource(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }

        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    private String normalizePatternLine(String line) {
        String withoutComment = removeHashComment(line);
        return withoutComment.trim();
    }

    private String normalizeExceptionLine(String line) {
        String withoutComment = removeHashComment(line);
        return withoutComment.trim();
    }

    private String removeHashComment(String line) {
        int commentIndex = line.indexOf('#');
        if (commentIndex >= 0) {
            return line.substring(0, commentIndex);
        }
        return line;
    }
}