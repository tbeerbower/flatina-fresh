package org.beerbower.flatinafresh.recipe;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_ingredient")
@Getter
@Setter
public class RecipeIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    private int position;
    private String name;
    private BigDecimal quantity2;
    private BigDecimal quantity4;
    private String unit;
    private String allergens;
    /** True for "Bust out" items you're expected to have at home (oil, salt, sugar...). */
    private boolean pantry;
}
