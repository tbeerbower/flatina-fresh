package org.beerbower.flatinafresh.shopping;

import org.beerbower.flatinafresh.recipe.RecipeIngredient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListServiceTest {
    @Test
    void usesCardColumnsAndScalesOtherServings() {
        RecipeIngredient ing = new RecipeIngredient();
        ing.setQuantity2(new BigDecimal("10"));
        ing.setQuantity4(new BigDecimal("20"));
        assertThat(ShoppingListService.scale(ing, 2)).isEqualByComparingTo("10");
        assertThat(ShoppingListService.scale(ing, 4)).isEqualByComparingTo("20");
        assertThat(ShoppingListService.scale(ing, 6)).isEqualByComparingTo("30");
        assertThat(ShoppingListService.scale(ing, 1)).isEqualByComparingTo("5");

        RecipeIngredient sameForBoth = new RecipeIngredient();
        sameForBoth.setQuantity2(BigDecimal.ONE);
        sameForBoth.setQuantity4(BigDecimal.ONE);
        assertThat(ShoppingListService.scale(sameForBoth, 4)).isEqualByComparingTo("1");

        assertThat(ShoppingListService.scale(new RecipeIngredient(), 2)).isNull();
    }

    @Test
    void normalizesUnitsAndConvertsVolume() {
        assertThat(Units.normalize("TBSP")).isEqualTo("tbsp");
        assertThat(Units.normalize("Cloves")).isEqualTo("clove");
        assertThat(Units.normalize(null)).isEmpty();
        assertThat(Units.fromTeaspoons(new BigDecimal("2"))).isEqualTo(new Units.Amount(new BigDecimal("2"), "tsp"));
        assertThat(Units.fromTeaspoons(new BigDecimal("9")).unit()).isEqualTo("tbsp");
        assertThat(Units.fromTeaspoons(new BigDecimal("24")).quantity()).isEqualByComparingTo("0.5");
    }

    @Test
    void matchesSingularAndPluralNames() {
        assertThat(ShoppingListService.normalizeName("Carrots")).isEqualTo(ShoppingListService.normalizeName("Carrot"));
        assertThat(ShoppingListService.normalizeName("Roma Tomatoes"))
                .isEqualTo(ShoppingListService.normalizeName("Roma Tomato"));
        assertThat(ShoppingListService.normalizeName("Ground Beef*")).isEqualTo("ground beef");
        assertThat(ShoppingListService.normalizeName("Swiss")).isEqualTo("swiss");
    }

    @Test
    void groupsByAisle() {
        assertThat(Aisles.of("Potato Buns")).isEqualTo(Aisles.BAKERY);
        assertThat(Aisles.of("Yukon Gold Potatoes")).isEqualTo(Aisles.PRODUCE);
        assertThat(Aisles.of("Monterey Jack Cheese")).isEqualTo(Aisles.DAIRY);
        assertThat(Aisles.of("Chicken Stock Concentrate")).isEqualTo(Aisles.OTHER);
        assertThat(Aisles.of("Ground Beef")).isEqualTo(Aisles.MEAT);
        assertThat(Aisles.of("Poblano Pepper")).isEqualTo(Aisles.PRODUCE);
    }

    @Test
    void countsPackagesFromRecipeAmounts() {
        // 6 tbsp soy sauce = 3 fl oz, one 10 fl oz bottle
        assertThat(Units.packages(Map.of("tsp", new BigDecimal("18")), new BigDecimal("10"), "fl oz")).isEqualTo(1);
        // 20 oz + 0.5 lb of beef in 1 lb packs = 28 oz, 2 packs
        assertThat(Units.packages(Map.of("oz", new BigDecimal("20"), "lb", new BigDecimal("0.5")),
                BigDecimal.ONE, "lb")).isEqualTo(2);
        // exactly 2 packages is 2, not 3
        assertThat(Units.packages(Map.of("oz", new BigDecimal("32")), BigDecimal.ONE, "lb")).isEqualTo(2);
        // 12 cloves of garlic, 10 per head
        assertThat(Units.packages(Map.of("clove", new BigDecimal("12")), BigDecimal.TEN, "cloves")).isEqualTo(2);
        // 3 onions, sold each
        assertThat(Units.packages(Map.of("", new BigDecimal("3")), BigDecimal.ONE, "")).isEqualTo(3);
        // 1 can of beans needs at least one 15 oz can, whatever the count
        assertThat(Units.packages(Map.of("can", BigDecimal.ONE), BigDecimal.ONE, "can")).isEqualTo(1);
        // volume can't become weight, and no size means no count
        assertThat(Units.packages(Map.of("tsp", BigDecimal.ONE), BigDecimal.ONE, "oz")).isNull();
        assertThat(Units.packages(Map.of("tsp", BigDecimal.ONE), null, null)).isNull();
        assertThat(Units.packages(Map.of(), BigDecimal.ONE, "oz")).isNull();
    }
}
