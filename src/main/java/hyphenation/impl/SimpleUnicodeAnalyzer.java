package hyphenation.impl;

import hyphenation.api.UnicodeAnalyzer;

import java.text.BreakIterator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Базовый анализатор безопасных Unicode-границ.
 *
 * Возвращает набор UTF-16 позиций, в которых строку можно
 * безопасно разрезать без повреждения Unicode-символов.
 *
 * Использует стандартный BreakIterator Java.
 */
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