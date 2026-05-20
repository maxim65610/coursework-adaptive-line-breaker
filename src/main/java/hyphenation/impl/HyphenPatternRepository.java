package hyphenation.impl;

import hyphenation.model.HyphenPattern;
import hyphenation.model.HyphenPatternSet;
import hyphenation.model.HyphenPatternTrie;

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

/**
 * Репозиторий языковых правил переноса.
 * Загружает:
 * - паттерны переноса из файлов вида "*-patterns.txt"
 * - словари исключений из файлов вида "*-exceptions.txt"
 * Формат паттернов:
 * одна строка = один паттерн
 * Формат исключений:
 * одна строка = слово с отмеченными дефисами местами переноса
 */
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

    /**
     * Возвращает набор правил переноса для нужного языка.
     * Если язык неизвестен, используется английский как fallback.
     */
    public HyphenPatternSet getPatternSet(Locale locale) {
        String language = (locale == null) ? DEFAULT_LANGUAGE : locale.getLanguage();

        HyphenPatternSet set = patternSets.get(language);
        if (set != null) {
            return set;
        }

        return patternSets.get(DEFAULT_LANGUAGE);
    }

    /**
     * Загружает полный набор правил для одного языка:
     * - паттерны
     * - исключения
     */
    private HyphenPatternSet loadPatternSet(String language) {
        String patternsResource = BASE_PATH + language + "-patterns.txt";
        String exceptionsResource = BASE_PATH + language + "-exceptions.txt";

        List<HyphenPattern> patterns = loadPatterns(patternsResource);
        HyphenPatternTrie patternTrie = buildPatternTrie(patterns);
        Map<String, List<Integer>> exceptions = loadExceptions(exceptionsResource);

        return new HyphenPatternSet(patternTrie, exceptions);
    }

    /**
     * Строит автомат паттернов переноса по списку паттернов.
     *
     * @param patterns список паттернов
     * @return готовый автомат паттернов
     */
    private HyphenPatternTrie buildPatternTrie(List<HyphenPattern> patterns) {
        HyphenPatternTrie trie = new HyphenPatternTrie();

        for (HyphenPattern pattern : patterns) {
            trie.addPattern(pattern);
        }

        trie.buildFailureLinks();
        return trie;
    }
    /**
     * Загружает паттерны из файла ресурсов.
     */
    private List<HyphenPattern> loadPatterns(String resourcePath) {
        List<HyphenPattern> patterns = new ArrayList<>();

        try (BufferedReader reader = openResource(resourcePath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = normalizePatternLine(line);

                if (trimmed.isEmpty()) {
                    continue;
                }

                HyphenPattern pattern = parser.parse(trimmed);
                if (pattern.getLetters().length > 0) {
                    patterns.add(pattern);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load hyphenation patterns from: " + resourcePath,
                    e
            );
        }

        return patterns;
    }

    /**
     * Загружает исключения из файла ресурсов.
     */
    private Map<String, List<Integer>> loadExceptions(String resourcePath) {
        Map<String, List<Integer>> exceptions = new HashMap<>();

        try (BufferedReader reader = openResource(resourcePath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String trimmed = normalizeExceptionLine(line);

                if (trimmed.isEmpty()) {
                    continue;
                }

                ParsedException parsedException = parseHyphenatedException(trimmed);
                exceptions.put(parsedException.getWord(), parsedException.getPositions());
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load hyphenation exceptions from: " + resourcePath,
                    e
            );
        }

        return exceptions;
    }

    /**
     * Разбирает строку исключения
     */
    private ParsedException parseHyphenatedException(String line) {
        StringBuilder wordBuilder = new StringBuilder();
        List<Integer> positions = new ArrayList<>();

        int codePointIndex = 0;
        int i = 0;

        while (i < line.length()) {
            int codePoint = line.codePointAt(i);
            i += Character.charCount(codePoint);

            // Дефис в файле исключений означает:
            // "запомни текущую позицию как допустимую точку переноса"
            if (codePoint == '-') {
                positions.add(codePointIndex);
                continue;
            }

            // Любой другой символ добавляем в итоговое слово
            // и увеличиваем индекс code point.
            wordBuilder.appendCodePoint(codePoint);
            codePointIndex++;
        }

        String word = wordBuilder.toString().toLowerCase(Locale.ROOT);
        return new ParsedException(word, positions);
    }

    /**
     * Открывает файл ресурсов из classpath.
     */
    private BufferedReader openResource(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);

        if (inputStream == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }

        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /**
     * Очищает строку паттерна:
     * - убирает комментарий после '#'
     * - обрезает пробелы по краям
     */
    private String normalizePatternLine(String line) {
        String withoutComment = removeHashComment(line);
        return withoutComment.trim();
    }

    /**
     * Очищает строку исключения:
     * - убирает комментарий после '#'
     * - обрезает пробелы по краям
     */
    private String normalizeExceptionLine(String line) {
        String withoutComment = removeHashComment(line);
        return withoutComment.trim();
    }

    /**
     * Удаляет комментарий, начинающийся с '#'.
     */
    private String removeHashComment(String line) {
        int commentIndex = line.indexOf('#');

        if (commentIndex >= 0) {
            return line.substring(0, commentIndex);
        }

        return line;
    }

    /**
     * Внутренний контейнер результата разбора строки исключения.
     */
    private static class ParsedException {
        private final String word;
        private final List<Integer> positions;

        public ParsedException(String word, List<Integer> positions) {
            this.word = word;
            this.positions = positions;
        }

        public String getWord() {
            return word;
        }

        public List<Integer> getPositions() {
            return positions;
        }
    }
}