package org.beerbower.flatinafresh.shopping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

    /** Teaspoons in one of each volume unit, and ounces in one of each weight unit, for package counting. */
    private static final Map<String, BigDecimal> TSP_IN = Map.of("tsp", BigDecimal.ONE, "tbsp", BigDecimal.valueOf(3),
            "cup", BigDecimal.valueOf(48), "fl oz", BigDecimal.valueOf(6), "ml", new BigDecimal("0.202884"));
    private static final Map<String, BigDecimal> OZ_IN = Map.of("oz", BigDecimal.ONE, "lb", BigDecimal.valueOf(16),
            "g", new BigDecimal("0.035274"));

    /** {@code quantity} of {@code unit} expressed in {@code target}, or null when they don't measure the same thing. */
    static BigDecimal convert(BigDecimal quantity, String unit, String target) {
        if (unit.equals(target)) {
            return quantity;
        }
        for (Map<String, BigDecimal> scale : List.of(TSP_IN, OZ_IN)) {
            if (scale.containsKey(unit) && scale.containsKey(target)) {
                return quantity.multiply(scale.get(unit)).divide(scale.get(target), 6, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    /**
     * Whole packages needed to cover {@code amounts} (normalized unit to quantity), when each package holds
     * {@code size} {@code sizeUnit}. Null when there's nothing to measure or an amount can't be converted.
     */
    static Integer packages(Map<String, BigDecimal> amounts, BigDecimal size, String sizeUnit) {
        if (size == null || size.signum() <= 0 || amounts.isEmpty()) {
            return null;
        }
        String target = normalize(sizeUnit);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> a : amounts.entrySet()) {
            BigDecimal converted = convert(a.getValue(), a.getKey(), target);
            if (converted == null) {
                return null;
            }
            total = total.add(converted);
        }
        // Allow a sliver of rounding (2.0001 packages is 2) before rounding up.
        BigDecimal count = total.divide(size, 6, RoundingMode.HALF_UP).subtract(new BigDecimal("0.001"));
        return Math.max(1, count.setScale(0, RoundingMode.CEILING).intValueExact());
    }

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    record Amount(BigDecimal quantity, String unit) {
    }
}
