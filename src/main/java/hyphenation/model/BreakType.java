package hyphenation.model;


/**
 * Тип точки разрыва строки.

 * Нужен, чтобы понимать, откуда взялся BreakCandidate
 * и как потом обрабатывать сегмент.
 */
public enum BreakType {
    SPACE,
    PUNCT,
    EXPLICIT_BREAK,
    HYPHENATION,
    END_OF_TEXT
}