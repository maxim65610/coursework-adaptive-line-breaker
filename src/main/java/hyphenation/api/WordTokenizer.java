package hyphenation.api;

import hyphenation.model.TextToken;

import java.util.List;

public interface WordTokenizer {
    List<TextToken> tokenize(String text);
}