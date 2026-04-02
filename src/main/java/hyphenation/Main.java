package hyphenation;

import hyphenation.core.AdaptiveLineBreaker;
import hyphenation.api.Hyphenator;
import hyphenation.api.UnicodeAnalyzer;
import hyphenation.api.WidthMeasurer;
import hyphenation.api.WordTokenizer;
import hyphenation.impl.AwtWidthMeasurer;
import hyphenation.impl.SimpleRuEnHyphenator;
import hyphenation.impl.SimpleUnicodeAnalyzer;
import hyphenation.impl.SimpleWordTokenizer;
import hyphenation.model.BreakResult;
import hyphenation.model.Segment;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.util.Locale;

public class Main {

    public static void main(String[] args) {
        UnicodeAnalyzer unicodeAnalyzer = new SimpleUnicodeAnalyzer();
        WordTokenizer tokenizer = new SimpleWordTokenizer();
        Hyphenator hyphenator = new SimpleRuEnHyphenator();
        WidthMeasurer widthMeasurer = new AwtWidthMeasurer();

        AdaptiveLineBreaker breaker = new AdaptiveLineBreaker(
                unicodeAnalyzer,
                tokenizer,
                hyphenator,
                widthMeasurer
        );

        String text = "Разработка алгоритма адаптивного переноса слов для систем верстки";
        Font font = new Font("Serif", Font.PLAIN, 18);
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        float maxWidth = 250.0f;
        Locale locale = new Locale("ru");

        BreakResult result = breaker.breakText(text, font, frc, maxWidth, locale);

        System.out.println("Исходный текст:");
        System.out.println(text);
        System.out.println();

        System.out.println("Результат разбиения:");
        for (Segment segment : result.getSegments()) {
            System.out.println(
                    "[" + segment.getText() + "] width=" + segment.getWidth()
            );
        }
    }
}
