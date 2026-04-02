package hyphenation.core;

import hyphenation.api.Hyphenator;
import hyphenation.api.UnicodeAnalyzer;
import hyphenation.api.WidthMeasurer;
import hyphenation.api.WordTokenizer;
import hyphenation.model.BreakResult;
import hyphenation.model.HyphenPoint;
import hyphenation.model.Segment;
import hyphenation.model.TextToken;
import hyphenation.model.TokenType;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdaptiveLineBreaker {

    private final UnicodeAnalyzer unicodeAnalyzer;
    private final WordTokenizer tokenizer;
    private final Hyphenator hyphenator;
    private final WidthMeasurer widthMeasurer;

    public AdaptiveLineBreaker(
            UnicodeAnalyzer unicodeAnalyzer,
            WordTokenizer tokenizer,
            Hyphenator hyphenator,
            WidthMeasurer widthMeasurer
    ) {
        this.unicodeAnalyzer = unicodeAnalyzer;
        this.tokenizer = tokenizer;
        this.hyphenator = hyphenator;
        this.widthMeasurer = widthMeasurer;
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
        Set<Integer> breakCandidates = collectBreakCandidates(tokens, locale, safeOffsets);

        List<Segment> segments = buildSegmentsGreedy(
                text,
                breakCandidates,
                safeOffsets,
                font,
                frc,
                maxWidth
        );

        return new BreakResult(segments, tokens);
    }

    private Set<Integer> collectBreakCandidates(
            List<TextToken> tokens,
            Locale locale,
            Set<Integer> safeOffsets
    ) {
        Set<Integer> candidates = new HashSet<>();
        candidates.add(0);

        for (TextToken token : tokens) {
            if (token.getType() == TokenType.SPACE || token.getType() == TokenType.EXPLICIT_BREAK) {
                if (safeOffsets.contains(token.getEndUtf16())) {
                    candidates.add(token.getEndUtf16());
                }
            } else if (token.getType() == TokenType.WORD) {
                List<HyphenPoint> points = hyphenator.findHyphenPoints(
                        token.getText(),
                        token.getStartUtf16(),
                        locale
                );

                for (HyphenPoint point : points) {
                    if (safeOffsets.contains(point.getUtf16Offset())) {
                        candidates.add(point.getUtf16Offset());
                    }
                }
            }
        }

        return candidates;
    }

    private List<Segment> buildSegmentsGreedy(
            String text,
            Set<Integer> breakCandidates,
            Set<Integer> safeOffsets,
            Font font,
            FontRenderContext frc,
            float maxWidth
    ) {
        List<Segment> segments = new ArrayList<>();
        int current = 0;

        while (current < text.length()) {
            int bestBreak = -1;
            float bestWidth = 0.0f;

            for (int i = current + 1; i <= text.length(); i++) {
                if (!safeOffsets.contains(i)) {
                    continue;
                }

                boolean isCandidate = breakCandidates.contains(i) || i == text.length();
                if (!isCandidate) {
                    continue;
                }

                String candidateText = text.substring(current, i);
                float width = widthMeasurer.measure(candidateText, font, frc);

                if (width <= maxWidth) {
                    bestBreak = i;
                    bestWidth = width;
                }
            }

            if (bestBreak == -1) {
                throw new IllegalStateException(
                        "No feasible break point found for substring starting at " + current
                );
            }

            String segmentText = text.substring(current, bestBreak);

            segments.add(new Segment(
                    current,
                    bestBreak,
                    false,
                    bestWidth,
                    segmentText
            ));

            current = bestBreak;
        }

        return segments;
    }
}