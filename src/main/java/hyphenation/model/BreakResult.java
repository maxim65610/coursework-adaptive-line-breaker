package hyphenation.model;

import java.util.List;

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