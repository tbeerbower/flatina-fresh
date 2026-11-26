package org.beerbower.flatinafresh.shopping;

import org.beerbower.flatinafresh.recipe.RecipeIngredient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
}
