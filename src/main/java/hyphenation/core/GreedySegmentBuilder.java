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

/** Класс, который из текста и кандидатов строит итоговые сегменты.
 Здесь реализован жадный выбор самой дальней допустимой точки разрыва. */
public class GreedySegmentBuilder {

    // Компонент для измерения ширины текста в конкретном шрифте.
    // Через него проверяем, помещается ли кандидат в maxWidth.
    private final WidthMeasurer widthMeasurer;

    public GreedySegmentBuilder(WidthMeasurer widthMeasurer) {
        this.widthMeasurer = widthMeasurer;
    }

    /**
     * Строит итоговый список сегментов.
     *
     * Идея алгоритма:
     * - идём слева направо по строке;
     * - для текущей позиции current ищем самую дальнюю допустимую точку разрыва,
     *   которая помещается в maxWidth;
     * - если стандартный кандидат не найден, используем fallback;
     * - повторяем, пока не дойдём до конца текста.
     */
    public List<Segment> build(
            String text,
            List<BreakCandidate> breakCandidates,
            Set<Integer> safeOffsets,
            Font font,
            FontRenderContext frc,
            float maxWidth
    ) {

        List<Segment> segments = new ArrayList<>();

        // current — текущая позиция в исходной строке,
        // с которой начинаем строить очередной сегмент.
        int current = 0;

        // candidateStartIndex — индекс первого кандидата,
        // с которого имеет смысл начинать поиск на текущем шаге.
        // Это оптимизация: не смотрим снова кандидаты, которые уже левее current.
        int candidateStartIndex = 0;

        while (current < text.length()) {
            BreakCandidate bestCandidate = null;
            int bestCandidateIndex = -1;
            float bestWidth = 0.0f;

            // Ищем лучший кандидат для сегмента, начинающегося в current.
            for (int i = candidateStartIndex; i < breakCandidates.size(); i++) {
                BreakCandidate candidate = breakCandidates.get(i);
                int end = candidate.getUtf16Offset();

                // Кандидаты левее или ровно в current уже не подходят.
                if (end <= current) {
                    continue;
                }

                // Берём подстроку от current до кандидата.
                String rawText = text.substring(current, end);

                // Если это явный перевод строки, убираем сам символ перевода
                // из видимого текста сегмента.
                if (candidate.getType() == BreakType.EXPLICIT_BREAK) {
                    rawText = trimTrailingNewline(rawText);
                }

                // Если кандидат требует дефис, добавляем его к видимому тексту.
                String visibleText = candidate.isAppendHyphen() ? rawText + "-" : rawText;

                // Измеряем ширину строки
                float width = widthMeasurer.measure(visibleText, font, frc);

                // Если сегмент помещается, кандидат подходит.
                if (width <= maxWidth) {
                    if (candidate.getType() == BreakType.EXPLICIT_BREAK) {
                        bestCandidate = candidate;
                        bestCandidateIndex = i;
                        bestWidth = width;
                        break;
                    }
                    if (isBetterCandidate(candidate, bestCandidate)) {
                        bestCandidate = candidate;
                        bestCandidateIndex = i;
                        bestWidth = width;
                    }
                } else {
                    break;
                }
            }

            // Если обычных кандидатов не нашлось, используем fallback.
            if (bestCandidate == null) {
                FallbackBreak fallback = findFallbackBreak(text, current, safeOffsets, font, frc, maxWidth);

                if (fallback == null) {
                    throw new IllegalStateException(
                            "No feasible break point found for substring starting at " + current
                    );
                }

                // Fallback создаёт сегмент без дефиса.
                segments.add(new Segment(
                        current,
                        fallback.getEndUtf16(),
                        false,
                        fallback.getWidth(),
                        fallback.getText(),
                        BreakType.END_OF_TEXT
                ));

                // Переходим к следующей позиции.
                current = fallback.getEndUtf16();
                continue;
            }

            // Если обычный кандидат найден, строим нормальный сегмент.
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

            if (bestCandidate.getType() == BreakType.SPACE) {
                current = skipLeadingSpaces(text, bestCandidate.getUtf16Offset());
            } else {
                current = bestCandidate.getUtf16Offset();
            }

            candidateStartIndex = bestCandidateIndex + 1;
        }

        return segments;
    }

    /**
     * Fallback используется, если среди стандартных кандидатов
     * не нашлось ни одной точки разрыва, которая помещается в maxWidth.
     *
     * Логика:
     * 1. Ищем самую дальнюю безопасную Unicode-границу, которая помещается.
     * 2. Если даже такой нет, берём ближайшую безопасную границу справа,
     *    чтобы не упасть и не зациклиться.
     */
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

        // Сначала ищем лучшую безопасную границу, которая ещё помещается.
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
                // Если уже не помещается, дальше будет только хуже.
                break;
            }
        }

        if (bestEnd != -1) {
            return new FallbackBreak(bestEnd, bestWidth, bestText);
        }

        // Если не помещается даже минимальный осмысленный кусок,
        // берём первую безопасную границу справа.
        // Это аварийный режим, чтобы алгоритм не падал.
        for (int end = current + 1; end <= text.length(); end++) {
            if (safeOffsets.contains(end)) {
                String candidateText = text.substring(current, end);
                float width = widthMeasurer.measure(candidateText, font, frc);
                return new FallbackBreak(end, width, candidateText);
            }
        }

        return null;
    }

    /**
     * Сравнение кандидатов.
     * В текущей реализации стратегия очень простая:
     * лучший кандидат — тот, который заканчивается дальше всего.
     * Это соответствует жадной стратегии минимизации числа сегментов.
     */
    private boolean isBetterCandidate(BreakCandidate candidate, BreakCandidate currentBest) {
        if (currentBest == null) {
            return true;
        }

        return candidate.getUtf16Offset() > currentBest.getUtf16Offset();
    }

    /**
     * Убирает символы перевода строки из конца текста сегмента.
     * Нужно для случая EXPLICIT_BREAK:
     * строка должна завершиться в этой точке,
     * но сам символ '\n' или '\r' не должен быть виден в сегменте.
     */
    private String trimTrailingNewline(String text) {
        if (text.endsWith("\r\n")) {
            return text.substring(0, text.length() - 2);
        }
        if (text.endsWith("\n") || text.endsWith("\r")) {
            return text.substring(0, text.length() - 1);
        }
        return text;
    }

    /**
     * Пропускает пробельные символы в начале следующего сегмента.
     * Не трогает явные переводы строки.
     */
    private int skipLeadingSpaces(String text, int offset) {
        int index = offset;

        while (index < text.length()) {
            int codePoint = text.codePointAt(index);

            if (!Character.isWhitespace(codePoint) || codePoint == '\n' || codePoint == '\r') {
                break;
            }

            index += Character.charCount(codePoint);
        }

        return index;
    }

    /**
     * Маленький служебный контейнер для результата fallback.
     */
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