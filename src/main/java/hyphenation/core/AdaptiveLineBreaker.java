package hyphenation.core;

import hyphenation.api.Hyphenator;
import hyphenation.api.UnicodeAnalyzer;
import hyphenation.api.WidthMeasurer;
import hyphenation.api.WordTokenizer;
import hyphenation.model.*;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.*;

//Главный координатор построения правильного разбиения строки на сегменты
public class AdaptiveLineBreaker {

    // Анализатор безопасных границ Unicode.
    // Нужен, чтобы не разрезать строку в середине суррогатной пары
    private final UnicodeAnalyzer unicodeAnalyzer;

    // Токенизатор исходного текста:
    // разбивает строку на слова, пробелы, пунктуацию, переводы строк и т.д.
    private final WordTokenizer tokenizer;

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
        // Коллектору кандидатов нужен Hyphenator,
        // потому что именно он ищет переносы внутри слов.
        this.breakCandidateCollector = new BreakCandidateCollector(hyphenator);
        // Построителю сегментов нужен измеритель ширины,
        // потому что он постоянно проверяет, влезает ли строка в maxWidth.
        this.greedySegmentBuilder = new GreedySegmentBuilder(widthMeasurer);
    }

    /**
     * Главный метод класса.
     * На вход получает:
     * - исходный текст
     * - шрифт
     * - FontRenderContext
     * - максимальную ширину строки
     * - locale для правил переноса
     * На выходе возвращает BreakResult:
     * - список готовых сегментов
     * - список токенов исходного текста
     */
    public BreakResult breakText(
            String text,
            Font font,
            FontRenderContext frc,
            float maxWidth,
            Locale locale
    ) {
        // Базовая валидация входных данных.
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
        // Если locale не передан, используем нейтральную локаль.
        // Это безопаснее, чем работать с null.
        if (locale == null) {
            locale = Locale.ROOT;
        }

        // Шаг 1. Разбиваем исходный текст на токены.
        // Например: WORD, SPACE, PUNCT, EXPLICIT_BREAK и т.п.
        List<TextToken> tokens = tokenizer.tokenize(text);

        // Шаг 2. Собираем безопасные позиции разреза в UTF-16.
        // Потом разрезание строки делаем только по этим индексам.
        Set<Integer> safeOffsets = unicodeAnalyzer.findSafeBreakOffsets(text);

        // Шаг 3. По токенам и правилам переноса собираем все допустимые кандидаты разрыва.
        List<BreakCandidate> breakCandidates = breakCandidateCollector.collect(
                tokens,
                locale,
                safeOffsets,
                text.length()
        );

        // Шаг 4. Из текста и списка кандидатов строим итоговые сегменты.
        // Жадный алгоритм выбирает самую дальнюю подходящую точку,
        // чтобы минимизировать число сегментов.
        List<Segment> segments = greedySegmentBuilder.build(
                text,
                breakCandidates,
                safeOffsets,
                font,
                frc,
                maxWidth
        );

        // Возвращаем и сегменты, и токены.
        // Это полезно, если позже понадобится дополнительная обработка,
        // например выравнивание по ширине.
        return new  BreakResult(segments, tokens);
    }


}