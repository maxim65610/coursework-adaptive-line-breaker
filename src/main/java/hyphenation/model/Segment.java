package hyphenation.model;

public class Segment {
    private final int startUtf16;
    private final int endUtf16;
    private final boolean hyphenAppended;
    private final float width;
    private final String text;

    public Segment(int startUtf16, int endUtf16, boolean hyphenAppended, float width, String text) {
        this.startUtf16 = startUtf16;
        this.endUtf16 = endUtf16;
        this.hyphenAppended = hyphenAppended;
        this.width = width;
        this.text = text;
    }

    public int getStartUtf16() {
        return startUtf16;
    }

    public int getEndUtf16() {
        return endUtf16;
    }

    public boolean isHyphenAppended() {
        return hyphenAppended;
    }

    public float getWidth() {
        return width;
    }

    public String getText() {
        return text;
    }
}