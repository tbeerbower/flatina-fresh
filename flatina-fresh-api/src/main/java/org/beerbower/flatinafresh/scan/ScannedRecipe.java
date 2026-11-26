package org.beerbower.flatinafresh.scan;

import java.math.BigDecimal;
import java.util.List;

/** What the vision model reads off a recipe card. Mirrors the JSON schema in {@link RecipeCardReader}. */
public record ScannedRecipe(
        String title,
        String subtitle,
        String description,
        Integer prepMinutes,
        Integer totalMinutes,
        Integer calories,
        List<String> tags,
        List<String> equipment,
        List<Item> ingredients,
        List<Item> pantryItems,
        List<Step> steps,
        String notes) {

    public record Item(String name, BigDecimal quantity2, BigDecimal quantity4, String unit, String allergens) {
    }

    public record Step(String title, String instructions) {
    }
}
