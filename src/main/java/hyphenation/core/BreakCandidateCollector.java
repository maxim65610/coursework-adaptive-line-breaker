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
        // Добавляем стартовую позицию 0 как технический кандидат.
        // Она нужна как опорная точка для алгоритма построения сегментов.
        candidates.add(new BreakCandidate(0, BreakType.EXPLICIT_BREAK, false, 0));

        // Проходим по всем токенам и в зависимости от их типа
        // добавляем допустимые точки разрыва.
        for (TextToken token : tokens) {

            // После пробела можно завершить сегмент.
            if (token.getType() == TokenType.SPACE) {
            	// TODO: Здесь ошибка в логике. Пробельный сегмент, если он 
            	// состоит из нескольких пробелов, рассматривается как неделимый,
            	// а на самом деле, если он оказывается в конце строки, его можно вообще удалить
            	// то есть позиция разрыва оказывается не в конце этого сегмента, а в начале, 
            	// а сам пробельный сегмент после разрыва строки игнорируется.
                addIfSafe(
                        candidates,
                        safeOffsets,
                        // TODO: заменить конструкторы на фабричные методы (это позволит спрятать комбинации параметров внутрь)
                        new BreakCandidate(
                                token.getEndUtf16(),
                                BreakType.SPACE,
                                false,
                                0
                        )
                );
            }
            // Явный перевод строки — это принудительная точка разрыва.
            // Для него задаётся очень "сильный" штраф через Integer.MIN_VALUE,
            // чтобы такой разрыв имел особый приоритет в логике алгоритма(на будущее).
            else if (token.getType() == TokenType.EXPLICIT_BREAK) {
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
            }
            // Если токен — слово, то возможные точки разрыва ищутся через Hyphenator.
            // Здесь подключаются языковые правила переноса.
            else if (token.getType() == TokenType.WORD) {
                List<HyphenPoint> points = hyphenator.findHyphenPoints(
                        token.getText(),
                        token.getStartUtf16(),
                        locale
                );

                // Каждую найденную точку переноса превращаем в BreakCandidate.
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
            }
            // После знаков пунктуации тоже можно завершать сегмент.
            // TODO: пунктуационные знаки нельзя отрывать от слова
            else if (token.getType() == TokenType.PUNCT) {
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

        // Всегда добавляем конец текста как допустимую точку.
        // Это нужно, чтобы алгоритм мог завершить последний сегмент.
        candidates.add(new BreakCandidate(textLength, BreakType.END_OF_TEXT, false, 0));
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
