package hyphenation.impl;

import hyphenation.api.WordTokenizer;
import hyphenation.model.TextToken;
import hyphenation.model.TokenType;

import java.util.ArrayList;
import java.util.List;

public class SimpleWordTokenizer implements WordTokenizer {

    @Override
    public List<TextToken> tokenize(String text) {
        List<TextToken> tokens = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return tokens;
        }

        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if (codePoint == '\n' || codePoint == '\r') {
                tokens.add(new TextToken(
                        TokenType.EXPLICIT_BREAK,
                        i,
                        i + charCount,
                        text.substring(i, i + charCount)
                ));
                i += charCount;

            } else if (Character.isWhitespace(codePoint)) {
                int start = i;

                while (i < text.length()) {
                    int cp = text.codePointAt(i);

                    if (!Character.isWhitespace(cp) || cp == '\n' || cp == '\r') {
                        break;
                    }

                    i += Character.charCount(cp);
                }

                tokens.add(new TextToken(
                        TokenType.SPACE,
                        start,
                        i,
                        text.substring(start, i)
                ));

            } else if (Character.isLetter(codePoint)) {
                int start = i;

                while (i < text.length()) {
                    int cp = text.codePointAt(i);

                    if (!Character.isLetter(cp)) {
                        break;
                    }

                    i += Character.charCount(cp);
                }

                tokens.add(new TextToken(
                        TokenType.WORD,
                        start,
                        i,
                        text.substring(start, i)
                ));

            } else if (Character.isISOControl(codePoint)) {
                tokens.add(new TextToken(
                        TokenType.CONTROL,
                        i,
                        i + charCount,
                        text.substring(i, i + charCount)
                ));
                i += charCount;

            } else {
                tokens.add(new TextToken(
                        TokenType.PUNCT,
                        i,
                        i + charCount,
                        text.substring(i, i + charCount)
                ));
                i += charCount;
            }
        }

        return tokens;
    }
}