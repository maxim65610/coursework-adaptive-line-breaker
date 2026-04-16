package hyphenation.model;

import java.util.List;
import java.util.Map;

/**
 * Набор правил переноса для одного языка.
 * Внутри хранит:
 * - patterns — список паттернов переноса
 * - exceptions — словарь слов-исключений
 */
public class HyphenPatternSet {
    private final List<HyphenPattern> patterns;
    private final Map<String, List<Integer>> exceptions;

    public HyphenPatternSet(
            List<HyphenPattern> patterns,
            Map<String, List<Integer>> exceptions
    ) {
        this.patterns = patterns;
        this.exceptions = exceptions;
    }

    public List<HyphenPattern> getPatterns() {
        return patterns;
    }

    public Map<String, List<Integer>> getExceptions() {
        return exceptions;
    }
}