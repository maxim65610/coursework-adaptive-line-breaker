package hyphenation.model;

import java.util.List;


/**
 * Итог работы алгоритма разбиения текста.
 * Хранит:
 * - готовые сегменты
 * - исходные токены текста
 */
public class BreakResult {
    private final List<Segment> segments;
    private final List<TextToken> tokens;

    public BreakResult(List<Segment> segments, List<TextToken> tokens) {
        this.segments = segments;
        this.tokens = tokens;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public List<TextToken> getTokens() {
        return tokens;
    }
}