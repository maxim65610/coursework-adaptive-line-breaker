package hyphenation.model;

/**
 * Один токен исходного текста.
 * Токен — это фрагмент строки:
 * слово, пробел, пунктуация, перевод строки и т.д.
 */
public class TextToken {
    private final TokenType type;
    private final int startUtf16;
    private final int endUtf16;
    private final String text;

    public TextToken(TokenType type, int startUtf16, int endUtf16, String text) {
        this.type = type;
        this.startUtf16 = startUtf16;
        this.endUtf16 = endUtf16;
        this.text = text;
    }

    public TokenType getType() {
        return type;
    }

    public int getStartUtf16() {
        return startUtf16;
    }

    public int getEndUtf16() {
        return endUtf16;
    }

    public String getText() {
        return text;
    }
}