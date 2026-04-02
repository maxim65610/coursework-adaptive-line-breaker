package hyphenation.api;

import hyphenation.model.HyphenPoint;

import java.util.List;
import java.util.Locale;

public interface Hyphenator {
    List<HyphenPoint> findHyphenPoints(String word, int wordStartOffset, Locale locale);
}
