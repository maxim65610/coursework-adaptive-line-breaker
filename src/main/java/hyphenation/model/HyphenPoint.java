package hyphenation.model;

public class HyphenPoint {
    private final int utf16Offset;
    private final boolean appendHyphen;
    private final int penalty;

    public HyphenPoint(int utf16Offset, boolean appendHyphen, int penalty) {
        this.utf16Offset = utf16Offset;
        this.appendHyphen = appendHyphen;
        this.penalty = penalty;
    }

    public int getUtf16Offset() {
        return utf16Offset;
    }

    public boolean isAppendHyphen() {
        return appendHyphen;
    }

    public int getPenalty() {
        return penalty;
    }
}
