package hyphenation;

import hyphenation.core.AdaptiveLineBreaker;
import hyphenation.api.Hyphenator;
import hyphenation.api.UnicodeAnalyzer;
import hyphenation.api.WidthMeasurer;
import hyphenation.api.WordTokenizer;
import hyphenation.impl.*;
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
        Hyphenator hyphenator = new KnuthLiangHyphenator();
        WidthMeasurer widthMeasurer = new AwtWidthMeasurer();

        AdaptiveLineBreaker breaker = new AdaptiveLineBreaker(
                unicodeAnalyzer,
                tokenizer,
                hyphenator,
                widthMeasurer
        );

        String text = "«Утро в сосновом лесу» Солнце только начинало подниматься над горизонтом, окрашивая небо в нежные оттенки розового и золотого. В сосновом лесу царила особая тишина — та, что бывает только ранним утром. Воздух был чист и свеж, наполнен ароматом хвои и влажной земли. Где-то вдалеке дятел выстукивал свою утреннюю дробь, а на ветке берёзы заливалась звонкая синица. Под ногами мягко пружинил ковёр из мха и прошлогодней хвои. В такой момент особенно остро чувствуешь связь с природой, и кажется, что весь мир замер в ожидании нового дня. Вдруг из-за кустов послышался треск сучьев. Это проснулся бурый медведь и не спеша побрёл к ручью, чтобы напиться холодной воды. Лес просыпался, наполняясь жизнью и движением. И наблюдая за этим, понимаешь: нет ничего прекраснее простых и настоящих мгновений.";
        Font font = new Font("Serif", Font.PLAIN, 18);
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        float maxWidth = 100.0f;
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
