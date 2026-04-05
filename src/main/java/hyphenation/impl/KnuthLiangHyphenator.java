package hyphenation.impl;

import hyphenation.api.Hyphenator;
import hyphenation.model.HyphenPoint;

import java.util.*;

public class KnuthLiangHyphenator implements Hyphenator {

    private static final int MIN_PART_LENGTH = 2;

    private static final String[] RU_PATTERNS = {
            "а1", "б1", "в1", "г1", "д1", "е1", "ё1", "ж1", "з1", "и1", "й1", "к1", "л1", "м1",
            "н1", "о1", "п1", "р1", "с1", "т1", "у1", "ф1", "х1", "ц1", "ч1", "ш1", "щ1", "ъ1",
            "ы1", "ь1", "э1", "ю1", "я1",
            "2б", "2в", "2г", "2д", "2ж", "2з", "2к", "2л", "2м", "2н", "2п", "2р", "2с", "2т",
            "2ф", "2х", "2ц", "2ч", "2ш", "2щ", "2ъ", "2ь"

    };

    private static final String[] EN_PATTERNS = {
            "1a", "1e", "1i", "1o", "1u", "1y", "2b", "2c", "2d", "2f", "2g", "2h", "2j", "2k",
            "2l", "2m", "2n", "2p", "2q", "2r", "2s", "2t", "2v", "2w", "2x", "2z"
    };

    private static final Map<String, List<Integer>> EXCEPTIONS = new HashMap<>();

    static {

        EXCEPTIONS.put("алгоритм",     List.of(2, 4));
        EXCEPTIONS.put("компьютер",    List.of(3, 6));

        EXCEPTIONS.put("computer",     List.of(3));
        EXCEPTIONS.put("information",  List.of(2, 5, 7));
    }

    @Override
    public List<HyphenPoint> findHyphenPoints(String word, int wordStartOffset, Locale locale) {
        List<HyphenPoint> result = new ArrayList<>();

        if (word == null || word.length() < MIN_PART_LENGTH * 2) {
            return result;
        }

        String lower = word.toLowerCase(locale);
        String language = locale.getLanguage();

        List<Integer> exc = EXCEPTIONS.get(lower);
        if (exc != null) {
            for (int pos : exc) {
                int utf16Offset = word.offsetByCodePoints(0, pos);
                result.add(new HyphenPoint(wordStartOffset + utf16Offset, true, 10));
            }
            return result;
        }

        String[] patterns = "ru".equals(language) ? RU_PATTERNS : EN_PATTERNS;

        int[] levels = computeLevels(lower, patterns);

        for (int i = MIN_PART_LENGTH; i < lower.length() - MIN_PART_LENGTH; i++) {
            if (levels[i + 1] % 2 == 1) {
                int utf16Offset = word.offsetByCodePoints(0, i);
                result.add(new HyphenPoint(
                        wordStartOffset + utf16Offset,
                        true,
                        10 - levels[i + 1]
                ));
            }
        }

        return result;
    }

    private int[] computeLevels(String lowerWord, String[] patterns) {
        String dotted = "." + lowerWord + ".";
        int[] levels = new int[dotted.length()];

        for (String pat : patterns) {
            int patLen = pat.length();
            for (int i = 0; i <= dotted.length() - patLen; i++) {
                if (isPatternMatch(dotted, i, pat)) {
                    applyPattern(levels, i, pat);
                }
            }
        }
        return levels;
    }

    private boolean isPatternMatch(String dotted, int start, String pat) {
        int j = 0;
        for (int k = 0; k < pat.length(); k++) {
            char pc = pat.charAt(k);
            if (Character.isDigit(pc)) {
                continue;
            }
            if (start + j >= dotted.length() || dotted.charAt(start + j) != pc) {
                return false;
            }
            j++;
        }
        return true;
    }

    private void applyPattern(int[] levels, int start, String pat) {
        int j = 0;
        for (int k = 0; k < pat.length(); k++) {
            char pc = pat.charAt(k);
            if (Character.isDigit(pc)) {
                levels[start + j] = Math.max(levels[start + j], pc - '0');
            } else {
                j++;
            }
        }
    }
}