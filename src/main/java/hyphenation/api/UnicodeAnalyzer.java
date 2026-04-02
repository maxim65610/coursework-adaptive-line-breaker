package hyphenation.api;

import java.util.Set;

public interface UnicodeAnalyzer {
    Set<Integer> findSafeBreakOffsets(String text);
}