package hyphenation.impl;

import hyphenation.model.HyphenPattern;

import java.util.ArrayList;
import java.util.List;

public class HyphenPatternParser {

    public HyphenPattern parse(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return new HyphenPattern(new int[0], new int[]{0});
        }

        List<Integer> letters = new ArrayList<>();
        List<Integer> levels = new ArrayList<>();
        levels.add(0);

        int i = 0;
        while (i < pattern.length()) {
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

    private int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }
}