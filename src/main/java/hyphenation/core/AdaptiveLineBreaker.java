package hyphenation.core;

import hyphenation.api.Hyphenator;
import hyphenation.api.UnicodeAnalyzer;
import hyphenation.api.WidthMeasurer;
import hyphenation.api.WordTokenizer;
import hyphenation.model.*;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.*;

public class AdaptiveLineBreaker {

    private final UnicodeAnalyzer unicodeAnalyzer;
    private final WordTokenizer tokenizer;
    private final WidthMeasurer widthMeasurer;
    private final BreakCandidateCollector breakCandidateCollector;
    private final GreedySegmentBuilder greedySegmentBuilder;

    public AdaptiveLineBreaker(
            UnicodeAnalyzer unicodeAnalyzer,
            WordTokenizer tokenizer,
            Hyphenator hyphenator,
            WidthMeasurer widthMeasurer
    ) {
        this.unicodeAnalyzer = unicodeAnalyzer;
        this.tokenizer = tokenizer;
        this.widthMeasurer = widthMeasurer;
        this.breakCandidateCollector = new BreakCandidateCollector(hyphenator);
        this.greedySegmentBuilder = new GreedySegmentBuilder(widthMeasurer);
    }

    public BreakResult breakText(
            String text,
            Font font,
            FontRenderContext frc,
            float maxWidth,
            Locale locale
    ) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (font == null) {
            throw new IllegalArgumentException("font must not be null");
        }
        if (frc == null) {
            throw new IllegalArgumentException("FontRenderContext must not be null");
        }
        if (maxWidth <= 0) {
            throw new IllegalArgumentException("maxWidth must be positive");
        }
        if (locale == null) {
            locale = Locale.ROOT;
        }

        List<TextToken> tokens = tokenizer.tokenize(text);
        Set<Integer> safeOffsets = unicodeAnalyzer.findSafeBreakOffsets(text);
        List<BreakCandidate> breakCandidates = breakCandidateCollector.collect(
                tokens,
                locale,
                safeOffsets,
                text.length()
        );

        List<Segment> segments = greedySegmentBuilder.build(
                text,
                breakCandidates,
                safeOffsets,
                font,
                frc,
                maxWidth
        );

        return new BreakResult(segments, tokens);
    }


}