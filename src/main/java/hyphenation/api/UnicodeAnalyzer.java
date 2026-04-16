package hyphenation.api;

import java.util.Set;

/**
 * Интерфейс для поиска безопасных Unicode-границ строки.
 *
 * Реализация должна вернуть такие UTF-16 позиции,
 * в которых строку можно безопасно разрезать.
 */
public interface UnicodeAnalyzer {
    /**
     * Находит безопасные позиции разреза строки.
     *
     * @param text исходная строка
     * @return множество безопасных UTF-16 offset
     */
    Set<Integer> findSafeBreakOffsets(String text);
}