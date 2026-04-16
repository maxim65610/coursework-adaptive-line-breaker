package hyphenation.api;

import java.awt.Font;
import java.awt.font.FontRenderContext;

/**
 * Интерфейс для измерения ширины строки.
 *
 * Реализация должна уметь вычислять,
 * сколько места занимает текст в заданном шрифте.
 */
public interface WidthMeasurer {
    /**
     * Измеряет ширину строки.
     *
     * @param text текст для измерения
     * @param font шрифт
     * @param frc контекст рендеринга шрифта
     * @return ширина строки
     */
    float measure(String text, Font font, FontRenderContext frc);
}