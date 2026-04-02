package hyphenation.impl;

import hyphenation.api.UnicodeAnalyzer;

import java.text.BreakIterator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class SimpleUnicodeAnalyzer implements UnicodeAnalyzer {

    @Override
    public Set<Integer> findSafeBreakOffsets(String text) {
        Set<Integer> safeOffsets = new HashSet<>();

        if (text == null) {
            return safeOffsets;
        }

        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);

        int boundary = iterator.first();
        while (boundary != BreakIterator.DONE) {
            safeOffsets.add(boundary);
            boundary = iterator.next();
        }

        return safeOffsets;
    }
}