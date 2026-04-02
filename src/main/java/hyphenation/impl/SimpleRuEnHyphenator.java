package hyphenation.impl;


import hyphenation.api.Hyphenator;
import hyphenation.model.HyphenPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimpleRuEnHyphenator implements Hyphenator {

    private static final int MIN_PART_LENGTH = 2;

    @Override
    public List<HyphenPoint> findHyphenPoints(String word, int wordStartOffset, Locale locale) {
        List<HyphenPoint> result = new ArrayList<>();

        if (word == null || word.isEmpty()) {
            return result;
        }

        int[] offsets = toCodePointOffsets(word);

        if (offsets.length < MIN_PART_LENGTH * 2 + 1) {
            return result;
        }

        String language = (locale == null) ? "" : locale.getLanguage();

        if ("ru".equals(language)) {
            collectRuPoints(word, wordStartOffset, offsets, result);
        } else if ("en".equals(language)) {
            collectEnPoints(word, wordStartOffset, offsets, result);
        } else {
            collectGenericPoints(word, wordStartOffset, offsets, result);
        }

        return result;
    }

    private void collectRuPoints(String word, int wordStartOffset, int[] offsets, List<HyphenPoint> result) {
        collectGenericPoints(word, wordStartOffset, offsets, result);
    }

    private void collectEnPoints(String word, int wordStartOffset, int[] offsets, List<HyphenPoint> result) {
        collectGenericPoints(word, wordStartOffset, offsets, result);
    }

    private void collectGenericPoints(String word, int wordStartOffset, int[] offsets, List<HyphenPoint> result) {
        int codePointCount = offsets.length - 1;

        for (int cpIndex = MIN_PART_LENGTH; cpIndex <= codePointCount - MIN_PART_LENGTH; cpIndex++) {
            int leftUtf16 = offsets[cpIndex - 1];
            int rightUtf16 = offsets[cpIndex];

            int leftCodePoint = word.codePointAt(leftUtf16);
            int rightCodePoint = word.codePointAt(rightUtf16);

            if (Character.isLetter(leftCodePoint) && Character.isLetter(rightCodePoint)) {
                result.add(new HyphenPoint(
                        wordStartOffset + rightUtf16,
                        true,
                        10
                ));
            }
        }
    }

    private int[] toCodePointOffsets(String word) {
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);

        int i = 0;
        while (i < word.length()) {
            int codePoint = word.codePointAt(i);
            i += Character.charCount(codePoint);
            offsets.add(i);
        }

        int[] result = new int[offsets.size()];
        for (int j = 0; j < offsets.size(); j++) {
            result[j] = offsets.get(j);
        }

        return result;
    }
}