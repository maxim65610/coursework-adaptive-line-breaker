package hyphenation.impl;

import hyphenation.model.HyphenPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Парсер одного паттерна переноса.
 */
public class HyphenPatternParser {

    /**
     * Разбирает строку паттерна и возвращает объект HyphenPattern.
     * Преобразует строковый паттерн вида:
     *   a1bc3
     * во внутреннее представление:
     * - letters = [a, b, c]
     * - levels = [0, 1, 0, 3]
     * Идея такая:
     * - буквы паттерна хранятся отдельно
     * - цифры задают уровни между буквами
     */
    public HyphenPattern parse(String pattern) {
        // Пустой паттерн превращаем в "пустую" структуру.
        // Букв нет, уровень один — нулевой.
        if (pattern == null || pattern.isEmpty()) {
            return new HyphenPattern(new int[0], new int[]{0});
        }

        List<Integer> letters = new ArrayList<>();
        List<Integer> levels = new ArrayList<>();

        // До первой буквы тоже существует позиция уровня,
        // поэтому начинаем с 0.
        levels.add(0);

        int i = 0;
        while (i < pattern.length()) {
            // Идём по строке паттерна не по char, а по code points,
            // чтобы корректно работать с Unicode(на всякий случай)
            int codePoint = pattern.codePointAt(i);
            i += Character.charCount(codePoint);

            if (Character.isDigit(codePoint)) {
                levels.set(levels.size() - 1, codePoint - '0');
            } else {
                letters.add(codePoint);
                levels.add(0);
            }
        }

        return new HyphenPattern(
                toIntArray(letters),
                toIntArray(levels)
        );
    }

    /**
     * Преобразует список Integer в массив int[].
     * Это нужно, потому что:
     * - во время разбора удобно накапливать данные в List
     * - а хранить и использовать паттерн удобнее в компактных int[] массивах
     */
    private int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }
}