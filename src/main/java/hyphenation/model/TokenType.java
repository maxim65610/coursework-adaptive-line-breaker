package hyphenation.model;

/**
 * Тип токена исходного текста.
 * Используется при токенизации,
 * чтобы понимать, что именно за фрагмент строки перед нами.
 */
public enum TokenType {
    WORD,
    SPACE,
    PUNCT,
    CONTROL,
    EXPLICIT_BREAK
}