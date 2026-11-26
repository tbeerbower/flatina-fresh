package org.beerbower.flatinafresh.shopping;

import lombok.RequiredArgsConstructor;
import org.beerbower.flatinafresh.plan.MealPlanEntry;
import org.beerbower.flatinafresh.plan.MealPlanRepository;
import org.beerbower.flatinafresh.recipe.RecipeIngredient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ShoppingListService {
    private final MealPlanRepository planEntries;

    public record AmountDto(BigDecimal quantity, String unit) {
    }

    public record ItemDto(String key, String name, String aisle, List<AmountDto> amounts, List<String> recipes,
                          boolean pantry) {
    }

    public record MealDto(LocalDate date, String recipeTitle, int servings) {
    }

    public record ShoppingListDto(LocalDate start, LocalDate end, List<MealDto> meals, List<ItemDto> items) {
    }

    @Transactional(readOnly = true)
    public ShoppingListDto build(LocalDate start, LocalDate end) {
        List<MealPlanEntry> entries = planEntries.findInRange(start, end);
        Map<String, Accumulator> byName = new LinkedHashMap<>();
        List<MealDto> meals = new ArrayList<>();

        for (MealPlanEntry entry : entries) {
            meals.add(new MealDto(entry.getPlanDate(), entry.getRecipe().getTitle(), entry.getServings()));
            for (RecipeIngredient ing : entry.getRecipe().getIngredients()) {
                String key = (ing.isPantry() ? "pantry:" : "") + normalizeName(ing.getName());
                Accumulator acc = byName.computeIfAbsent(key, k -> new Accumulator(k, ing.getName().trim(), ing.isPantry()));
                acc.add(scale(ing, entry.getServings()), Units.normalize(ing.getUnit()), entry.getRecipe().getTitle());
            }
        }

        List<ItemDto> items = byName.values().stream()
                .map(Accumulator::toDto)
                .sorted(Comparator.comparing(ItemDto::pantry)
                        .thenComparing(i -> Aisles.ORDER.indexOf(i.aisle()))
                        .thenComparing(i -> i.name().toLowerCase(Locale.ROOT)))
                .toList();
        return new ShoppingListDto(start, end, meals, items);
    }

    /** Amount for the planned servings: the card's 2- or 4-person column, otherwise scaled from the closest. */
    static BigDecimal scale(RecipeIngredient ing, int servings) {
        BigDecimal q2 = ing.getQuantity2();
        BigDecimal q4 = ing.getQuantity4();
        if (servings == 2 && q2 != null) {
            return q2;
        }
        if (servings == 4 && q4 != null) {
            return q4;
        }
        if (q4 != null && (servings > 2 || q2 == null)) {
            return q4.multiply(BigDecimal.valueOf(servings)).divide(BigDecimal.valueOf(4), 4, RoundingMode.HALF_UP);
        }
        if (q2 != null) {
            return q2.multiply(BigDecimal.valueOf(servings)).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        }
        return null;
    }

    /** Matching key for an ingredient name: case, spacing, footnote marks and a plural "s" don't matter. */
    static String normalizeName(String name) {
        String n = name.toLowerCase(Locale.ROOT).replace("*", "").replaceAll("\\s+", " ").trim();
        if (n.endsWith("oes")) {
            return n.substring(0, n.length() - 2);
        }
        if (n.endsWith("s") && !n.endsWith("ss")) {
            return n.substring(0, n.length() - 1);
        }
        return n;
    }

    private static final class Accumulator {
        private final String key;
        private final String name;
        private final boolean pantry;
        private BigDecimal teaspoons;
        private final Map<String, BigDecimal> byUnit = new LinkedHashMap<>();
        private final Set<String> recipes = new LinkedHashSet<>();

        Accumulator(String key, String name, boolean pantry) {
            this.key = key;
            this.name = name;
            this.pantry = pantry;
        }

        void add(BigDecimal quantity, String unit, String recipe) {
            recipes.add(recipe);
            if (quantity == null) {
                return;
            }
            if (Units.isVolume(unit)) {
                BigDecimal tsp = Units.toTeaspoons(quantity, unit);
                teaspoons = teaspoons == null ? tsp : teaspoons.add(tsp);
            } else {
                byUnit.merge(unit, quantity, BigDecimal::add);
            }
        }

        ItemDto toDto() {
            List<AmountDto> amounts = new ArrayList<>();
            byUnit.forEach((unit, q) -> amounts.add(new AmountDto(Units.round(q), unit.isEmpty() ? null : unit)));
            if (teaspoons != null) {
                Units.Amount a = Units.fromTeaspoons(teaspoons);
                amounts.add(new AmountDto(a.quantity(), a.unit()));
            }
            return new ItemDto(key, name, pantry ? "Pantry" : Aisles.of(name), amounts, List.copyOf(recipes), pantry);
        }
    }
}
