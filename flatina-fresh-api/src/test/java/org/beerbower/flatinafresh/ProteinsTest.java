package org.beerbower.flatinafresh;

import org.beerbower.flatinafresh.recipe.Proteins;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProteinsTest {
    @Test
    void tagsWin() {
        assertThat(Proteins.classify("vegetarian, pasta", List.of("Chicken Stock Concentrate"))).isEqualTo("vegetarian");
        assertThat(Proteins.classify("Beef, burger", List.of("Ground Turkey"))).isEqualTo("beef");
    }

    @Test
    void fallsBackToIngredients() {
        assertThat(Proteins.classify("tacos", List.of("Yellow Onion", "Ground Pork"))).isEqualTo("pork");
        assertThat(Proteins.classify(null, List.of("Chicken Breasts", "Rice"))).isEqualTo("chicken");
        assertThat(Proteins.classify("", List.of("Salmon", "Lemon"))).isEqualTo("seafood");
        assertThat(Proteins.classify("", List.of("Italian Sausage"))).isEqualTo("pork");
    }

    @Test
    void stocksAndSaucesAreNotTheProtein() {
        assertThat(Proteins.classify(null, List.of("Beef Stock Concentrate", "Fish Sauce", "Mushrooms")))
                .isEqualTo("vegetarian");
        assertThat(Proteins.classify(null, List.of())).isNull();
    }

    @Test
    void normalizesSynonyms() {
        assertThat(Proteins.normalize(" Veggie ")).isEqualTo("vegetarian");
        assertThat(Proteins.normalize("Shrimp")).isEqualTo("seafood");
        assertThat(Proteins.normalize("  ")).isNull();
    }
}
