package hyphenation.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Один узел автомата паттернов переноса.
 *
 * Хранит:
 * - переходы к дочерним узлам по следующему code point
 * - список паттернов, которые заканчиваются в этом узле
 * - суффиксную ссылку (failure link) для отката при несовпадении
 */
public class HyphenPatternTrieNode {

    private final Map<Integer, HyphenPatternTrieNode> children;
    private final List<HyphenPattern> terminalPatterns;
    private HyphenPatternTrieNode failureLink;

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

    public HyphenPatternTrieNode getFailureLink() {
        return failureLink;
    }

    public void setFailureLink(HyphenPatternTrieNode failureLink) {
        this.failureLink = failureLink;
    }
}