package hyphenation.model;

import java.util.List;
import java.util.Map;

/**
 * Набор правил переноса для одного языка.
 * Хранит:
 * - дерево паттернов переноса
 * - словарь слов-исключений
 */
public class HyphenPatternSet {

    private final HyphenPatternTrie patternTrie;
    private final Map<String, List<Integer>> exceptions;

    public HyphenPatternSet(
            HyphenPatternTrie patternTrie,
            Map<String, List<Integer>> exceptions
    ) {
        this.patternTrie = patternTrie;
        this.exceptions = exceptions;
    }

    public HyphenPatternTrie getPatternTrie() {
        return patternTrie;
    }

    public Map<String, List<Integer>> getExceptions() {
        return exceptions;
    }
}