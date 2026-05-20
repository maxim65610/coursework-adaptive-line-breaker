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

/** Класс, который собирает все допустимые точки разрыва:
после пробелов, после пунктуации, по переносу внутри слова и т.д.*/
public class BreakCandidateCollector {

    private final Hyphenator hyphenator;

    public BreakCandidateCollector(Hyphenator hyphenator) {
        this.hyphenator = hyphenator;
    }

    /**
     * Собирает все допустимые точки разрыва строки.
     *
     * На вход получает:
     * - tokens: результат токенизации текста
     * - locale: язык, нужный для правил переноса
     * - safeOffsets: безопасные UTF-16 позиции, где вообще можно резать строку
     * - textLength: длину исходного текста
     */
    public List<BreakCandidate> collect(
            List<TextToken> tokens,
            Locale locale,
            Set<Integer> safeOffsets,
            int textLength
    ) {
        List<BreakCandidate> candidates = new ArrayList<>();

        candidates.add(BreakCandidate.technicalStart(0));

        for (TextToken token : tokens) {
            if (token.getType() == TokenType.SPACE) {
                addIfSafe(
                        candidates,
                        safeOffsets,
                        BreakCandidate.space(token.getStartUtf16())
                );
            } else if (token.getType() == TokenType.EXPLICIT_BREAK) {
                addIfSafe(
                        candidates,
                        safeOffsets,
                        BreakCandidate.explicitBreak(token.getEndUtf16())
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
                            BreakCandidate.hyphenation(
                                    point.getUtf16Offset(),
                                    point.isAppendHyphen(),
                                    point.getPenalty()
                            )
                    );
                }
            } else if (token.getType() == TokenType.PUNCT) {
                addIfSafe(
                        candidates,
                        safeOffsets,
                        BreakCandidate.punctuation(token.getEndUtf16())
                );
            }
        }

        candidates.add(BreakCandidate.endOfText(textLength));
        return candidates;
    }

    /**
     * Добавляет кандидата только если его позиция безопасна с точки зрения кодировки.
     * Это защита от разрезания строки в некорректной Unicode-позиции,
     * например в середине суррогатной пары.
     */
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
