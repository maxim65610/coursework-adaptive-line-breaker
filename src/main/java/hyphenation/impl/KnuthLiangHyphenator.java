package hyphenation.impl;

import hyphenation.api.Hyphenator;
import hyphenation.model.HyphenPattern;
import hyphenation.model.HyphenPatternSet;
import hyphenation.model.HyphenPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KnuthLiangHyphenator implements Hyphenator {

    private static final int MIN_PART_LENGTH = 2;
    private static final int WORD_BOUNDARY = '.';

    private final HyphenPatternRepository patternRepository;

    public KnuthLiangHyphenator() {
        this.patternRepository = new HyphenPatternRepository();
    }

    @Override
    public List<HyphenPoint> findHyphenPoints(String word, int wordStartOffset, Locale locale) {
        List<HyphenPoint> result = new ArrayList<>();

        if (word == null || word.isEmpty()) {
            return result;
        }

        Locale safeLocale = (locale == null) ? Locale.ROOT : locale;
        String lowerWord = word.toLowerCase(safeLocale);

        int[] codePointOffsets = toCodePointOffsets(word);
        int codePointCount = codePointOffsets.length - 1;

        if (codePointCount < MIN_PART_LENGTH * 2) {
            return result;
        }

        HyphenPatternSet patternSet = patternRepository.getPatternSet(safeLocale);

        List<HyphenPoint> exceptionPoints = buildExceptionPoints(
                lowerWord,
                wordStartOffset,
                codePointOffsets,
                codePointCount,
                patternSet.getExceptions()
        );

        if (!exceptionPoints.isEmpty()) {
            return exceptionPoints;
        }

        int[] wordCodePoints = toCodePoints(lowerWord);
        int[] levels = computeLevels(wordCodePoints, patternSet.getPatterns());

        for (int cpIndex = MIN_PART_LENGTH; cpIndex <= codePointCount - MIN_PART_LENGTH; cpIndex++) {
            int levelIndex = cpIndex + 1; // +1 из-за начальной границы '.'

            if (levelIndex < levels.length && levels[levelIndex] % 2 == 1) {
                result.add(new HyphenPoint(
                        wordStartOffset + codePointOffsets[cpIndex],
                        true,
                        10 - levels[levelIndex]
                ));
            }
        }

        return result;
    }

    private List<HyphenPoint> buildExceptionPoints(
            String lowerWord,
            int wordStartOffset,
            int[] codePointOffsets,
            int codePointCount,
            Map<String, List<Integer>> exceptions
    ) {
        List<HyphenPoint> result = new ArrayList<>();

        List<Integer> exceptionPositions = exceptions.get(lowerWord);
        if (exceptionPositions == null) {
            return result;
        }

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

    private boolean isValidHyphenPosition(int codePointIndex, int codePointCount) {
        return codePointIndex >= MIN_PART_LENGTH
                && codePointIndex <= codePointCount - MIN_PART_LENGTH;
    }

    private int[] computeLevels(int[] wordCodePoints, List<HyphenPattern> patterns) {
        int[] dottedWord = addWordBoundaries(wordCodePoints);
        int[] levels = new int[dottedWord.length + 1];

        for (HyphenPattern pattern : patterns) {
            int[] patternLetters = pattern.getLetters();
            int[] patternLevels = pattern.getLevels();

            if (patternLetters.length == 0) {
                continue;
            }

            for (int start = 0; start <= dottedWord.length - patternLetters.length; start++) {
                if (matchesAt(dottedWord, start, patternLetters)) {
                    applyPattern(levels, start, patternLevels);
                }
            }
        }

        return levels;
    }

    private boolean matchesAt(int[] text, int start, int[] patternLetters) {
        for (int i = 0; i < patternLetters.length; i++) {
            if (text[start + i] != patternLetters[i]) {
                return false;
            }
        }
        return true;
    }

    private void applyPattern(int[] levels, int start, int[] patternLevels) {
        for (int i = 0; i < patternLevels.length; i++) {
            levels[start + i] = Math.max(levels[start + i], patternLevels[i]);
        }
    }

    private int[] addWordBoundaries(int[] wordCodePoints) {
        int[] result = new int[wordCodePoints.length + 2];
        result[0] = WORD_BOUNDARY;
        System.arraycopy(wordCodePoints, 0, result, 1, wordCodePoints.length);
        result[result.length - 1] = WORD_BOUNDARY;
        return result;
    }

    private int[] toCodePoints(String text) {
        return text.codePoints().toArray();
    }

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