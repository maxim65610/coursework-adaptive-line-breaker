package hyphenation.model;

public class HyphenPattern {
    private final int[] letters;
    private final int[] levels;

    public HyphenPattern(int[] letters, int[] levels) {
        this.letters = letters;
        this.levels = levels;
    }

    public int[] getLetters() {
        return letters;
    }

    public int[] getLevels() {
        return levels;
    }
}