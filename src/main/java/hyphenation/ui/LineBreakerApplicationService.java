package hyphenation.ui;

import hyphenation.api.Hyphenator;
import hyphenation.api.WidthMeasurer;
import hyphenation.core.AdaptiveLineBreaker;
import hyphenation.impl.AwtWidthMeasurer;
import hyphenation.impl.KnuthLiangHyphenator;
import hyphenation.impl.UnicodeAnalyzer;
import hyphenation.impl.WordTokenizer;
import hyphenation.model.BreakResult;
import hyphenation.model.Segment;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.Locale;

/**
 * Сервис приложения.
 * Подготавливает параметры и запускает алгоритм разбиения.
 */
public class LineBreakerApplicationService {

    private final AdaptiveLineBreaker breaker;

    /**
     * Создаёт сервис и собирает зависимости алгоритма.
     */
    public LineBreakerApplicationService() {
        hyphenation.api.UnicodeAnalyzer unicodeAnalyzer = new UnicodeAnalyzer();
        hyphenation.api.WordTokenizer tokenizer = new WordTokenizer();
        Hyphenator hyphenator = new KnuthLiangHyphenator();
        WidthMeasurer widthMeasurer = new AwtWidthMeasurer();

        this.breaker = new AdaptiveLineBreaker(
                unicodeAnalyzer,
                tokenizer,
                hyphenator,
                widthMeasurer
        );
    }

    /**
     * Запускает разбиение текста и возвращает результат в виде строки.
     */
    public String process(
            String text,
            String fontName,
            int fontSize,
            float maxWidth,
            String language
    ) {
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        Locale locale = new Locale(language);

        BreakResult result = breaker.breakText(text, font, frc, maxWidth, locale);
        return buildOutput(text, fontName, fontSize, maxWidth, language, result);
    }

    /**
     * Формирует текстовый вывод результата для интерфейса.
     */
    private String buildOutput(
            String sourceText,
            String fontName,
            int fontSize,
            float maxWidth,
            String language,
            BreakResult result
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("Исходный текст:\n");
        sb.append(sourceText).append("\n\n");

        sb.append("Параметры:\n");
        sb.append("Шрифт: ").append(fontName).append("\n");
        sb.append("Размер: ").append(fontSize).append("\n");
        sb.append("Макс. ширина: ").append(maxWidth).append("\n");
        sb.append("Язык: ").append(language).append("\n\n");

        sb.append("Результат разбиения:\n");
        for (Segment segment : result.getSegments()) {
            sb.append("[")
                    .append(segment.getText())
                    .append("] width=")
                    .append(segment.getWidth())
                    .append(" type=")
                    .append(segment.getBreakType())
                    .append("\n");
        }

        return sb.toString();
    }
}