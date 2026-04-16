package hyphenation.impl;


import hyphenation.api.WidthMeasurer;

import java.awt.Font;
import java.awt.font.FontRenderContext;

/**
 * Базовый измеритель ширины текста через AWT.

 * Возвращает ширину строки в пикселях
 * для заданного шрифта и FontRenderContext.
 */
public class AwtWidthMeasurer implements WidthMeasurer {

    @Override
    public float measure(String text, Font font, FontRenderContext frc) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }

        return (float) font.getStringBounds(text, frc).getWidth();
    }
}
