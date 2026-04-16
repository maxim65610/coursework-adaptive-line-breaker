package hyphenation.api;

import hyphenation.model.TextToken;

import java.util.List;


/**
 * Интерфейс токенизатора текста.
 *
 * Реализация должна разбивать строку на токены:
 * слова, пробелы, пунктуацию, переводы строки и т.д.
 */
public interface WordTokenizer {
    /**
     * Разбивает строку на токены.
     *
     * @param text исходный текст
     * @return список токенов
     */
    List<TextToken> tokenize(String text);
}