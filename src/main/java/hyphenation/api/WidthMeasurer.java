package hyphenation.api;

import java.awt.Font;
import java.awt.font.FontRenderContext;

public interface WidthMeasurer {
    float measure(String text, Font font, FontRenderContext frc);
}