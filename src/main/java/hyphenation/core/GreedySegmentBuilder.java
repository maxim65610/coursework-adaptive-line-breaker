package hyphenation.core;

import hyphenation.api.WidthMeasurer;
import hyphenation.model.BreakCandidate;
import hyphenation.model.BreakType;
import hyphenation.model.Segment;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class GreedySegmentBuilder {

    private final WidthMeasurer widthMeasurer;

    public GreedySegmentBuilder(WidthMeasurer widthMeasurer) {
        this.widthMeasurer = widthMeasurer;
    }

    public List<Segment> build(
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
                    if (isBetterCandidate(candidate, bestCandidate)) {
                        bestCandidate = candidate;
                        bestCandidateIndex = i;
                        bestWidth = width;
                    }
                } else {
                    break;
                }
            }

            if (bestCandidate == null) {
                FallbackBreak fallback = findFallbackBreak(text, current, safeOffsets, font, frc, maxWidth);

                if (fallback == null) {
                    throw new IllegalStateException(
                            "No feasible break point found for substring starting at " + current
                    );
                }

                segments.add(new Segment(
                        current,
                        fallback.getEndUtf16(),
                        false,
                        fallback.getWidth(),
                        fallback.getText(),
                        BreakType.END_OF_TEXT
                ));

                current = fallback.getEndUtf16();
                continue;
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

    private FallbackBreak findFallbackBreak(
            String text,
            int current,
            Set<Integer> safeOffsets,
            Font font,
            FontRenderContext frc,
            float maxWidth
    ) {
        int bestEnd = -1;
        float bestWidth = 0.0f;
        String bestText = null;

        for (int end = current + 1; end <= text.length(); end++) {
            if (!safeOffsets.contains(end)) {
                continue;
            }

            String candidateText = text.substring(current, end);
            float width = widthMeasurer.measure(candidateText, font, frc);

            if (width <= maxWidth) {
                bestEnd = end;
                bestWidth = width;
                bestText = candidateText;
            } else {
                break;
            }
        }

        if (bestEnd != -1) {
            return new FallbackBreak(bestEnd, bestWidth, bestText);
        }

        for (int end = current + 1; end <= text.length(); end++) {
            if (safeOffsets.contains(end)) {
                String candidateText = text.substring(current, end);
                float width = widthMeasurer.measure(candidateText, font, frc);
                return new FallbackBreak(end, width, candidateText);
            }
        }

        return null;
    }

    private boolean isBetterCandidate(BreakCandidate candidate, BreakCandidate currentBest) {
        if (currentBest == null) {
            return true;
        }

        return candidate.getUtf16Offset() > currentBest.getUtf16Offset();
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

    private static class FallbackBreak {
        private final int endUtf16;
        private final float width;
        private final String text;

        public FallbackBreak(int endUtf16, float width, String text) {
            this.endUtf16 = endUtf16;
            this.width = width;
            this.text = text;
        }

        public int getEndUtf16() {
            return endUtf16;
        }

        public float getWidth() {
            return width;
        }

        public String getText() {
            return text;
        }
    }
}