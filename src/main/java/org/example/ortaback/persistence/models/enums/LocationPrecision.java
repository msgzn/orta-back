package org.example.ortaback.persistence.models.enums;

/**
 * Точность отображения координат события.
 *
 * <ul>
 *   <li>{@link #EXACT} — точные координаты, отдаются всем.</li>
 *   <li>{@link #APPROXIMATE} — на карте отображается «зона» (точка с округлением
 *       через {@code ST_SnapToGrid}); реальные координаты раскрываются только
 *       присоединившимся участникам.</li>
 * </ul>
 */
public enum LocationPrecision {
    EXACT,
    APPROXIMATE
}
