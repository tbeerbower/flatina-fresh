package org.beerbower.flatinafresh.shopping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/** Normalizes unit names and converts kitchen volume units so amounts from different recipes can be added. */
final class Units {
    private Units() {
    }

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("tablespoon", "tbsp"), Map.entry("tablespoons", "tbsp"), Map.entry("tbs", "tbsp"),
            Map.entry("tbsp", "tbsp"), Map.entry("tbsps", "tbsp"), Map.entry("t", "tbsp"),
            Map.entry("teaspoon", "tsp"), Map.entry("teaspoons", "tsp"), Map.entry("tsp", "tsp"),
            Map.entry("tsps", "tsp"),
            Map.entry("cup", "cup"), Map.entry("cups", "cup"), Map.entry("c", "cup"),
            Map.entry("ounce", "oz"), Map.entry("ounces", "oz"), Map.entry("oz", "oz"),
            Map.entry("fl oz", "fl oz"), Map.entry("fluid ounce", "fl oz"), Map.entry("fluid ounces", "fl oz"),
            Map.entry("pound", "lb"), Map.entry("pounds", "lb"), Map.entry("lb", "lb"), Map.entry("lbs", "lb"),
            Map.entry("clove", "clove"), Map.entry("cloves", "clove"),
            Map.entry("gram", "g"), Map.entry("grams", "g"), Map.entry("g", "g"),
            Map.entry("milliliter", "ml"), Map.entry("milliliters", "ml"), Map.entry("ml", "ml"),
            Map.entry("slice", "slice"), Map.entry("slices", "slice"),
            Map.entry("can", "can"), Map.entry("cans", "can"),
            Map.entry("package", "package"), Map.entry("packages", "package"),
            Map.entry("pkg", "package"), Map.entry("bunch", "bunch"), Map.entry("bunches", "bunch"),
            Map.entry("piece", "piece"), Map.entry("pieces", "piece"),
            Map.entry("unit", ""), Map.entry("units", ""), Map.entry("each", ""), Map.entry("whole", ""));

    /** Teaspoons per unit, for units that can be merged. */
    private static final Map<String, Integer> VOLUME_TSP = Map.of("tsp", 1, "tbsp", 3, "cup", 48);

    static String normalize(String unit) {
        if (unit == null) {
            return "";
        }
        String u = unit.trim().toLowerCase(Locale.ROOT).replaceAll("\\.$", "").replaceAll("\\s+", " ");
        return ALIASES.getOrDefault(u, u);
    }

    static boolean isVolume(String normalizedUnit) {
        return VOLUME_TSP.containsKey(normalizedUnit);
    }

    static BigDecimal toTeaspoons(BigDecimal quantity, String normalizedUnit) {
        return quantity.multiply(BigDecimal.valueOf(VOLUME_TSP.get(normalizedUnit)));
    }

    /** Presents a teaspoon total in the largest unit that reads naturally. */
    static Amount fromTeaspoons(BigDecimal tsp) {
        if (tsp.compareTo(BigDecimal.valueOf(12)) >= 0) {
            return new Amount(round(tsp.divide(BigDecimal.valueOf(48), 4, RoundingMode.HALF_UP)), "cup");
        }
        if (tsp.compareTo(BigDecimal.valueOf(3)) >= 0) {
            return new Amount(round(tsp.divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP)), "tbsp");
        }
        return new Amount(round(tsp), "tsp");
    }

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    record Amount(BigDecimal quantity, String unit) {
    }
}
