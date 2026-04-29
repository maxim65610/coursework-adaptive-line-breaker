package hyphenation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Один узел префиксного дерева паттернов переноса.
 *
 * Хранит:
 * - переходы к дочерним узлам по следующему code point
 * - список паттернов, которые заканчиваются в этом узле
 */
public class HyphenPatternTrieNode {

    private final Map<Integer, HyphenPatternTrieNode> children;
    private final List<HyphenPattern> terminalPatterns;

    public HyphenPatternTrieNode() {
        this.children = new HashMap<>();
        this.terminalPatterns = new ArrayList<>();
    }

    public Map<Integer, HyphenPatternTrieNode> getChildren() {
        return children;
    }

    public List<HyphenPattern> getTerminalPatterns() {
        return terminalPatterns;
    }
}