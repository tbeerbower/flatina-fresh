package org.beerbower.flatinafresh.plan;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.beerbower.flatinafresh.recipe.Recipe;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_plan_entry")
@Getter
@Setter
public class MealPlanEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate planDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    private int servings = 2;
    private LocalDateTime createdAt = LocalDateTime.now();
}
