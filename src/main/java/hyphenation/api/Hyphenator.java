package hyphenation.api;

import hyphenation.model.HyphenPoint;

import java.util.List;
import java.util.Locale;

/**
 * Интерфейс для поиска переносов внутри одного слова.
 *
 * Реализация должна по слову и языку вернуть все допустимые
 * точки переноса внутри этого слова.
 */
public interface Hyphenator {
    /**
     * Ищет допустимые точки переноса внутри слова.
     *
     * @param word слово, для которого ищутся переносы
     * @param wordStartOffset позиция начала слова в исходной строке
     * @param locale язык слова
     * @return список точек переноса
     */
    List<HyphenPoint> findHyphenPoints(String word, int wordStartOffset, Locale locale);
}
