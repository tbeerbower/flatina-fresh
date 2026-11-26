package org.beerbower.flatinafresh.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class RecipeDtos {
    private RecipeDtos() {
    }

    public record IngredientDto(
            Long id,
            @NotBlank String name,
            BigDecimal quantity2,
            BigDecimal quantity4,
            String unit,
            String allergens,
            boolean pantry) {
    }

    public record StepDto(Long id, String title, @NotBlank String instructions) {
    }

    public record RecipeDto(
            Long id,
            @NotBlank String title,
            String subtitle,
            String description,
            String source,
            Integer prepMinutes,
            Integer totalMinutes,
            Integer calories,
            String tags,
            String equipment,
            String notes,
            @Valid List<IngredientDto> ingredients,
            @Valid List<StepDto> steps,
            List<Long> imageIds) {
    }

    public record RecipeSummaryDto(
            Long id,
            String title,
            String subtitle,
            Integer prepMinutes,
            Integer totalMinutes,
            Integer calories,
            String tags,
            Long coverImageId) {
    }
}
