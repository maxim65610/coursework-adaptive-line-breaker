package hyphenation.model;

/**
 * Префиксное дерево паттернов переноса.
 */
public class HyphenPatternTrie {

    private final HyphenPatternTrieNode root;

    public HyphenPatternTrie() {
        this.root = new HyphenPatternTrieNode();
    }

    public HyphenPatternTrieNode getRoot() {
        return root;
    }

    /**
     * Добавляет один паттерн в дерево.
     */
    public void addPattern(HyphenPattern pattern) {
        HyphenPatternTrieNode current = root;

        for (int letter : pattern.getLetters()) {
            current = current.getChildren()
                    .computeIfAbsent(letter, key -> new HyphenPatternTrieNode());
        }

        current.getTerminalPatterns().add(pattern);
    }
}
