package hyphenation.impl;

import hyphenation.api.Hyphenator;
import hyphenation.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Реализация переносов слов по мотивам алгоритма Knuth–Liang.
 * Класс работает только с одним словом за раз и ищет допустимые точки переноса
 * внутри этого слова. Сначала проверяется словарь исключений, а если слово там
 * не найдено, то используются языковые паттерны переноса.
 */
public class KnuthLiangHyphenator implements Hyphenator {

    // Минимальная длина части слова слева и справа от переноса.
    // Например, если MIN_PART_LENGTH = 2, то перенос нельзя ставить
    // раньше 2-й позиции и позже, чем за 2 символа до конца слова.
    private static final int MIN_PART_LENGTH = 2;

    // Специальный символ границы слова.
    // Используется при построении массива вида ".word.",
    // чтобы паттерны могли учитывать начало и конец слова.
    private static final int WORD_BOUNDARY = '.';

    // Репозиторий, который хранит паттерны и исключения для языков.
    // Сам hyphenator не знает, откуда они взялись:
    private final HyphenPatternRepository patternRepository;

    public KnuthLiangHyphenator() {
        this.patternRepository = new HyphenPatternRepository();
    }

    /**
     * Ищет все допустимые точки переноса внутри одного слова.
     * На вход получает:
     * - word: само слово
     * - wordStartOffset: позицию начала слова в исходной строке
     * - locale: язык слова
     * Возвращает список HyphenPoint:
     * - позиция переноса в UTF-16 offset относительно исходной строки
     * - нужно ли добавлять дефис
     * - штраф (penalty)
     */
    @Override
    public List<HyphenPoint> findHyphenPoints(String word, int wordStartOffset, Locale locale) {
        List<HyphenPoint> result = new ArrayList<>();

        // Пустое слово переносить бессмысленно.
        if (word == null || word.isEmpty()) {
            return result;
        }

        // Если locale не передан, используем нейтральную локаль.
        Locale safeLocale = (locale == null) ? Locale.ROOT : locale;

        // Приводим слово к нижнему регистру.
        // Это важно, потому что паттерны и словарь исключений
        // обычно хранятся в lower-case.
        String lowerWord = word.toLowerCase(safeLocale);

        // Строим таблицу соответствия:
        // индекс по code points -> UTF-16 offset.
        // Это нужно, потому что алгоритм логически работает по Unicode code points,
        // а Java-строки режутся по UTF-16 индексам.
        int[] codePointOffsets = toCodePointOffsets(word);

        // Количество Unicode code points в слове.
        int codePointCount = codePointOffsets.length - 1;

        // Если слово слишком короткое, переносы невозможны.
        if (codePointCount < MIN_PART_LENGTH * 2) {
            return result;
        }

        // Получаем набор правил для нужного языка:
        // паттерны + словарь исключений.
        HyphenPatternSet patternSet = patternRepository.getPatternSet(safeLocale);


        // Сначала проверяем словарь исключений.
        // Если слово найдено там, именно эти позиции имеют приоритет над паттернами.
        Map<String, List<Integer>> exceptions = patternSet.getExceptions();

        if (exceptions.containsKey(lowerWord)) {
            return buildExceptionPoints(
                    lowerWord,
                    wordStartOffset,
                    codePointOffsets,
                    codePointCount,
                    exceptions
            );
        }

        // Если слово не найдено в исключениях, работаем через паттерны.
        // Преобразуем слово в массив Unicode code points.
        int[] wordCodePoints = toCodePoints(lowerWord);

        // Вычисляем массив уровней переноса.
        // Здесь происходит "наложение" всех паттернов на слово.
        int[] levels = computeLevels(wordCodePoints, patternSet.getPatternTrie());

        // Проходим по допустимым позициям переноса.
        // cpIndex — это позиция между code points.
        for (int cpIndex = MIN_PART_LENGTH; cpIndex <= codePointCount - MIN_PART_LENGTH; cpIndex++) {
            // В levels есть сдвиг на 1, потому что слово было расширено до ".word."
            int levelIndex = cpIndex + 1;

            // Нечётный уровень означает, что перенос разрешён.
            if (levelIndex < levels.length && levels[levelIndex] % 2 == 1) {
                result.add(new HyphenPoint(
                        wordStartOffset + codePointOffsets[cpIndex], // Переводим позицию внутри слова в позицию в исходной строке.
                        true, // При переносе внутри слова добавляем дефис.
                        10 - levels[levelIndex] // Чем выше уровень, тем меньше штраф.
                ));
            }
        }

        return result;
    }

    /**
     * Проверяет, есть ли слово в словаре исключений,
     * и если есть — строит готовые точки переноса.
     */
    private List<HyphenPoint> buildExceptionPoints(
            String lowerWord,
            int wordStartOffset,
            int[] codePointOffsets,
            int codePointCount,
            Map<String, List<Integer>> exceptions
    ) {
        List<HyphenPoint> result = new ArrayList<>();

        // Берём позиции переноса для слова из словаря исключений.
        List<Integer> exceptionPositions = exceptions.get(lowerWord);
        if (exceptionPositions == null) {
            return result;
        }

        // Каждую позицию исключения превращаем в HyphenPoint.
        for (int point : exceptionPositions) {
            if (isValidHyphenPosition(point, codePointCount)) {
                result.add(new HyphenPoint(
                        wordStartOffset + codePointOffsets[point],
                        true,
                        10
                ));
            }
        }

        return result;
    }

    /**
     * Проверяет, допустима ли позиция переноса с точки зрения минимальной длины частей.
     */

    private boolean isValidHyphenPosition(int codePointIndex, int codePointCount) {
        return codePointIndex >= MIN_PART_LENGTH
                && codePointIndex <= codePointCount - MIN_PART_LENGTH;
    }

    /**
     * Вычисляет массив уровней переноса для слова с использованием префиксного дерева паттернов.
     * Логика:
     * 1. Добавляем к слову фиктивные границы: ".word."
     * 2. Для каждой стартовой позиции в слове начинаем обход trie от корня
     * 3. Идём вправо по символам слова, пока в trie существует соответствующий переход
     * 4. Если в текущем узле заканчиваются паттерны, накладываем их уровни на общий массив
     * 5. В каждой позиции сохраняется максимальный уровень
     * 6. Нечётные уровни означают допустимые точки переноса
     */
    private int[] computeLevels(int[] wordCodePoints, HyphenPatternTrie patternTrie) {
        // Превращаем слово в ".word."
        int[] dottedWord = addWordBoundaries(wordCodePoints);

        // Уровни ставятся между символами,
        // поэтому количество позиций = длина массива символов + 1.
        int[] levels = new int[dottedWord.length + 1];

        // Перебираем все возможные стартовые позиции в слове.
        for (int start = 0; start < dottedWord.length; start++) {
            HyphenPatternTrieNode currentNode = patternTrie.getRoot();

            // Идём вправо от текущей стартовой позиции,
            // пока в trie существует переход по очередному символу слова.
            for (int pos = start; pos < dottedWord.length; pos++) {
                int letter = dottedWord[pos];

                currentNode = currentNode.getChildren().get(letter);

                // Если перехода нет, дальше совпадений уже не будет.
                if (currentNode == null) {
                    break;
                }

                // Если в текущем узле заканчиваются какие-то паттерны,
                // значит они совпали с подстрокой слова, начинающейся в start.
                for (HyphenPattern pattern : currentNode.getTerminalPatterns()) {
                    applyPattern(levels, start, pattern.getLevels());
                }
            }
        }

        return levels;
    }


    /**
     * Накладывает уровни паттерна на общий массив уровней.
     * Если несколько паттернов влияют на одну и ту же позицию,
     * остаётся максимальный уровень.
     */
    private void applyPattern(int[] levels, int start, int[] patternLevels) {
        for (int i = 0; i < patternLevels.length; i++) {
            levels[start + i] = Math.max(levels[start + i], patternLevels[i]);
        }
    }

    /**
     * Добавляет фиктивные границы слова, точки слева и справа
     */
    private int[] addWordBoundaries(int[] wordCodePoints) {
        int[] result = new int[wordCodePoints.length + 2];
        result[0] = WORD_BOUNDARY;
        System.arraycopy(wordCodePoints, 0, result, 1, wordCodePoints.length);
        result[result.length - 1] = WORD_BOUNDARY;
        return result;
    }

    /**
     * Преобразует строку в массив Unicode code points.
     */
    private int[] toCodePoints(String text) {
        return text.codePoints().toArray();
    }


    /**
     * Строит таблицу соответствия:
     * номер code point -> UTF-16 offset.
     */
    private int[] toCodePointOffsets(String word) {
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        int utf16Index = 0;
        while (utf16Index < word.length()) {
            int codePoint = word.codePointAt(utf16Index);
            utf16Index += Character.charCount(codePoint);
            offsets.add(utf16Index);
        }

        int[] result = new int[offsets.size()];
        for (int i = 0; i < offsets.size(); i++) {
            result[i] = offsets.get(i);
        }

        return result;
    }
}