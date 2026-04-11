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
        List<BreakCandidate> breakCandidates = collectBreakCandidates(
                tokens,
                locale,
                safeOffsets,
                text.length()
        );

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

    private List<BreakCandidate> collectBreakCandidates(
            List<TextToken> tokens,
            Locale locale,
            Set<Integer> safeOffsets,
            int textLength
    ) {
        List<BreakCandidate> candidates = new ArrayList<>();
        candidates.add(new BreakCandidate(0, BreakType.EXPLICIT_BREAK, false, 0));

        for (TextToken token : tokens) {
            if (token.getType() == TokenType.SPACE) {
                if (safeOffsets.contains(token.getEndUtf16())) {
                    candidates.add(new BreakCandidate(
                            token.getEndUtf16(),
                            BreakType.SPACE,
                            false,
                            0
                    ));
                }
            } else if (token.getType() == TokenType.EXPLICIT_BREAK) {
                if (safeOffsets.contains(token.getEndUtf16())) {
                    candidates.add(new BreakCandidate(
                            token.getEndUtf16(),
                            BreakType.EXPLICIT_BREAK,
                            false,
                            Integer.MIN_VALUE
                    ));
                }
            } else if (token.getType() == TokenType.WORD) {
                List<HyphenPoint> points = hyphenator.findHyphenPoints(
                        token.getText(),
                        token.getStartUtf16(),
                        locale
                );

                for (HyphenPoint point : points) {
                    if (safeOffsets.contains(point.getUtf16Offset())) {
                        candidates.add(new BreakCandidate(
                                point.getUtf16Offset(),
                                BreakType.HYPHENATION,
                                point.isAppendHyphen(),
                                point.getPenalty()
                        ));
                    }
                }
            } else if (token.getType() == TokenType.PUNCT) {
                if (safeOffsets.contains(token.getEndUtf16())) {
                    candidates.add(new BreakCandidate(
                            token.getEndUtf16(),
                            BreakType.SPACE,
                            false,
                            1
                    ));
                }
            }
        }

        candidates.add(new BreakCandidate(textLength, BreakType.END_OF_TEXT, false, 0));
        return candidates;
    }

    private List<Segment> buildSegmentsGreedy(
            String text,
            List<BreakCandidate> breakCandidates,
            Set<Integer> safeOffsets,
            Font font,
            FontRenderContext frc,
            float maxWidth
    ) {
        breakCandidates.sort(Comparator.comparingInt(BreakCandidate::getUtf16Offset));

        List<Segment> segments = new ArrayList<>();
        int current = 0;
        int candidateStartIndex = 0;

        while (current < text.length()) {
            BreakCandidate bestCandidate = null;
            int bestCandidateIndex = -1;
            float bestWidth = 0.0f;

            for (int i = candidateStartIndex; i < breakCandidates.size(); i++) {
                BreakCandidate candidate = breakCandidates.get(i);
                int end = candidate.getUtf16Offset();

                if (end <= current) {
                    continue;
                }
                if (!safeOffsets.contains(end)) {
                    continue;
                }

                String rawText = text.substring(current, end);

                if (candidate.getType() == BreakType.EXPLICIT_BREAK) {
                    rawText = trimTrailingNewline(rawText);
                }

                String visibleText = candidate.isAppendHyphen() ? rawText + "-" : rawText;
                float width = widthMeasurer.measure(visibleText, font, frc);

                if (width <= maxWidth) {
                    bestCandidate = candidate;
                    bestCandidateIndex = i;
                    bestWidth = width;
                } else {
                    break;
                }
            }

            if (bestCandidate == null) {
                throw new IllegalStateException(
                        "No feasible break point found for substring starting at " + current
                );
            }

            String rawSegmentText = text.substring(current, bestCandidate.getUtf16Offset());

            if (bestCandidate.getType() == BreakType.EXPLICIT_BREAK) {
                rawSegmentText = trimTrailingNewline(rawSegmentText);
            }

            String visibleSegmentText = bestCandidate.isAppendHyphen()
                    ? rawSegmentText + "-"
                    : rawSegmentText;

            segments.add(new Segment(
                    current,
                    bestCandidate.getUtf16Offset(),
                    bestCandidate.isAppendHyphen(),
                    bestWidth,
                    visibleSegmentText,
                    bestCandidate.getType()
            ));

            current = bestCandidate.getUtf16Offset();
            candidateStartIndex = bestCandidateIndex + 1;
        }

        return segments;
    }

    private String trimTrailingNewline(String text) {
        if (text.endsWith("\r\n")) {
            return text.substring(0, text.length() - 2);
        }
        if (text.endsWith("\n") || text.endsWith("\r")) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }
}