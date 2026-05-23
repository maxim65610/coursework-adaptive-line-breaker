package hyphenation.impl;

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
public class UnicodeAnalyzer implements hyphenation.api.UnicodeAnalyzer {

    @Override
    public Set<Integer> findSafeBreakOffsets(String text) {
    	
    	// TODO: тут можно инвертировать логику множества, чтобы не сохранять все позиции строки
    	// В реальных сценариях позиций, где нельзя резать строку существенно меньше, чем 
    	// тех, где можно (или запрещенных вообще нет). Поэтому если в качестве результата использовать
    	// специальный контейнер (не просто Set) и инвертированную логику (запрещенные позиции), то 
    	// можно ускорить код и сократить затраты по памяти.
    	
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