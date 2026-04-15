package hyphenation.core;

import hyphenation.api.Hyphenator;
import hyphenation.model.BreakCandidate;
import hyphenation.model.BreakType;
import hyphenation.model.HyphenPoint;
import hyphenation.model.TextToken;
import hyphenation.model.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BreakCandidateCollector {

    private final Hyphenator hyphenator;

    public BreakCandidateCollector(Hyphenator hyphenator) {
        this.hyphenator = hyphenator;
    }

    public List<BreakCandidate> collect(
            List<TextToken> tokens,
            Locale locale,
            Set<Integer> safeOffsets,
            int textLength
    ) {
        List<BreakCandidate> candidates = new ArrayList<>();
        candidates.add(new BreakCandidate(0, BreakType.EXPLICIT_BREAK, false, 0));

        for (TextToken token : tokens) {
            if (token.getType() == TokenType.SPACE) {
                addIfSafe(
                        candidates,
                        safeOffsets,
                        new BreakCandidate(
                                token.getEndUtf16(),
                                BreakType.SPACE,
                                false,
                                0
                        )
                );
            } else if (token.getType() == TokenType.EXPLICIT_BREAK) {
                addIfSafe(
                        candidates,
                        safeOffsets,
                        new BreakCandidate(
                                token.getEndUtf16(),
                                BreakType.EXPLICIT_BREAK,
                                false,
                                Integer.MIN_VALUE
                        )
                );
            } else if (token.getType() == TokenType.WORD) {
                List<HyphenPoint> points = hyphenator.findHyphenPoints(
                        token.getText(),
                        token.getStartUtf16(),
                        locale
                );

                for (HyphenPoint point : points) {
                    addIfSafe(
                            candidates,
                            safeOffsets,
                            new BreakCandidate(
                                    point.getUtf16Offset(),
                                    BreakType.HYPHENATION,
                                    point.isAppendHyphen(),
                                    point.getPenalty()
                            )
                    );
                }
            } else if (token.getType() == TokenType.PUNCT) {
                addIfSafe(
                        candidates,
                        safeOffsets,
                        new BreakCandidate(
                                token.getEndUtf16(),
                                BreakType.PUNCT,
                                false,
                                1
                        )
                );
            }
        }

        candidates.add(new BreakCandidate(textLength, BreakType.END_OF_TEXT, false, 0));
        return candidates;
    }

    private void addIfSafe(
            List<BreakCandidate> candidates,
            Set<Integer> safeOffsets,
            BreakCandidate candidate
    ) {
        if (safeOffsets.contains(candidate.getUtf16Offset())) {
            candidates.add(candidate);
        }
    }
}
