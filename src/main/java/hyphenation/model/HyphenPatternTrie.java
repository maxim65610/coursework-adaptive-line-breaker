package hyphenation.model;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

/**
 * Префиксное дерево паттернов переноса.
 */
public class HyphenPatternTrie {

    private final HyphenPatternTrieNode root;

    public HyphenPatternTrie() {
        this.root = new HyphenPatternTrieNode();
        this.root.setFailureLink(this.root);
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

    /**
     * Строит суффиксные ссылки для всех узлов дерева.
     */
    public void buildFailureLinks() {
        Queue<HyphenPatternTrieNode> queue = new ArrayDeque<>();

        // Первый уровень: все дети корня откатываются в корень.
        for (HyphenPatternTrieNode child : root.getChildren().values()) {
            child.setFailureLink(root);
            queue.add(child);
        }

        // BFS по дереву
        while (!queue.isEmpty()) {
            HyphenPatternTrieNode current = queue.poll();

            for (Map.Entry<Integer, HyphenPatternTrieNode> entry : current.getChildren().entrySet()) {
                int letter = entry.getKey();
                HyphenPatternTrieNode child = entry.getValue();

                HyphenPatternTrieNode fallback = current.getFailureLink();

                // Ищем, куда можно откатиться и откуда есть переход по тому же символу.
                while (fallback != root && !fallback.getChildren().containsKey(letter)) {
                    fallback = fallback.getFailureLink();
                }

                child.setFailureLink(fallback.getChildren().getOrDefault(letter, root));

                queue.add(child);
            }
        }
    }
}